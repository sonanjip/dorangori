# Architecture

Two modules, one applicationId (`io.github.taehunkim.dorangori`) so the Wearable Data Layer pairs them.

## Watch (`:wear`)
- `RecordingService` (foreground, microphone) writes AAC m4a, 16 kHz mono, 32 kbps to `filesDir/recordings/dorangori_yyyyMMdd_HHmmss.m4a`. Default cap 15 minutes.
- The launch activity is `singleTop`: a launch while idle starts recording (setting "record when opened", default on), a launch while recording stops it. Hardware buttons are not intercepted; the user assigns one to "open Dorangori" in the watch settings.
- Transfer: `DataClient.putDataItem("/rec/<file name>")` with an `Asset` plus metadata (recorded-at epoch, duration, mime). The Data Layer queues it until the phone is reachable. The file name is the key, so re-sending is idempotent.
- Acknowledgement: the phone deletes the DataItem after saving the file; the watch's `WearableListenerService` sees TYPE_DELETED and removes the local file.

## Phone (`:app`)
- `WearableListenerService` copies the Asset to `filesDir/recordings/`, inserts a Room row, deletes the DataItem, then follows the transcription mode.
- `RecordingEntity` (Room, no destructive fallback): file, recordedAt, durationMs, state (RECEIVED, TRANSCRIBING, DONE, FAILED, NO_MODEL), text, cuesJson, ASR model id and version, Jogakbo note id.
- Transcription: m4a is decoded to 16 kHz mono 16-bit PCM and passed to `OndolSaemClient.transcribe`. Modes: immediate (default), scheduled, idle, manual.
- Notification actions: copy (receiver writes the clipboard), ask Malbotdari (`ACTION_SEND text/plain` to its package), save to Jogakbo (Jogakbo SDK `createNote`, audio + transcript as one note).
- Settings: transcription timing, auto-delete period, notification and battery hints, watch and OndolSaem status.

## Request-scoped ASR language

`SettingsStore.asrLanguage` stores `AsrLanguage` separately from UI localization. `Transcriber` resolves SYSTEM using `LocaleManager.systemLocales` on API 33+ (system Resources earlier), snapshots the language at transcription time, and forwards a model code. `ModelHost` uses the legacy SDK call for auto; explicit languages use SDK API V5 `transcribeWithLanguage` after a capability check. An older host yields an actionable update-required error. Existing recordings are not reprocessed automatically. There is no Room schema change.
