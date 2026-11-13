package io.github.taehunkim.dorangori

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import io.github.taehunkim.dorangori.ui.AppScaffold
import io.github.taehunkim.dorangori.ui.theme.DorangoriTheme

class MainActivity : ComponentActivity() {
    /** Set when a notification opened a recording; the screen shows it once and clears it. */
    private var openId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) takeRequest(intent)
        setContent {
            DorangoriTheme {
                val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                AppScaffold(openId = openId, onOpened = { openId = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeRequest(intent)
    }

    private fun takeRequest(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_RECORDING) openId = intent.getLongExtra(EXTRA_ID, 0L).takeIf { it > 0 }
    }

    companion object {
        const val ACTION_OPEN_RECORDING = "io.github.taehunkim.dorangori.action.OPEN_RECORDING"
        const val EXTRA_ID = "id"
    }
}
