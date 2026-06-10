package com.example.wikipedia_app.data

import android.util.Log
import org.json.JSONObject

/**
 * Emits structured Wikirace events to logcat under the "WikiViz" tag.
 *
 * The companion visualiser (see /gameviz) tails `adb logcat -s WikiViz:I`,
 * streams the events to a browser over SSE, and draws the article graph live:
 * nodes = articles, edges = the hyperlinks you traverse. Prefetch events are
 * emitted too, so the background cache-warming shows up as faint edges.
 *
 * This is purely a developer/demo hook — in production it's just a log line.
 */
object GameViz {
    private const val TAG = "WikiViz"

    private fun emit(obj: JSONObject) {
        Log.i(TAG, obj.toString())
    }

    /** A new game began: the random start article and the target to reach. */
    fun start(startArticle: String, target: String) = emit(
        JSONObject()
            .put("type", "start")
            .put("article", startArticle)
            .put("target", target)
    )

    /** The player followed a hyperlink from one article to another. */
    fun hop(from: String, to: String, steps: Int, won: Boolean) = emit(
        JSONObject()
            .put("type", "hop")
            .put("from", from)
            .put("to", to)
            .put("steps", steps)
            .put("won", won)
    )

    /** The player used the in-game Back button, returning to [to]. */
    fun back(to: String) = emit(
        JSONObject().put("type", "back").put("to", to)
    )

    /** A background prefetch of [to] (a link of [from]) has started. */
    fun prefetch(from: String, to: String) = emit(
        JSONObject().put("type", "prefetch").put("from", from).put("to", to)
    )

    /** A background prefetch of [to] finished and is now cached. */
    fun prefetchDone(from: String, to: String) = emit(
        JSONObject().put("type", "prefetch_done").put("from", from).put("to", to)
    )
}
