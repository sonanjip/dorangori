package io.github.taehunkim.dorangori.watch

import java.io.File
import java.io.IOException
import java.io.InputStream

/** Publish a complete, nonempty recording atomically; failed retries must never truncate a good copy. */
internal object IncomingAudio {
    fun copy(input: InputStream, target: File) {
        val tmp = File.createTempFile("incoming-", ".part", target.parentFile)
        try {
            // Asset descriptors can be duplicated from a cached file with its shared offset at EOF.
            // We need the whole asset. Pipes are not seekable and should retain normal streaming behavior.
            if (input is java.io.FileInputStream) {
                try { input.channel.position(0) } catch (_: IOException) { /* pipe */ }
            }
            val bytes = input.use { source -> tmp.outputStream().use { source.copyTo(it) } }
            if (bytes == 0L) throw IOException("empty recording asset")
            if (!tmp.renameTo(target)) throw IOException("cannot publish ${target.name}")
        } finally { tmp.delete() }
    }
}
