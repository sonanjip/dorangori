# Privacy

- The phone app has no INTERNET permission. Recordings and transcripts stay on the phone and watch.
- The watch holds a recording only until the phone confirms receipt.
- Permissions: watch `RECORD_AUDIO`, foreground service (microphone), notifications; phone notifications.
- Transcription runs in OndolSaem on the phone. Text leaves Dorangori only when the user taps an action (clipboard, Malbotdari, Jogakbo).
- Watch recordings live in the app's private storage (`filesDir/recordings`); an unfinished recording is a `.part` file and is deleted at the next start.
