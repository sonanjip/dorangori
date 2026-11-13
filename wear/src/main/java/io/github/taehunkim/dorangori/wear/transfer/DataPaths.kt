package io.github.taehunkim.dorangori.wear.transfer

/** Shared with the phone app (`io.github.taehunkim.dorangori.watch.DataPaths`): keep the two in step. */
object DataPaths {
    const val RECORDING_PREFIX = "/rec/"
    const val KEY_AUDIO = "audio"
    const val KEY_RECORDED_AT = "recordedAt"
    const val KEY_NAME = "name"
    /** Capability the phone app declares in `res/values/wear.xml`, so the watch can tell the app is installed. */
    const val CAPABILITY_PHONE = "dorangori_phone"

    fun pathFor(fileName: String) = RECORDING_PREFIX + fileName
    fun nameOf(path: String): String? = path.takeIf { it.startsWith(RECORDING_PREFIX) }?.removePrefix(RECORDING_PREFIX)?.takeIf { it.isNotEmpty() }
}
