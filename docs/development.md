# Development

```bash
cd dorangori && source scripts/env.sh
./gradlew assembleDebug testDebugUnitTest
scripts/emu-start.sh --window   # phone AVD (5554) + watch AVD (5556)
scripts/run.sh both
```

- AVDs: `dori_phone36` (Google Play image) and `dori_wear36` (Wear OS 6). Pair them from Android Studio Device Manager ("Pair Wearable"); signing in to a Google account may be required.
- `local.properties`: `ondolsaem.dir=../ondolsaem`, `jogakbo.dir=../jogakbo` build the SDKs from sibling checkouts. Without them the JitPack artifacts (`com.github.sonanjip:ondolsaem:<tag>`) are used.
- Release signing uses the shared Nunsori release key through the `DORANGORI_STORE_*` / `DORANGORI_KEY_*` properties so OndolSaem and Jogakbo accept the app without asking.
- No watch needed for the phone side: a debug build accepts `adb shell am broadcast -a io.github.taehunkim.dorangori.debug.INGEST -n io.github.taehunkim.dorangori/.debug.DebugIngestReceiver --es path <file>` for a `dorangori_yyyyMMdd_HHmmss.m4a` inside the app's own files (copy it in with `run-as`, since the app cannot read `/data/local/tmp`). Make test speech with `say` + `ffmpeg -ac 1 -ar 16000 -c:a aac -b:a 32k`.
- The emulator needs OndolSaem installed with its speech model (Models tab) and Jogakbo 0.7.0 or later for "Save to Jogakbo".
- Run Gradle for one repository at a time. Real devices: enable ADB over Wi-Fi on the watch and set `PHONE_SERIAL` / `WEAR_SERIAL`.

## Paired emulator checks

See [2026-11-14 test report](emulator-pairing-test.md). This Wear OS 6 image specifies `com.google.android.apps.wear.companion` (Google Pixel Watch), not the legacy Wear OS companion. Install it from the phone Play Store, then use Android Studio Pair Wearable. Google login was used for the Play Store install; no account was copied to the watch for these checks.

For a manual bridge (the same mechanism used by Android Studio), forward phone TCP 5601 to host TCP 5602 and reverse watch TCP 5601 to host TCP 5602. Refresh the watch connection with the `com.google.android.gms.wearable.EMULATOR` broadcast, operation `refresh-emulator-connection`. First-time setup uses the companion's `.EmulatorActivity`, with `emulator-name` (AVD name) and `emulator-id` (watch node ID from the `get-pairing-status` operation). The user-facing consent screen must be completed. Check that both devices report the peer connected/enabled before testing.

To simulate losing the link, remove both bridge mappings and refresh the watch connection. Confirm its peer is disconnected, record and stop, and verify that only the watch has the new file. Restore the mappings and refresh; do not reopen Dorangori. Verify automatic phone receipt, one database row, ASR completion and removal of the watch file. This tests Data Layer queuing, not physical Bluetooth radio behaviour.

Build with emulators stopped and one Gradle worker as specified in the workspace AGENTS.md. Run the two emulators together only for pair tests after stopping the local model service, monitor memory, and shut both down before restoring that service.

Language-selection builds use OndolSaem SDK 0.7.0/API V5 (local composite checkout until its SDK tag is published). Install OndolSaem 0.9.0+ together with Dorangori 0.2.0 to use explicit languages. Pure language-resolution tests cover fixed/auto/system language, regions, Cantonese and unsupported-language fallback.
