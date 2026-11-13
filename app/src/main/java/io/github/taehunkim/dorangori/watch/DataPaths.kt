package io.github.taehunkim.dorangori.watch

/** Shared with the watch app (`io.github.taehunkim.dorangori.wear.transfer.DataPaths`): keep the two in step. */
object DataPaths {
    const val RECORDING_PREFIX = "/rec/"
    const val KEY_AUDIO = "audio"
    const val KEY_RECORDED_AT = "recordedAt"
    private val NAME = Regex("^dorangori_\\d{8}_\\d{6}\\.m4a$")

    fun nameOf(path: String): String? = path.takeIf { it.startsWith(RECORDING_PREFIX) }?.removePrefix(RECORDING_PREFIX)?.takeIf { isValidName(it) }

    /** The name arrives over the data layer: only our own pattern is accepted, so it can never point outside the recordings folder. */
    fun isValidName(name: String): Boolean = NAME.matches(name)
}
