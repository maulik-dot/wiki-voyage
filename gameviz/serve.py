#!/usr/bin/env python3
"""
Wiki-The-Racer live graph bridge.

Tails `adb logcat -s WikiViz:I` from the running app, parses the structured
game events, and streams them to the browser over Server-Sent Events (SSE).
Open http://localhost:8000 and start a game in the app to watch the article
graph build itself in real time.

Usage:
    python3 gameviz/serve.py            # port 8000, auto-opens browser
    python3 gameviz/serve.py --port 9000 --no-open
    ANDROID_SERIAL=emulator-5554 python3 gameviz/serve.py   # pick a device
"""
import argparse
import json
import os
import queue
import shutil
import subprocess
import sys
import threading
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HERE = os.path.dirname(os.path.abspath(__file__))
TAG = "WikiViz"

# Subscribers (SSE clients) and the event history for the current game.
_subscribers = []
_history = []          # raw event dicts since the last "start"
_lock = threading.Lock()


def find_adb():
    """Locate the adb binary from PATH or common SDK locations."""
    cand = shutil.which("adb")
    if cand:
        return cand
    home = os.path.expanduser("~")
    for p in [
        os.environ.get("ANDROID_HOME", ""),
        os.environ.get("ANDROID_SDK_ROOT", ""),
        os.path.join(home, "Library/Android/sdk"),
        os.path.join(home, "Android/Sdk"),
        os.path.join(home, "AppData/Local/Android/Sdk"),
    ]:
        if p:
            adb = os.path.join(p, "platform-tools", "adb")
            for ext in ("", ".exe"):
                if os.path.isfile(adb + ext):
                    return adb + ext
    return None


def broadcast(event: dict):
    """Push an event to every connected browser; reset history on 'start'."""
    with _lock:
        if event.get("type") == "start":
            _history.clear()
        _history.append(event)
        dead = []
        for q in _subscribers:
            try:
                q.put_nowait(event)
            except Exception:
                dead.append(q)
        for q in dead:
            _subscribers.remove(q)


def logcat_reader(adb: str):
    """Background thread: read logcat lines and broadcast WikiViz events."""
    serial = os.environ.get("ANDROID_SERIAL")
    base = [adb] + (["-s", serial] if serial else [])
    try:
        subprocess.run(base + ["logcat", "-c"], check=False)
    except Exception as e:
        print(f"[bridge] could not clear logcat: {e}")
    proc = subprocess.Popen(
        base + ["logcat", "-s", f"{TAG}:I"],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        bufsize=1, text=True, encoding="utf-8", errors="replace",
    )
    print(f"[bridge] tailing logcat for tag '{TAG}' … start a game in the app.")
    for line in proc.stdout:
        if TAG not in line:
            continue
        # Robust against tag-format variations ("WikiViz:" vs "WikiViz :"):
        # the payload is a JSON object, so just slice from the first { to last }.
        i, j = line.find("{"), line.rfind("}")
        if i == -1 or j <= i:
            continue
        try:
            event = json.loads(line[i:j + 1])
        except json.JSONDecodeError:
            continue
        print(f"[event] {event}")
        broadcast(event)


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *a):
        pass  # quiet

    def do_GET(self):
        if self.path.split("?")[0] == "/events":
            return self.handle_sse()
        if self.path in ("/", "/index.html"):
            return self.serve_file("index.html", "text/html; charset=utf-8")
        self.send_error(404)

    def serve_file(self, name, ctype):
        path = os.path.join(HERE, name)
        try:
            with open(path, "rb") as f:
                data = f.read()
        except OSError:
            return self.send_error(404)
        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def handle_sse(self):
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Cache-Control", "no-cache")
        self.send_header("Connection", "keep-alive")
        self.end_headers()
        q = queue.Queue()
        with _lock:
            snapshot = list(_history)
            _subscribers.append(q)
        try:
            # Replay current game so a fresh tab catches up.
            for ev in snapshot:
                self._send(ev)
            while True:
                try:
                    ev = q.get(timeout=15)
                    self._send(ev)
                except queue.Empty:
                    self.wfile.write(b": keepalive\n\n")
                    self.wfile.flush()
        except (BrokenPipeError, ConnectionResetError):
            pass
        finally:
            with _lock:
                if q in _subscribers:
                    _subscribers.remove(q)

    def _send(self, event: dict):
        self.wfile.write(f"data: {json.dumps(event)}\n\n".encode("utf-8"))
        self.wfile.flush()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8000)
    ap.add_argument("--no-open", action="store_true")
    args = ap.parse_args()

    adb = find_adb()
    if not adb:
        print("ERROR: adb not found. Install platform-tools or set ANDROID_HOME.")
        sys.exit(1)

    threading.Thread(target=logcat_reader, args=(adb,), daemon=True).start()

    url = f"http://localhost:{args.port}"
    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    print(f"[bridge] serving {url}  (Ctrl-C to stop)")
    if not args.no_open:
        threading.Timer(0.6, lambda: webbrowser.open(url)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n[bridge] bye")


if __name__ == "__main__":
    main()
