package io.github.jqssun.airplay.renderer

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.roundToInt

// decodes into an app-owned SurfaceTexture that outlives display surface
// gl thread blits it to any surface attached, so fullscreen toggles re-point display without restarting codec
class VideoPipeline {

    private val lock = Object()
    private var thread: Thread? = null
    @Volatile private var running = false

    private var egl: EglCore? = null
    private var window: EGLSurface = EGL14.EGL_NO_SURFACE
    private var winW = 0
    private var winH = 0

    private var oesTex = 0
    private var oesProgram = 0
    private var blurProgram = 0
    private var copyProgram = 0
    private var uBlurStep = 0
    private val texMatrix = FloatArray(16)
    private val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private var hasFrame = false

    // small offscreen targets for the blurred backdrop behind letterboxed (e.g. portrait) video
    private val fbo = IntArray(2)
    private val fboTex = IntArray(2)
    private var fboW = 0
    private var fboH = 0

    private var surfaceTexture: SurfaceTexture? = null
    var inputSurface: Surface? = null; private set

    @Volatile private var frameAvailable = false
    private var pendingDisplay: Surface? = null
    private var displayDirty = false
    @Volatile private var videoW = 0
    @Volatile private var videoH = 0
    @Volatile var blurBars = true

    fun start() = synchronized(lock) {
        if (running) return@synchronized
        running = true
        // a restarted pipeline has a fresh EGL context, so rebind the display it already has
        displayDirty = pendingDisplay != null
        thread = Thread({ _loop() }, "VideoPipeline").also { it.start() }
        while (inputSurface == null && running) lock.wait()
    }

    fun setDisplaySurface(surface: Surface?) = synchronized(lock) {
        pendingDisplay = surface
        displayDirty = true
        lock.notifyAll()
    }

    fun setVideoSize(w: Int, h: Int) {
        videoW = w
        videoH = h
        surfaceTexture?.setDefaultBufferSize(w, h)
    }

    fun release() {
        synchronized(lock) {
            if (!running) return
            running = false
            lock.notifyAll()
        }
        thread?.join()
        thread = null
    }

    private fun _loop() {
        try {
            egl = EglCore()
            _initGl()
        } catch (e: Exception) {
            Log.e(TAG, "GL init failed", e)
            synchronized(lock) { running = false; lock.notifyAll() }
            return
        }
        Matrix.setIdentityM(texMatrix, 0)
        synchronized(lock) {
            inputSurface = Surface(surfaceTexture)
            lock.notifyAll()
        }
        while (true) {
            var newDisplay: Surface? = null
            var displayChanged = false
            var doFrame = false
            synchronized(lock) {
                while (running && !frameAvailable && !displayDirty) lock.wait()
                if (running && displayDirty) {
                    newDisplay = pendingDisplay
                    displayChanged = true
                    displayDirty = false
                }
                if (running && frameAvailable) {
                    frameAvailable = false
                    doFrame = true
                }
            }
            if (!running) break
            if (displayChanged) _bindDisplay(newDisplay)
            if (doFrame) _consumeAndDraw()
        }
        _releaseGl()
    }

    private fun _bindDisplay(surface: Surface?) {
        val egl = egl ?: return
        if (window != EGL14.EGL_NO_SURFACE) {
            egl.makeCurrent()
            egl.destroySurface(window)
            window = EGL14.EGL_NO_SURFACE
        }
        if (surface == null || !surface.isValid) return
        window = egl.windowSurface(surface)
        if (window == EGL14.EGL_NO_SURFACE) {
            Log.w(TAG, "eglCreateWindowSurface failed: ${EGL14.eglGetError()}")
            return
        }
        egl.makeCurrent(window)
        winW = egl.query(window, EGL14.EGL_WIDTH)
        winH = egl.query(window, EGL14.EGL_HEIGHT)
        // an idle source sends no new frames, so repaint last one or new surface stays black
        if (hasFrame) _render()
    }

