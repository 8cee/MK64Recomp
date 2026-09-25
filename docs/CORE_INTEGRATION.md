# MK64 Android core integration

The Android shell does not ship a Mario Kart 64 ROM or Nintendo assets.

The upstream recompilation source is pinned as the `upstream/MK64Recomp` submodule. Its primary `RecompiledFuncs` sources and RSP output are generated from the user's own verified NTSC-U ROM.

Expected ROM SHA-1:

`579c48e211ae952530ffc8738709f078d5dd215e`

## Generate the game source

Build N64Recomp and RSPRecomp, then run:

```bash
N64RECOMP=/path/to/N64Recomp \
RSPRECOMP=/path/to/RSPRecomp \
./tools/generate_mk64_core.sh /path/to/mk64.us.z64
```

The script validates the ROM before generation and deletes the temporary ROM copy from the upstream working tree when it exits.

Generated source is not a substitute for the ROM and must be handled according to applicable copyright law and the upstream project's licensing requirements.

## Android renderer boundary

The Android app already owns an `ANativeWindow` from its `SurfaceView`. The remaining renderer integration replaces the desktop-created SDL window path in RT64 with this externally supplied Android window/surface and then links the generated game/runtime libraries into `libmk64android.so`.

The Android-facing API remains:

- initialize game core
- attach/detach native surface
- resize surface
- controller/touch state
- save path
- diagnostics
- shutdown
