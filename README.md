# Dhun

Premium Indian-modern local music player for Android.

## Current repair snapshot

- Package/namespace: `com.dhun.music`
- Local/offline-first playback
- Audio-focus state handling repaired so an intentional user pause is not undone by another app regaining focus
- Android framework MediaSession transport callbacks
- No synthetic/sample tracks when the local library is empty
- V1 Sleep Timer and cosmetic High-Res Audio setting removed
- Artwork failure fallback and overlay BackHandler behavior added

## Verification status

Source repair snapshot is uploaded, but build/device verification is still required. See `DHUN_REPAIR_STATUS.md`.

Do not treat an old APK as evidence for this source.
