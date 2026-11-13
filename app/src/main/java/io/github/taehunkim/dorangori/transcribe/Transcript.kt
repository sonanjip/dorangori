package io.github.taehunkim.dorangori.transcribe

import io.github.taehunkim.dorangori.audio.Cue
import org.json.JSONArray
import org.json.JSONObject

object Transcript {
    /** The cues as one text: one line per utterance, trimmed, empty ones dropped. */
    fun join(cues: List<Cue>): String = cues.map { it.text.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

    fun toJson(cues: List<Cue>): String = JSONArray().also { a ->
        cues.forEach { a.put(JSONObject().put("s", it.startMs).put("e", it.endMs).put("t", it.text)) }
    }.toString()

    fun fromJson(json: String?): List<Cue> {
        if (json.isNullOrBlank()) return emptyList()
        val a = JSONArray(json)
        return List(a.length()) { i -> a.getJSONObject(i).let { Cue(it.getLong("s"), it.getLong("e"), it.getString("t")) } }
    }
}
