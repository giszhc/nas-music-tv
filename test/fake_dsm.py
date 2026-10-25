#!/usr/bin/env python3
"""本地假 DSM 服务：模拟 FileStation 登录/列目录/下载，用于真机链路验证。"""
import json
import os
import re
import sys
import wave
import math
import struct
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

ROOT = "/tmp/yy_music"
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 5000
SCROLL_TRACKS = int(os.environ.get("YY_SCROLL_TRACKS", "0"))
VIRTUAL_TRACKS = int(os.environ.get("YY_VIRTUAL_TRACKS", "0"))


def tone(path, seconds, freq, rate=44100):
    with wave.open(path, "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        frames = bytearray()
        for i in range(int(rate * seconds)):
            v = int(12000 * math.sin(2 * math.pi * freq * i / rate)
                    * (0.5 + 0.5 * math.sin(2 * math.pi * 0.5 * i / rate)))
            frames += struct.pack("<h", v)
        w.writeframes(bytes(frames))


def seed():
    os.makedirs(os.path.join(ROOT, "专辑"), exist_ok=True)
    specs = [("夜曲.wav", 36, 440), ("月光.wav", 28, 523), ("Ocean Drive.wav", 42, 330),
             ("专辑/雪落下的声音.wav", 25, 392), ("专辑/小镇姑娘.wav", 31, 294)]
    for name, sec, freq in specs:
        p = os.path.join(ROOT, name)
        if not os.path.exists(p):
            tone(p, sec, freq)
            print("made", p)


    import shutil
    for i in range(1,SCROLL_TRACKS+1):
        target=os.path.join(ROOT,"测试歌手%02d - 滚动测试%02d.wav"%(i,i))
        if not os.path.exists(target):shutil.copyfile(os.path.join(ROOT,"夜曲.wav"),target)

def entry(name, isdir, size=0):
    return {"name": name, "path": "/music/" + name, "isdir": isdir,
            "additional": {"size": size, "time": {"mtime": 1700000000}}}


class H(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        sys.stderr.write("[dsm] " + (fmt % args) + "\n")

    def _json(self, obj, code=200):
        body = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        u = urlparse(self.path)
        q = {k: v[0] for k, v in parse_qs(u.query).items()}
        api = q.get("api", "")
        method = q.get("method", "")

        if u.path == "/" and not api:
            body = "<html><title>home-dsm</title></html>".encode()
            self.send_response(200)
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        if api == "SYNO.API.Auth":
            if q.get("passwd") in ("", None):
                return self._json({"success": False, "error": {"code": 400}})
            return self._json({"success": True,
                               "data": {"sid": "TESTSID", "synotoken": "TESTTOKEN"}})

        if api == "SYNO.FileStation.List" and method == "list_share":
            return self._json({"success": True, "data": {"shares": [
                {"name": "music", "path": "/music"},
                {"name": "电影", "path": "/video"}]}})

        if api == "SYNO.FileStation.List" and method == "list":
            folder = q.get("folder_path", "")
            files = []
            if folder == "/music":
                files = [entry("专辑", True), entry("夜曲.wav", False, 3175 * 1024),
                         entry("月光.wav", False, 2470 * 1024),
                         entry("Ocean Drive.wav", False, 3704 * 1024)]
                files += [entry("测试歌手%02d - 滚动测试%02d.wav"%(i,i),False,os.path.getsize(os.path.join(ROOT,"夜曲.wav"))) for i in range(1,SCROLL_TRACKS+1)]
                files += [entry("长列表%05d.wav" % i,False,3175*1024) for i in range(1,VIRTUAL_TRACKS+1)]
            elif folder == "/music/专辑":
                files = [entry("雪落下的声音.wav", False, 2205 * 1024),
                         entry("小镇姑娘.wav", False, 2734 * 1024)]
            return self._json({"success": True,
                               "data": {"files": files, "total": len(files)}})

        if api == "SYNO.FileStation.Download":
            m = re.search(r'"([^"]+)"', q.get("path", ""))
            rel = (m.group(1) if m else "").replace("/music/", "", 1)
            if rel.startswith("长列表") and rel.endswith(".wav"): rel="夜曲.wav"
            full = os.path.join(ROOT, rel)
            if not os.path.exists(full):
                return self._json({"success": False, "error": {"code": 408}}, 404)
            self._file(full)
            return

        return self._json({"success": False, "error": {"code": 1000}}, 400)

    def _file(self, full):
        size = os.path.getsize(full)
        start, end = 0, size - 1
        rng = self.headers.get("Range")
        if rng:
            m = re.match(r"bytes=(\d*)-(\d*)", rng)
            if m:
                if m.group(1):
                    start = int(m.group(1))
                if m.group(2):
                    end = int(m.group(2))
        length = end - start + 1
        self.send_response(206 if rng else 200)
        self.send_header("Content-Type", "audio/wav")
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(length))
        if rng:
            self.send_header("Content-Range", "bytes %d-%d/%d" % (start, end, size))
        self.end_headers()
        with open(full, "rb") as f:
            f.seek(start)
            remaining = length
            while remaining > 0:
                chunk = f.read(min(65536, remaining))
                if not chunk:
                    break
                try:
                    self.wfile.write(chunk)
                except (BrokenPipeError, ConnectionResetError):
                    break
                remaining -= len(chunk)


if __name__ == "__main__":
    seed()
    print("fake DSM on :%d root=%s" % (PORT, ROOT), flush=True)
    ThreadingHTTPServer(("0.0.0.0", PORT), H).serve_forever()
