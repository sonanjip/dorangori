package io.github.taehunkim.dorangori.watch

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files

class IncomingAudioTest {
    @Test fun fileBackedAssetIsReadFromStartEvenIfDescriptorWasPreviouslyConsumed() {
        val dir = Files.createTempDirectory("incoming-offset").toFile()
        try {
            val source = java.io.File(dir, "source.m4a").apply { writeText("complete audio") }
            val target = java.io.File(dir, "target.m4a")
            java.io.FileInputStream(source).use { input ->
                input.channel.position(source.length())
                IncomingAudio.copy(input, target)
            }
            assertEquals("complete audio", target.readText())
        } finally { dir.deleteRecursively() }
    }

    @Test fun emptyDeliveryMustNotCreateAFileThatCouldBeAcknowledged() {
        val dir = Files.createTempDirectory("incoming").toFile()
        try {
            val target = java.io.File(dir, "audio.m4a")
            val result = runCatching { IncomingAudio.copy(ByteArrayInputStream(byteArrayOf()), target) }
            assertTrue("empty assets must fail so the watch keeps its copy", result.isFailure)
            assertFalse(target.exists())
            assertEquals(0, dir.listFiles()!!.size)
        } finally { dir.deleteRecursively() }
    }
    @Test fun failedRetryMustPreserveThePreviouslyReceivedAudio() {
        val dir = Files.createTempDirectory("incoming").toFile()
        try {
            val target = java.io.File(dir, "audio.m4a").apply { writeText("valid recording") }
            runCatching { IncomingAudio.copy(ByteArrayInputStream(byteArrayOf()), target) }
            assertEquals("valid recording", target.readText())
            assertEquals(listOf("audio.m4a"), dir.list()!!.toList())
        } finally { dir.deleteRecursively() }
    }
    @Test fun validDeliveryIsCompleteAndFailedStreamLeavesNoPartialFile() {
        val dir = Files.createTempDirectory("incoming").toFile()
        try {
            val target = java.io.File(dir, "audio.m4a")
            IncomingAudio.copy(ByteArrayInputStream(ByteArray(8192) { (it % 251).toByte() }), target)
            assertEquals(8192L, target.length())
            val broken = object : InputStream() { override fun read(): Int = throw IOException("lost stream") }
            runCatching { IncomingAudio.copy(broken, target) }
            assertEquals(8192L, target.length())
            assertEquals(listOf("audio.m4a"), dir.list()!!.toList())
        } finally { dir.deleteRecursively() }
    }
}
