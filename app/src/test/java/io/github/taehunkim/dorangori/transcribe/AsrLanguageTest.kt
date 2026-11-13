package io.github.taehunkim.dorangori.transcribe

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AsrLanguageTest {
    @Test fun automaticAndFixedLanguagesDoNotFollowDevice() {
        assertEquals("auto", AsrLanguage.AUTO.resolve(Locale.KOREAN))
        assertEquals("ko", AsrLanguage.KOREAN.resolve(Locale.ENGLISH))
        assertEquals("en", AsrLanguage.ENGLISH.resolve(Locale.KOREAN))
        assertEquals("ja", AsrLanguage.JAPANESE.resolve(Locale.KOREAN))
        assertEquals("zh", AsrLanguage.CHINESE.resolve(Locale.KOREAN))
        assertEquals("yue", AsrLanguage.CANTONESE.resolve(Locale.KOREAN))
    }
    @Test fun deviceLanguageUsesProvidedSystemLocaleNotJvmAppLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH) // app may be English while the system is Korean
            assertEquals("ko", AsrLanguage.SYSTEM.resolve(Locale.KOREAN))
        } finally { Locale.setDefault(previous) }
    }
    @Test fun deviceLocaleSupportsRegionsAndFallsBackWhenUnsupported() {
        mapOf("en-US" to "en", "ko-KR" to "ko", "ja-JP" to "ja", "zh-CN" to "zh", "zh-TW" to "zh", "zh-HK" to "yue", "zh-MO" to "yue", "yue-Hant-HK" to "yue", "fr-FR" to "auto").forEach { (tag, expected) ->
            assertEquals(tag, expected, AsrLanguage.SYSTEM.resolve(Locale.forLanguageTag(tag)))
        }
    }
    @Test fun unsetOrObsoletePreferenceDefaultsToAuto() {
        assertEquals(AsrLanguage.AUTO, AsrLanguage.of(null))
        assertEquals(AsrLanguage.AUTO, AsrLanguage.of("removed"))
    }
}
