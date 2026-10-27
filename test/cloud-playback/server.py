"""隔离的云盘 HTTP 夹具，不使用真实云盘账号。"""
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from pathlib import Path
from urllib.parse import urlsplit
import time, subprocess
AUDIO=Path('/tmp/yy-cloud-tone.mp3')
if not AUDIO.exists():
    subprocess.run(['ffmpeg','-v','error','-f','lavfi','-i','sine=frequency=440:duration=35','-c:a','libmp3lame','-b:a','192k','-y',str(AUDIO)],check=True)
DATA=bytes(i%251 for i in range(700000))
class Handler(BaseHTTPRequestHandler):
    def log_message(self,*args):pass
    def do_GET(self):
        path=urlsplit(self.path).path
        if self.headers.get('Referer')!='https://www.alipan.com/' or not self.headers.get('User-Agent','').startswith('Mozilla/'):
            self.send_error(403);return
        if path in ['/redirect','/redirect-data']:
            self.send_response(308);self.send_header('Location','/audio' if path=='/redirect' else '/data');self.send_header('Content-Length','0');self.end_headers();return
        if path in ['/denied','/rate']:
            self.send_error(403 if path=='/denied' else 429);return
        if path=='/html':
            self.send_response(200);self.send_header('Content-Type','text/html');self.send_header('Content-Length','13');self.end_headers();self.wfile.write(b'<html></html>');return
        if path=='/slow-header':time.sleep(12)
        data=AUDIO.read_bytes() if path in ['/audio','/jitter'] else DATA
        start=0
        if self.headers.get('Range','').startswith('bytes=') and path!='/no-range':start=int(self.headers['Range'][6:].split('-')[0])
        partial=path!='/no-range'
        self.send_response(206 if partial else 200)
        self.send_header('Content-Type','audio/mpeg' if path in ['/audio','/jitter'] else 'application/octet-stream')
        if path!='/unknown':self.send_header('Content-Length',str(len(data)-start))
        if partial:self.send_header('Content-Range',f'bytes {start+1 if path=="/bad-range" else start}-{len(data)-1}/{"*" if path=="/unknown" else len(data)}')
        self.end_headers()
        try:
            if path=='/stall':self.wfile.write(data[start:start+1]);self.wfile.flush();time.sleep(12);return
            if path=='/truncated':self.wfile.write(data[start:start+100]);return
            for off in range(start,len(data),16384):
                self.wfile.write(data[off:off+16384]);self.wfile.flush()
                if path=='/jitter' and off%65536==0:time.sleep(.25)
        except (BrokenPipeError,ConnectionResetError):pass
if __name__=='__main__':ThreadingHTTPServer(('0.0.0.0',18083),Handler).serve_forever()
