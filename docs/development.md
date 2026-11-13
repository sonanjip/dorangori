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
