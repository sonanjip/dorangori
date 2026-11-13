# User guide

## Record on the watch
Open Dorangori on the watch: it starts recording (switch off "Record when opened" to start with the button instead). Open it again, or press the Stop button, to stop and save. Assign a hardware button to "open Dorangori" in the watch settings to start and stop with it; Dorangori never takes over the Home or Back buttons. A recording stops by itself after 15 minutes.

The recording goes to the phone over Bluetooth, also later if the phone was away, and the watch deletes its copy once the phone has it.

## On the phone
- **List**: recordings, newest first, with their text. Long-press to select several and delete them.
- **A recording**: play it, read and copy the text, ask Malbotdari (opens a new chat with the text in the input box), save it to Jogakbo (one note: the audio, the text and the timed lines), transcribe again, delete.
- **Notification**: when the text is ready it shows the text with Copy, Ask Malbotdari and Save to Jogakbo; tapping it opens the recording.
- **Settings**: when to transcribe (right away, at a set time, when the phone is idle, only when you ask; with the last three a "New voice recording" notification has a Transcribe now button), auto-delete after 7 / 30 / 90 days (audio, text and list entry together), and the status of notifications, the watch and OndolSaem.

Transcription runs in OndolSaem: install it, allow Dorangori there (not needed when both are signed with the same key) and download the speech model (SenseVoice) in its Models tab.

## Recognition language (0.2.0)

Settings → Speech recognition language offers Automatic detection (default), Device language, Korean, English, Japanese, Chinese (Mandarin), Cantonese. This is independent of the English/Korean display language. Device language uses the phone system language even when this app is displayed in another language; unsupported languages use automatic detection. Cantonese/yue and Chinese Hong Kong/Macao locales resolve to Cantonese; other Chinese locales resolve to Mandarin.

Changes apply when transcription starts. Existing text is retained; open a recording and use Transcribe again to apply a new language. Explicit language hints require OndolSaem 0.9.0 or later. Automatic detection continues working with older hosts; explicit languages report an update-required message rather than being ignored. Language hints can help language identification but do not guarantee higher accuracy, especially for mixed-language speech.

Phone and watch now share interwoven teal/apricot rings as the launcher icon, with a separate monochrome silhouette for themed launchers. Microphone icons still identify actual recording notifications.
