package io.github.taehunkim.dorangori.wear

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.platform.app.InstrumentationRegistry
import io.github.taehunkim.dorangori.wear.recording.RecordingFiles
import io.github.taehunkim.dorangori.wear.ui.RecorderScreen
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Regression: a phone acknowledgement deletes a file without changing the recorder state. */
class PendingRecordingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun acknowledgementRemovesPendingLabelWithoutReopening() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = RecordingFiles.dir(context)
        val baseline = RecordingFiles.pending(dir).size
        val file = File(dir, "dorangori_20991231_235959.m4a")
        val label = context.getString(R.string.pending_count, baseline + 1)
        try {
            file.writeBytes(byteArrayOf(1))
            compose.setContent { RecorderScreen {} }
            compose.waitUntil(10_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            file.delete() // what the acknowledgement listener does after phone receipt
            compose.waitUntil(10_000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isEmpty() }
        } finally { file.delete() }
    }
}
