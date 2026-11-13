# Roadmap

0.2.0 adds the interwoven-ring identity and independent recognition-language selection (OndolSaem API V5).

0.1.0: scaffold, watch recorder (foreground microphone service, toggle on open, `.part` files, 15-minute cap), Data Layer transfer with acknowledgement, phone intake (Room), transcription through OndolSaem (right away / set time / idle / on request), notification actions (copy, Malbotdari, Jogakbo), list / detail / settings, auto-delete.

Verified on emulators: watch recording, phone intake from a debug broadcast, transcription, notifications and their actions, Jogakbo note with audio + transcript + cues, multi-select delete. On 2026-11-14 the paired phone/watch emulators also passed connected delivery and queued delivery after disconnect/reconnect, including transcription, file integrity and watch-copy deletion (see [test report](emulator-pairing-test.md)).

Still to verify on real devices (Galaxy Watch8 Classic + Galaxy phone):
- the Data Layer transfer and acknowledgement over real Bluetooth, including delivery after a Bluetooth gap (the equivalent ADB-bridge interruption/reconnection passes on emulators);
- assigning a hardware button to open Dorangori, and the toggle behaviour;
- recording with the screen off, and the speech content itself (emulator microphones are silent);
- battery-optimisation behaviour of the watch service on Samsung.

Later: other phone-watch AI features, a Wear tile for one-tap recording, Malbotdari auto-send.
Limits: recordings are capped at 15 minutes (Data Layer assets suit small files); SenseVoice drops the first syllable of some short utterances and may transliterate English into Hangul (language is "auto").
