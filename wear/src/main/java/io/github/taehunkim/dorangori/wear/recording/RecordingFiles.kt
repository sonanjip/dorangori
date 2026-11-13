package io.github.taehunkim.dorangori.wear.recording

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Naming and location of the recordings kept on the watch until the phone confirms receipt.
 * A finished recording is `dorangori_yyyyMMdd_HHmmss.m4a` (device local time); while it is being written it carries a
 * `.part` suffix, so an interrupted recording is never mistaken for a finished one.
 */
object RecordingFiles {
    const val PREFIX = "dorangori_"
    const val EXT = "m4a"
    private const val PART = ".part"
    private val FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun nameFor(at: Instant, zone: ZoneId = ZoneId.systemDefault()): String = "$PREFIX${FORMAT.format(at.atZone(zone))}.$EXT"

    /** The moment encoded in a finished recording's name, or null when [name] is not one of ours. */
    fun parse(name: String, zone: ZoneId = ZoneId.systemDefault()): Instant? {
        if (!name.startsWith(PREFIX) || !name.endsWith(".$EXT")) return null
        val stamp = name.removePrefix(PREFIX).removeSuffix(".$EXT")
        return try { LocalDateTime.parse(stamp, FORMAT).atZone(zone).toInstant() } catch (_: DateTimeParseException) { null }
    }

    fun dir(context: Context): File = File(context.filesDir, "recordings").also { it.mkdirs() }

    /** A name for a recording started at [at] that does not clash with a file in [dir] (two starts in one second move on by a second). */
    fun uniqueName(dir: File, at: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        var t = at
        while (File(dir, nameFor(t, zone)).exists() || File(dir, nameFor(t, zone) + PART).exists()) t = t.plusSeconds(1)
        return nameFor(t, zone)
    }

    fun partFor(final: File): File = File(final.parentFile, final.name + PART)

    /** Finished recordings waiting to be sent, oldest first. */
    fun pending(dir: File): List<File> =
        dir.listFiles { f -> f.isFile && parse(f.name) != null }.orEmpty().sortedBy { it.name }

    /** Leftovers of a recording that was cut short (process killed): unplayable, so they are removed. */
    fun deleteStaleParts(dir: File) {
        dir.listFiles { f -> f.name.endsWith(PART) }.orEmpty().forEach { it.delete() }
    }
}
