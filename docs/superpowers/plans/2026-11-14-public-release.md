# Public release and demo plan

User requested publication of Dorangori, an emulator GIF, a new OndolSaem release, an audit of sibling releases, and bilingual Sonanjip profile updates.

- [ ] Audit remote tags/assets and versioned local changes; publish missing companion releases needed by Dorangori (Jogakbo audio+transcript API).
- [ ] Build signed release APKs one repo at a time with low-memory flags and all emulators stopped. Use shared release certificate for phone/watch and hosts; verify signer, package, versions and checksums.
- [ ] Capture both watch and phone emulator screens in one GIF; use deterministic prerecorded speech for emulator audio and disclose it. Show normal recording controls and queued delivery/reconnect into phone transcription/detail/actions. No personal account screens or real-device changes.
- [ ] Update English/Korean Dorangori README, name meaning (도란도란 + 고리), install dependencies and release links; document demo reproduction.
- [ ] Create public sonanjip/dorangori repo, push main and app tag, publish release with phone/watch signed APKs. Push host/notes SDK tags and publish corresponding missing app releases.
- [ ] Update bilingual Sonanjip org profile with fifth app, diagram, dependencies, name and GIF. Verify remote releases and raw GIF links. Restore MLX after emulator shutdown.

Constraints: existing user sign-in is preserved; no force pushes, no rewriting released tags; no builds alongside emulators, no parallel Gradle. Keep downloads and generated clips under ignored build folders. Publish only source, docs and final optimized GIF, no keys/models.

Audit: Nunsori v0.12.1 peels to current main (7df49c6), and Malbotdari v0.4.2 is current with an uploaded APK. Jogakbo main is one SDK/API-2 commit behind local 0.7.0 and needs publication. No extra Nunsori release is needed.
