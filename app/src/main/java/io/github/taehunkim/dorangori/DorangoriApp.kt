package io.github.taehunkim.dorangori

import android.app.Application
import androidx.room.Room
import io.github.taehunkim.dorangori.data.RecordingRepository
import io.github.taehunkim.dorangori.data.db.ALL_MIGRATIONS
import io.github.taehunkim.dorangori.data.db.AppDatabase
import io.github.taehunkim.dorangori.data.prefs.SettingsStore
import io.github.taehunkim.dorangori.ml.ModelHost
import io.github.taehunkim.dorangori.notify.JogakboSaver
import io.github.taehunkim.dorangori.notify.Notifications
import io.github.taehunkim.dorangori.retention.RetentionWorker
import io.github.taehunkim.dorangori.transcribe.TranscribeScheduler
import io.github.taehunkim.dorangori.transcribe.Transcriber
import io.github.taehunkim.dorangori.watch.Intake
import io.github.taehunkim.ondolsaem.sdk.IdleState
import io.github.taehunkim.ondolsaem.sdk.ModelChangedReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Everything the screens, receivers and workers share. */
class AppContainer(context: android.content.Context) {
    private val app = context.applicationContext
    val db: AppDatabase by lazy { Room.databaseBuilder(app, AppDatabase::class.java, "dorangori.db").addMigrations(*ALL_MIGRATIONS).build() }
    val dao by lazy { db.recordings() }
    val settings = SettingsStore(app)
    val models = ModelHost(app)
    val repo by lazy { RecordingRepository(app, dao) }
    val transcriber by lazy { Transcriber(app, dao, models) }
    val scheduler by lazy { TranscribeScheduler(app, settings, dao) }
    val jogakboSaver by lazy { JogakboSaver(app) }
}

class DorangoriApp : Application() {
    lateinit var container: AppContainer
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannels(this)
        IdleState(this).watchScreen()
        ModelChangedReceiver.handler = { scope.launch { container.scheduler.onModelChanged() } }
        scope.launch {
            // A recording left "transcribing" by a kill is waiting again; then follow the settings.
            container.dao.resetStuck()
            container.scheduler.start(this)
            container.settings.retentionDays.distinctUntilChanged().collect { RetentionWorker.apply(this@DorangoriApp, it) }
        }
        // Anything the watch queued while the app was gone (the listener service normally gets it first).
        scope.launch(Dispatchers.IO) { Intake.pull(this@DorangoriApp) }
    }
}
