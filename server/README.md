# MK64Recomp Multiplayer Server

This directory contains the first dedicated-server transport for the Android port.

It currently provides:

- UDP room creation/join
- Up to 4 clients per room
- Heartbeats and ping responses
- Peer join/leave notifications
- Generic game-packet relay channel
- Stale-client cleanup

Run it with:

```bash
python3 server/mk64_server.py --host 0.0.0.0 --port 6464
```

The current server is transport infrastructure only. Actual race-state synchronization is wired after the MK64 runtime is integrated on Android.
