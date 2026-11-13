package io.github.taehunkim.dorangori.wear.recording

import android.content.Context

/** Small watch-side preferences. */
object WatchSettings {
    const val DEFAULT_MAX_MINUTES = 15
    private const val FILE = "watch_settings"
    private const val KEY_AUTO_START = "auto_start"
    private const val KEY_MAX_MINUTES = "max_minutes"

    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Start recording right away when the app is opened from the launcher or an assigned button. Default on. */
    fun autoStart(context: Context): Boolean = prefs(context).getBoolean(KEY_AUTO_START, true)
    fun setAutoStart(context: Context, value: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO_START, value).apply()

    /** A recording stops by itself after this long (Data Layer assets suit small files). */
    fun maxMinutes(context: Context): Int = prefs(context).getInt(KEY_MAX_MINUTES, DEFAULT_MAX_MINUTES)
}
