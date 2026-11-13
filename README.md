# Dorangori

**English** | [한국어](README.ko.md)

Record a voice note on your Wear OS watch; your phone transcribes it with [OndolSaem](https://github.com/sonanjip/ondolsaem) on-device speech recognition and hands you the text: copy it, ask Malbotdari, or save it to Jogakbo together with the audio.

Status: 0.1.0. The watch recorder, the transfer code and the whole phone side work on emulators; the watch-to-phone transfer and the hardware button still need a real watch ([docs/roadmap.md](docs/roadmap.md)).

| Feature | Status |
|---|---|
| Record on the watch (screen button or an assigned hardware button) | done (watch emulator) |
| Send to the phone over Bluetooth, retry when reconnected | written, needs a paired watch to verify |
| On-device transcription (right away, at a set time, when idle, or on request) | done |
| Notification actions: copy, ask Malbotdari, save to Jogakbo (audio + text as one note) | done |
| Recording list, detail, multi-select delete, auto-delete | done |

Build: see [docs/development.md](docs/development.md). License: Apache-2.0.
