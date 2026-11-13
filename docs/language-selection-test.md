# ASR language and icon verification — 2026-11-14

Versions: Dorangori 0.2.0; OndolSaem 0.9.0; SDK 0.7.0/API V5. Sequential low-memory builds with all emulators stopped: host app (89 tests) + SDK (15), Dorangori phone (14) + watch (6), Nunsori compatibility (170). All passed. Separate review found no significant issues.

Phone emulator only (`dori_phone36`), MLX unloaded, no concurrent build:

- HostServiceTest: 25 fake-engine Binder tests passed. Includes explicit en → ko → legacy auto with resume offsets, early callback cancellation, raw Binder unsupported language BAD_INPUT, existing API floors and older-client support.
- Opt-in real SenseVoice: 1 test passed. English fixture with Samantha voice and Korean fixture with Yuna voice were decoded to raw 16 kHz mono PCM. Auto → en → ko → legacy auto ran successfully. English automatic and explicit outputs were both `Remember to buy milk and call the dentist tomorrow morning.` Automatic output after explicit requests was identical to the initial output. Korean produced a nonempty transcript but still dropped early syllables: language hints do not fix every recognition error and this is not an accuracy benchmark.
- New phone UI: selected English, restarted app, then transcribed on installed host 0.8.0: clear update-required failure. Upgraded host to 0.9.0; selected Device language, changed app display locale to Korean while system stayed en_US, restarted app and retried. Result DONE with the English sentence above. This exercises settings persistence and system-vs-app locale independence end to end. App locale and recognition default were restored afterward.
- Both launcher variants use the same vector rings; a separate monochrome alpha silhouette preserves crossing gaps. Preview rendered and inspected at 512 px. English/Korean string parity tests passed.

Both emulator and test model processes were stopped before restoring the MLX launch agent; `/v1/models` returned HTTP 200. Raw results and screenshot remain in ignored `build/language-test/`. On-device improvement varies; only Korean and English audio were exercised (Japanese/Mandarin/Cantonese codes are supported and validated but not audio-benchmarked here).
