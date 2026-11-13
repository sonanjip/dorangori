package io.github.taehunkim.dorangori.wear.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneId

class RecordingFilesTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val at = Instant.parse("2026-11-13T05:07:09Z") // 14:07:09 in Seoul

    @Test fun nameUsesLocalTime() {
        assertEquals("dorangori_20261113_140709.m4a", RecordingFiles.nameFor(at, seoul))
    }

    @Test fun parseRoundTrips() {
        assertEquals(at, RecordingFiles.parse(RecordingFiles.nameFor(at, seoul), seoul))
    }

    @Test fun parseRejectsForeignAndUnfinishedNames() {
        assertNull(RecordingFiles.parse("dorangori_20261113_140709.m4a.part", seoul))
        assertNull(RecordingFiles.parse("voice.m4a", seoul))
        assertNull(RecordingFiles.parse("dorangori_2026_bad.m4a", seoul))
    }

    @Test fun uniqueNameSkipsTakenSeconds() {
        val dir = kotlin.io.path.createTempDirectory("dori").toFile()
        try {
            File(dir, RecordingFiles.nameFor(at, seoul)).writeText("x")
            val name = RecordingFiles.uniqueName(dir, at, seoul)
            assertNotEquals(RecordingFiles.nameFor(at, seoul), name)
            assertEquals(at.plusSeconds(1), RecordingFiles.parse(name, seoul))
        } finally { dir.deleteRecursively() }
    }

    @Test fun pendingListsOnlyFinishedFilesOldestFirst() {
        val dir = kotlin.io.path.createTempDirectory("dori").toFile()
        try {
            val later = File(dir, RecordingFiles.nameFor(at.plusSeconds(60), seoul)).also { it.writeText("b") }
            val first = File(dir, RecordingFiles.nameFor(at, seoul)).also { it.writeText("a") }
            File(dir, RecordingFiles.nameFor(at.plusSeconds(5), seoul) + ".part").writeText("c")
            assertEquals(listOf(first, later), RecordingFiles.pending(dir))
            RecordingFiles.deleteStaleParts(dir)
            assertEquals(2, dir.listFiles()!!.size)
        } finally { dir.deleteRecursively() }
    }
}
