# Dhun Repair Status

Date: 2026-09-20

## Changes in this source snapshot

- Normalized Android namespace/applicationId to `com.dhun.music`.
- Fixed audio-focus state handling so a manual Dhun pause cannot be undone by AUDIOFOCUS_GAIN from another app.
- Temporary audio-focus loss only resumes if Dhun was actually playing before the interruption.
- Permanent audio-focus loss requires explicit user Play.
- Added ACTION_AUDIO_BECOMING_NOISY auto-pause handling.
- Added Android framework MediaSession transport callbacks and playback metadata.
- Removed V1 Sleep Timer UI and playback logic.
- Removed synthetic/sample-track fallback when no local music is found.
- Removed cosmetic High-Res Audio Engine setting.
- Added artwork-load failure fallback.
- Added BackHandler behavior for Now Playing, Queue, and Playlist Detail overlays.
- Removed stale generated APK copies from the source snapshot.

## Verification limitation

This environment does not have the Android SDK/Gradle toolchain needed to compile and install the Android app.

Required verification on an Android/Gradle-capable machine:

1. Clean build from this exact source.
2. Install the newly generated APK.
3. Verify Favorite does not change the current track.
4. Verify Play/Pause/Next/Previous/Shuffle/Repeat/Seek independently.
5. Manually pause Dhun -> play YouTube -> Dhun remains paused.
6. Repeat with Instagram.
7. Verify temporary focus interruption resumes only when Dhun was playing before interruption.
8. Verify notification/system media controls.
9. Verify empty library state when no local music exists.

Source/build/device verification must be reported separately.
