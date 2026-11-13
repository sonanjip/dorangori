package io.github.taehunkim.dorangori.transcribe

import java.util.Locale

/** UI locale and recognition language are independent. SYSTEM is resolved against Android's system configuration. */
enum class AsrLanguage(private val code: String) {
    AUTO("auto"), SYSTEM("auto"), KOREAN("ko"), ENGLISH("en"), JAPANESE("ja"), CHINESE("zh"), CANTONESE("yue");

    fun resolve(systemLocale: Locale): String {
        if (this != SYSTEM) return code
        return when (systemLocale.language) {
            "ko", "en", "ja", "yue" -> systemLocale.language
            "zh" -> if (systemLocale.country in setOf("HK", "MO")) "yue" else "zh"
            else -> "auto"
        }
    }

    companion object {
        fun of(name: String?): AsrLanguage = entries.firstOrNull { it.name == name } ?: AUTO
    }
}
