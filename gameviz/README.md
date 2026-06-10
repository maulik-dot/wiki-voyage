# Wiki-The-Racer — live game graph

Watch the Wikirace build itself as a graph: **nodes are articles, edges are the
hyperlinks you follow**. Background prefetches show up as faint dashed edges, so
you can literally see the app's async cache-warming run ahead of your clicks.

```
 ┌──────────────┐   logcat (tag: WikiViz)   ┌───────────┐   SSE    ┌─────────────┐
 │  Android app │ ────────────────────────▶ │ serve.py  │ ───────▶ │  index.html │
 │  GameViz.kt  │   structured JSON events  │ (bridge)  │  /events │ vis-network │
 └──────────────┘                           └───────────┘          └─────────────┘
```

## How it works

The app calls `GameViz` (see `app/.../data/GameViz.kt`) at key moments and logs a
one-line JSON event under the `WikiViz` tag:

| When                                   | Event |
|----------------------------------------|-------|
| Game starts                            | `{"type":"start","article":…,"target":…}` |
| You tap a hyperlink                    | `{"type":"hop","from":…,"to":…,"steps":n,"won":bool}` |
| In-game Back                           | `{"type":"back","to":…}` |
| Background prefetch begins / finishes  | `{"type":"prefetch"…}` / `{"type":"prefetch_done"…}` |

`serve.py` tails `adb logcat -s WikiViz:I`, parses those lines, and streams them
to the browser over Server-Sent Events. `index.html` adds/upgrades nodes and
edges live.

## Run it

1. **Run the app** from Android Studio (or `./gradlew installDebug`) on a
   connected device/emulator.
2. **Start the bridge** (needs Python 3 and `adb` on PATH or `ANDROID_HOME` set):
   ```bash
   python3 gameviz/serve.py
   ```
   It opens <http://localhost:8000> automatically.
3. In the app, open **Wikirace** and start a game. The start and 🎯 target nodes
   appear; every link you tap draws a new edge; faint edges show prefetching.

### Options
```bash
python3 gameviz/serve.py --port 9000 --no-open
ANDROID_SERIAL=emulator-5554 python3 gameviz/serve.py   # if multiple devices
```

## Legend
- **green** start · **gold star (dashed)** target · **blue** visited ·
  **gold star** reached 🏁
- **faint grey dot + dashed edge** = prefetch in flight (async) ·
  **light-blue + dashed edge** = prefetched & cached (warm, ready for an instant hop)
- A faint prefetch edge turning **solid blue** = you clicked a link the app had
  already warmed (cache hit).

> The visualiser is a dev/demo hook. In a normal build the events are just log
> lines with no effect on the app.
