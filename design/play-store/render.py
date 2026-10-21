"""Render the Google Play listing assets to design/play-store/export/.

    python design/play-store/render.py

Needs Microsoft Edge or Google Chrome. Serves the repo on a local port so the
bundled Open Sauce Sans fonts load, then screenshots each page at its exact size.
"""
import functools
import http.server
import os
import shutil
import struct
import subprocess
import sys
import tempfile
import threading
import zlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
OUT = HERE / "export"

BROWSERS = [
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "google-chrome", "chromium", "msedge",
]

# Shared screenshot state: a believable living-room setup on a Friday evening.
SHOT = "design/beamsy-ui.html?capture&name=Living%20Room&from=Sam%27s%20iPhone&time=2026-10-23T20:30&warn=0&plan=free&session=0"

# (output file, page, width, height, keep alpha)
JOBS = [
    ("icon-512.png",            "design/play-store/listing.html?asset=icon",     512,  512,  True),
    ("feature-graphic-1024x500.png", "design/play-store/listing.html?asset=feature", 1024, 500, False),
    ("tv-banner-1280x720.png",  "design/play-store/listing.html?asset=tvbanner", 1280, 720,  False),
    ("tv-screenshot-1-home.png",              SHOT + "&screen=steps",           1920, 1080, False),
    ("tv-screenshot-2-mirroring-upright.png", SHOT + "&screen=mirror&upright=1",          1920, 1080, False),
    ("tv-screenshot-3-settings.png",          SHOT + "&screen=settings",        1920, 1080, False),
    ("tv-screenshot-4-pin.png",               SHOT + "&screen=pin",                       1920, 1080, False),
    ("tv-screenshot-5-audio.png",             SHOT + "&screen=audio",                     1920, 1080, False),
]


def browser():
    for b in BROWSERS:
        if os.path.isfile(b) or shutil.which(b):
            return b
    sys.exit("Edge or Chrome not found; add its path to BROWSERS.")


def serve():
    handler = functools.partial(QuietHandler, directory=str(ROOT))
    httpd = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
    threading.Thread(target=httpd.serve_forever, daemon=True).start()
    return httpd


class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *args):
        pass


def png_chunks(data):
    pos = 8
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        yield kind, data[pos + 8:pos + 8 + length]
        pos += 12 + length


def set_alpha(path, alpha):
    """Rewrite the PNG as 8-bit RGBA (alpha=True) or RGB (alpha=False). The pages are opaque, so this is lossless.
    Play wants the icon as 32-bit PNG and the feature graphic, banner and screenshots as 24-bit PNG (no alpha)."""
    data = path.read_bytes()
    chunks = list(png_chunks(data))
    w, h, depth, ctype = struct.unpack(">IIBB", chunks[0][1][:10])
    if depth != 8 or ctype not in (2, 6) or (ctype == 6) == alpha:
        return
    bpp = 4 if ctype == 6 else 3
    stride = w * bpp
    raw = zlib.decompress(b"".join(c for k, c in chunks if k == b"IDAT"))
    out, prev, pos = bytearray(), bytearray(stride), 0
    for _ in range(h):
        f = raw[pos]; line = bytearray(raw[pos + 1:pos + 1 + stride]); pos += 1 + stride
        for i in range(stride):  # undo the PNG row filter
            a = line[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            if f == 1: line[i] = (line[i] + a) & 255
            elif f == 2: line[i] = (line[i] + b) & 255
            elif f == 3: line[i] = (line[i] + ((a + b) >> 1)) & 255
            elif f == 4:
                p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        prev = line
        out.append(0)
        if alpha:
            rgba = bytearray(w * 4); rgba[0::4], rgba[1::4], rgba[2::4] = line[0::3], line[1::3], line[2::3]; rgba[3::4] = b"\xff" * w
            out += rgba
        else:
            out += bytes(b for i, b in enumerate(line) if i % 4 != 3)
    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6 if alpha else 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(out), 9)) + chunk(b"IEND", b"")
    path.write_bytes(png)


def main():
    exe = browser()
    OUT.mkdir(exist_ok=True)
    httpd = serve()
    port = httpd.server_address[1]
    with tempfile.TemporaryDirectory() as profile:
        for name, page, w, h, alpha in JOBS:
            target = OUT / name
            subprocess.run([
                exe, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--no-first-run",
                f"--user-data-dir={profile}", "--force-device-scale-factor=1", "--lang=en-US",
                "--virtual-time-budget=4000", f"--window-size={w},{h}",
                f"--screenshot={target}", f"http://127.0.0.1:{port}/{page}",
            ], check=True, capture_output=True)
            set_alpha(target, alpha)
            print(f"{name:42} {w}x{h}  {target.stat().st_size // 1024} KB")
    httpd.shutdown()


if __name__ == "__main__":
    main()
