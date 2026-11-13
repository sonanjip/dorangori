# Implementation plan

Spec: ../specs/2026-11-14-icon-asr-language-design.md (user-approved direction).

- [x] Add phone AsrLanguage enum + system-locale resolver and tests; persist it in SettingsStore, show localized selection dialog and apply it at transcription start. Preserve existing text. Display clear old-host failure.
- [x] Append IAsrService.transcribeWithLanguage at transaction 4, bump host API V5/SDK 0.7.0, share SDK callback/cancellation handling. Validate language. Add host-engine explicit language implementation with one cached recognizer and legacy-auto delegation. Add binder tests for requests en, ko, auto and invalid input.
- [x] Replace both launcher vectors with interwoven teal/apricot rings and monochrome silhouette, render a preview. Keep microphone notification icons for actual recording.
- [x] Bump Dorangori to 0.2.0 and host to 0.9.0; update READMEs, architecture, user/client guides and privacy as applicable.
- [x] Run unit tests and build host, Dorangori, Nunsori in sequence using low-memory flags; one-emulator binder tests; optional real SenseVoice language comparison, document limits and restore MLX.

Review focus: system locale must not accidentally mean app locale; old hosts must not ignore explicit language; language choices must not leak between clients; invalid language and fromMs must not bypass caller checks; monochrome icon must preserve both holes and intertwined shape under circular mask.

Execution: inline in existing clean repositories, retaining existing composite builds and signing. No parallel Gradle builds or builds alongside emulators.

Verification: host app 89 + SDK 15 JVM tests; Dorangori app 14 + watch 6 JVM tests; Nunsori 170 JVM tests and APK compatibility build. HostServiceTest 25 on-device fake-engine tests, including raw Binder rejection and language/auto isolation, passed. Real SenseVoice smoke passed (auto → en → ko → auto, deterministic auto result). UI en selection survived process restart, old host 0.8.0 showed update-required, upgraded 0.9.0 plus SYSTEM with Korean UI and English system produced English text. Emulator stopped and MLX restored. No model or dependency download was added.
