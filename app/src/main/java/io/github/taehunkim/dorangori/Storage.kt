package io.github.taehunkim.dorangori

import android.content.Context
import java.io.File

object Storage {
    fun dir(context: Context): File = File(context.filesDir, "recordings").also { it.mkdirs() }
}
