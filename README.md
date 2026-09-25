# MK64Recomp Android

Android-focused Mario Kart 64 recompilation project with mobile controls, diagnostics, and online multiplayer infrastructure.

## Current Android features

- ARM64 Android app, package `com.eightcee.mk64recomp`
- User-supplied Mario Kart 64 USA ROM import and SHA-1 validation
- Native JNI/C++ host with Android `ANativeWindow` surface lifecycle
- DK64-style dark-glass touch controls
- N64 A/B/C/Start colors, Z/L/R controls, analog stick, haptics, HUD toggle
- Bluetooth/USB controller input mapping
- Settings for touch controls, VSync and resolution scale
- Save directory and backup foundation
- Java/Kotlin and native crash diagnostics
- On-device diagnostic viewer, copy/export/clear
- Dedicated UDP multiplayer client/server infrastructure
- 4-player rooms, ping/heartbeat, relay channel, stale-peer cleanup
- Docker-packaged multiplayer server
- Multiplayer protocol tests in CI
- GitHub Actions debug APK builds

## Game-core integration

The upstream recompilation source is pinned under `upstream/MK64Recomp`.

The actual `RecompiledFuncs` and RSP sources are generated from the user's own Mario Kart 64 NTSC-U ROM. The ROM itself is never committed.

Expected ROM SHA-1:

`579c48e211ae952530ffc8738709f078d5dd215e`

Windows and Unix generation scripts are provided in `tools/`. See `docs/CORE_INTEGRATION.md`.

The remaining playable-game milestone is linking those generated game sources and the Android RT64 renderer adapter into `libmk64android.so`.

## Multiplayer server

Run directly:

```bash
python3 server/mk64_server.py --host 0.0.0.0 --port 6464
```

Or build the Docker image from `server/Dockerfile`.

## Legal

This repository does not include Nintendo ROMs or proprietary game assets. Users must provide their own legally obtained ROM.
