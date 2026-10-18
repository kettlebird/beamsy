package io.github.jqssun.airplay.bridge

interface RaopCallbackHandler {
    fun onVideoData(data: ByteArray, ntpTimeNs: Long, isH265: Boolean)
    fun onAudioFormat(ct: Int, spf: Int, usingScreen: Boolean)
    fun onVideoSize(srcW: Float, srcH: Float, w: Float, h: Float)
    fun onVolumeChange(volume: Float)
    fun onClientVolume(): Float
    fun onAudioTeardown()
    fun onConnectionInit()
    fun onConnectionDestroy()
    // sender device name from the first SETUP; empty if the sender did not provide one
    fun onClientRequest(name: String)
    fun onConnectionReset(reason: Int)
    fun onDisplayPin(pin: String)
    fun onMetadata(data: ByteArray)
    fun onCoverArt(data: ByteArray)
    fun onProgress(start: Long, curr: Long, end: Long)
    fun onDacpId(dacpId: String, activeRemote: String)
    fun onMirrorRunning(running: Boolean)
    // video (hls), distinct from mirroring and raop audio
    fun onVideoPlay(location: String, startPositionSeconds: Float)
    fun onVideoScrub(positionSeconds: Float)
    fun onVideoRate(rate: Float)
    fun onVideoStop()
    // fired per sender GET /playback-info poll; polling starts before /play
    fun onVideoSessionPoll()
}
