# Privacy

- The phone app has no INTERNET permission. Recordings and transcripts stay on the phone and watch.
- The watch holds a recording only until the phone confirms receipt.
- Permissions: watch `RECORD_AUDIO`, foreground service (microphone), notifications; phone notifications.
- The phone app asks Android to show notifications and runs a data-sync foreground job while transcribing; it reads no other apps' data. "Save to Jogakbo" sends the audio file and the text to Jogakbo on the same phone through its SDK; "Ask Malbotdari" sends only the text.
- Transcription runs in OndolSaem on the phone. Text leaves Dorangori only when the user taps an action (clipboard, Malbotdari, Jogakbo).
- Watch recordings live in the app's private storage (`filesDir/recordings`); an unfinished recording is a `.part` file and is deleted at the next start.

- The recognition-language preference is stored locally. Its resolved language code is sent only to OndolSaem on the same phone with the transcription request. Device-language resolution adds no permission.
