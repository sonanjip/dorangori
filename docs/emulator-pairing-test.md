# Paired emulator test — 2026-11-14

Devices: `dori_phone36` (Google Play Android 16/API 36, emulator-5554) and `dori_wear36` (Wear OS 6/API 36, emulator-5556). Both use the existing debug APKs. No source/build changes were required. Pixel Watch companion was installed from Play Store using the phone account, then its EmulatorActivity paired over the Studio-style ADB bridge. Both nodes reported connected/enabled and the companion logged `EMULATOR_PAIRING:SUCCESS`.

## Connected at recording stop — passed

- Launched the watch recorder, confirmed microphone foreground service and `.part` file, then launched again to stop.
- `dorangori_20261114_063141.m4a`: phone received the 16.128-second recording; watch copy disappeared after acknowledgement. In manual mode the phone posted a new-recording notification. This emulator recording contains no speech.
- Switched the phone to immediate transcription. Copied the existing Korean spoken fixture into the watch recording folder as `dorangori_20261114_063300.m4a` and resumed the watch app to queue it through its normal Data Layer sender (not the phone debug-ingest receiver).
- Phone ASR: DONE, 5696 ms, nonempty Korean transcript. SHA-256 of received audio matched the fixture; one DB row; watch file removed. Result notification present.

## Disconnected at recording stop, then reconnected — passed

- Removed both ADB bridge mappings and refreshed the watch transport. Pairing status was `Peer:[848206cd,false,true]`.
- Started a new watch recording, navigated Home while recording, then reopened to stop. `dorangori_20261114_063305.m4a` remained on watch; it was absent on phone.
- Also queued Korean fixture `dorangori_20261114_063400.m4a` on watch while disconnected and returned watch Home.
- Restored the ADB bridge and refreshed the transport, without reopening Dorangori on either device. Both files arrived automatically and disappeared from watch. Phone states: DONE; real recording 4096 ms (no speech), fixture 5696 ms (Korean transcript). Fixture SHA-256 matched and its DB row count was one. Result notifications present.

## Scope and cleanup

ADB-bridge interruption simulates loss of the Data Layer transport; real Samsung Bluetooth/reconnection, hardware keys, microphone content and battery restrictions still need physical-device checks. A spoken fixture was used to distinguish transport/ASR verification from the emulator's silent microphone. The phone transcription mode was restored to its original manual setting. Both emulators were stopped and the local MLX service restored after testing.

Host memory free percentage remained around 78–79% during pair tests with MLX unloaded; no Gradle builds ran alongside the emulators. Raw diagnostic output is kept in ignored `build/pairing-check/20261114/`.
