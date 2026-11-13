package io.github.taehunkim.dorangori.transcribe

import io.github.taehunkim.dorangori.audio.Cue
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptTest {
    private val cues = listOf(Cue(0, 1000, " hello "), Cue(1000, 1500, "  "), Cue(1600, 3000, "world"))

    @Test fun joinTrimsAndDropsEmptyLines() {
        assertEquals("hello\nworld", Transcript.join(cues))
    }

    @Test fun jsonRoundTrips() {
        assertEquals(cues, Transcript.fromJson(Transcript.toJson(cues)))
    }

    @Test fun missingJsonIsEmpty() {
        assertEquals(emptyList<Cue>(), Transcript.fromJson(null))
        assertEquals(emptyList<Cue>(), Transcript.fromJson(""))
    }
}
