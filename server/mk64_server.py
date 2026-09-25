#!/usr/bin/env python3
"""
Minimal MK64Recomp UDP lobby/relay server.

This does not contain game assets. It provides room membership, heartbeat,
latency responses, and packet relay plumbing for the Android port.
"""

import argparse
import asyncio
import json
import secrets
import time
from dataclasses import dataclass, field
from typing import Dict, Tuple

Address = Tuple[str, int]


@dataclass
class Client:
    addr: Address
    name: str
    last_seen: float = field(default_factory=time.monotonic)


@dataclass
class Room:
    code: str
    clients: Dict[Address, Client] = field(default_factory=dict)


class MK64Server(asyncio.DatagramProtocol):
    def __init__(self):
        self.transport = None
        self.rooms: Dict[str, Room] = {}
        self.addr_to_room: Dict[Address, str] = {}

    def connection_made(self, transport):
        self.transport = transport
        print("MK64 multiplayer UDP server ready")

    def datagram_received(self, data: bytes, addr: Address):
        try:
            msg = json.loads(data.decode("utf-8"))
            kind = msg.get("type")
            if kind == "join":
                self.handle_join(msg, addr)
            elif kind == "ping":
                self.handle_ping(msg, addr)
            elif kind == "relay":
                self.handle_relay(msg, addr)
            elif kind == "leave":
                self.remove_client(addr)
            else:
                self.send(addr, {"type": "error", "message": "unknown packet type"})
        except Exception as exc:
            self.send(addr, {"type": "error", "message": str(exc)})

    def handle_join(self, msg, addr: Address):
        room_code = str(msg.get("room", "")).strip().upper()
        if not room_code:
            room_code = self.new_room_code()

        old = self.addr_to_room.get(addr)
        if old and old != room_code:
            self.remove_client(addr)

        room = self.rooms.setdefault(room_code, Room(room_code))
        if len(room.clients) >= 4 and addr not in room.clients:
            self.send(addr, {"type": "error", "message": "room full"})
            return

        client = Client(addr=addr, name=str(msg.get("name", "Player"))[:24])
        room.clients[addr] = client
        self.addr_to_room[addr] = room_code

        self.send(addr, {
            "type": "joined",
            "room": room_code,
            "players": len(room.clients),
        })
        self.broadcast(room, {
            "type": "peer_joined",
            "players": len(room.clients),
        }, exclude=addr)
        print(f"{addr} joined {room_code} ({len(room.clients)}/4)")

    def handle_ping(self, msg, addr: Address):
        self.touch(addr)
        self.send(addr, {
            "type": "pong",
            "clientTime": msg.get("clientTime", 0),
            "serverTime": int(time.time() * 1000),
        })

    def handle_relay(self, msg, addr: Address):
        room = self.room_for(addr)
        if room is None:
            self.send(addr, {"type": "error", "message": "not in a room"})
            return
        self.touch(addr)
        payload = {
            "type": "relay",
            "channel": msg.get("channel", "game"),
            "seq": msg.get("seq", 0),
            "payload": msg.get("payload"),
        }
        self.broadcast(room, payload, exclude=addr)

    def room_for(self, addr: Address):
        code = self.addr_to_room.get(addr)
        return self.rooms.get(code) if code else None

    def touch(self, addr: Address):
        room = self.room_for(addr)
        if room and addr in room.clients:
            room.clients[addr].last_seen = time.monotonic()

    def remove_client(self, addr: Address):
        code = self.addr_to_room.pop(addr, None)
        if not code:
            return
        room = self.rooms.get(code)
        if not room:
            return
        room.clients.pop(addr, None)
        if room.clients:
            self.broadcast(room, {
                "type": "peer_left",
                "players": len(room.clients),
            })
        else:
            self.rooms.pop(code, None)

    def broadcast(self, room: Room, msg: dict, exclude: Address | None = None):
        for addr in tuple(room.clients):
            if addr != exclude:
                self.send(addr, msg)

    def send(self, addr: Address, msg: dict):
        if self.transport:
            self.transport.sendto(
                json.dumps(msg, separators=(",", ":")).encode("utf-8"),
                addr,
            )

    @staticmethod
    def new_room_code():
        alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return "".join(secrets.choice(alphabet) for _ in range(6))


async def cleanup_loop(server: MK64Server):
    while True:
        await asyncio.sleep(5)
        now = time.monotonic()
        stale = []
        for room in tuple(server.rooms.values()):
            for addr, client in tuple(room.clients.items()):
                if now - client.last_seen > 15:
                    stale.append(addr)
        for addr in stale:
            server.remove_client(addr)


async def main(host: str, port: int):
    loop = asyncio.get_running_loop()
    transport, protocol = await loop.create_datagram_endpoint(
        MK64Server,
        local_addr=(host, port),
    )
    asyncio.create_task(cleanup_loop(protocol))
    try:
        await asyncio.Future()
    finally:
        transport.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=6464)
    args = parser.parse_args()
    asyncio.run(main(args.host, args.port))