    private fun _consumeAndDraw() {
        val st = surfaceTexture ?: return
        val egl = egl ?: return
        if (window == EGL14.EGL_NO_SURFACE) {
            // no display: keep consuming so decoder doesn't stall
            egl.makeCurrent()
            st.updateTexImage()
            hasFrame = true
            return
        }
        egl.makeCurrent(window)
        st.updateTexImage()
        st.getTransformMatrix(texMatrix)
        hasFrame = true
        _render()
    }

    // the surface fills the screen; video is fitted here so the bars can show a blurred copy of it
    private fun _render() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, winW, winH)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        val vw = videoW
        val vh = videoH
        if (vw > 0 && vh > 0) {
            val s = minOf(winW.toFloat() / vw, winH.toFloat() / vh)
            val fitW = (vw * s).roundToInt()
            val fitH = (vh * s).roundToInt()
            if (blurBars && (fitW < winW - BAR_MIN_PX || fitH < winH - BAR_MIN_PX)) _drawBackdrop(vw, vh)
            GLES20.glViewport((winW - fitW) / 2, (winH - fitH) / 2, fitW, fitH)
        }
        _draw(oesProgram, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTex, texMatrix)
        egl?.swap(window)
    }

    // video scaled to cover a ~1/10 size target, blurred there, then stretched dimmed over the screen
    private fun _drawBackdrop(vw: Int, vh: Int) {
        _ensureFbos(maxOf(1, winW / BACKDROP_DOWNSCALE), maxOf(1, winH / BACKDROP_DOWNSCALE))
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
        val s = maxOf(fboW.toFloat() / vw, fboH.toFloat() / vh)
        val cw = (vw * s).roundToInt()
        val ch = (vh * s).roundToInt()
        GLES20.glViewport((fboW - cw) / 2, (fboH - ch) / 2, cw, ch)
        _draw(oesProgram, GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTex, texMatrix)
        GLES20.glViewport(0, 0, fboW, fboH)
        repeat(BLUR_ROUNDS) {
            _blur(fboTex[0], fbo[1], 1f / fboW, 0f)
            _blur(fboTex[1], fbo[0], 0f, 1f / fboH)
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, winW, winH)
        _draw(copyProgram, GLES20.GL_TEXTURE_2D, fboTex[0], identity)
    }

    private fun _blur(src: Int, dst: Int, dx: Float, dy: Float) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, dst)
        GLES20.glUseProgram(blurProgram)
        GLES20.glUniform2f(uBlurStep, dx, dy)
        _draw(blurProgram, GLES20.GL_TEXTURE_2D, src, identity)
    }

    private fun _draw(program: Int, target: Int, tex: Int, matrix: FloatArray) {
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(target, tex)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uTexMatrix"), 1, false, matrix, 0)
        GLES20.glEnableVertexAttribArray(A_POS)
        GLES20.glVertexAttribPointer(A_POS, 2, GLES20.GL_FLOAT, false, 0, POS)
        GLES20.glEnableVertexAttribArray(A_TEX)
        GLES20.glVertexAttribPointer(A_TEX, 2, GLES20.GL_FLOAT, false, 0, TEX)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(A_POS)
        GLES20.glDisableVertexAttribArray(A_TEX)
    }

    private fun _ensureFbos(w: Int, h: Int) {
        if (w == fboW && h == fboH) return
        if (fboW > 0) {
            GLES20.glDeleteFramebuffers(2, fbo, 0)
            GLES20.glDeleteTextures(2, fboTex, 0)
        }
        GLES20.glGenFramebuffers(2, fbo, 0)
        GLES20.glGenTextures(2, fboTex, 0)
        for (i in 0..1) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex[i])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[i])
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTex[i], 0)
        }
        fboW = w
        fboH = h
    }

    private fun _initGl() {
        oesProgram = _buildProgram(FRAG)
        blurProgram = _buildProgram(FRAG_BLUR)
        copyProgram = _buildProgram(FRAG_DIM)
        uBlurStep = GLES20.glGetUniformLocation(blurProgram, "uStep")
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        oesTex = tex[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTex)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        surfaceTexture = SurfaceTexture(oesTex).also {
            if (videoW > 0 && videoH > 0) it.setDefaultBufferSize(videoW, videoH)
            it.setOnFrameAvailableListener {
                synchronized(lock) { frameAvailable = true; lock.notifyAll() }
            }
        }
    }

    private fun _buildProgram(frag: String): Int {
        val vs = _shader(GLES20.GL_VERTEX_SHADER, VERT)
        val fs = _shader(GLES20.GL_FRAGMENT_SHADER, frag)
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glBindAttribLocation(it, A_POS, "aPos")
            GLES20.glBindAttribLocation(it, A_TEX, "aTex")
            GLES20.glLinkProgram(it)
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
        }
    }

    private fun _shader(type: Int, src: String): Int {
        return GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, src)
            GLES20.glCompileShader(it)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(it, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) Log.e(TAG, "shader compile failed: ${GLES20.glGetShaderInfoLog(it)}")
        }
    }

    private fun _releaseGl() {
        surfaceTexture?.release()
        surfaceTexture = null
        inputSurface?.release()
        inputSurface = null
        egl?.let {
            if (window != EGL14.EGL_NO_SURFACE) it.destroySurface(window)
            it.close()
        }
        egl = null
        window = EGL14.EGL_NO_SURFACE
        // the context took the targets with it; a restarted pipeline must create new ones
        fboW = 0
        fboH = 0
    }

    companion object {
        private const val TAG = "VideoPipeline"
        private const val A_POS = 0
        private const val A_TEX = 1
        // bars thinner than this (rounding, near-16:9 video) stay plain black
        private const val BAR_MIN_PX = 8
        private const val BACKDROP_DOWNSCALE = 10
        // taps one texel apart (wider spacing left blocky gaps); rounds on the tiny target widen it cheaply
        private const val BLUR_ROUNDS = 8

        private val POS = _fb(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
        private val TEX = _fb(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))

        private fun _fb(a: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(a.size * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply { put(a); position(0) }

        private const val VERT =
            "attribute vec2 aPos;\n" +
            "attribute vec2 aTex;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "varying vec2 vTex;\n" +
            "void main() {\n" +
            "  gl_Position = vec4(aPos, 0.0, 1.0);\n" +
            "  vTex = (uTexMatrix * vec4(aTex, 0.0, 1.0)).xy;\n" +
            "}\n"

        private const val FRAG =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTex;\n" +
            "uniform samplerExternalOES sTex;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(sTex, vTex);\n" +
            "}\n"

        // 9-tap gaussian folded into 5 fetches via linear filtering
        private const val FRAG_BLUR =
            "precision mediump float;\n" +
            "varying vec2 vTex;\n" +
            "uniform sampler2D sTex;\n" +
            "uniform vec2 uStep;\n" +
            "void main() {\n" +
            "  vec2 o1 = uStep * 1.3846153846;\n" +
            "  vec2 o2 = uStep * 3.2307692308;\n" +
            "  gl_FragColor = texture2D(sTex, vTex) * 0.2270270270\n" +
            "    + (texture2D(sTex, vTex + o1) + texture2D(sTex, vTex - o1)) * 0.3162162162\n" +
            "    + (texture2D(sTex, vTex + o2) + texture2D(sTex, vTex - o2)) * 0.0702702703;\n" +
            "}\n"

        // dimmed so the backdrop never competes with the video itself
        private const val FRAG_DIM =
            "precision mediump float;\n" +
            "varying vec2 vTex;\n" +
            "uniform sampler2D sTex;\n" +
            "void main() {\n" +
            "  gl_FragColor = vec4(texture2D(sTex, vTex).rgb * 0.5, 1.0);\n" +
            "}\n"
    }
}
