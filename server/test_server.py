import json
import unittest

from mk64_server import MK64Server


class FakeTransport:
    def __init__(self):
        self.sent = []

    def sendto(self, payload, addr):
        self.sent.append((json.loads(payload.decode("utf-8")), addr))


class ServerTests(unittest.TestCase):
    def setUp(self):
        self.server = MK64Server()
        self.transport = FakeTransport()
        self.server.connection_made(self.transport)

    def send(self, addr, payload):
        self.server.datagram_received(
            json.dumps(payload).encode("utf-8"),
            addr,
        )

    def test_blank_room_creates_code(self):
        addr = ("127.0.0.1", 10001)
        self.send(addr, {"type": "join", "room": "", "name": "P1"})
        msg, dest = self.transport.sent[-1]
        self.assertEqual(dest, addr)
        self.assertEqual(msg["type"], "joined")
        self.assertEqual(len(msg["room"]), 6)
        self.assertEqual(msg["players"], 1)

    def test_room_limit_is_four(self):
        code = "ABC123"
        for i in range(4):
            self.send(("127.0.0.1", 11000 + i), {
                "type": "join",
                "room": code,
                "name": f"P{i + 1}",
            })
        fifth = ("127.0.0.1", 12000)
        self.send(fifth, {
            "type": "join",
            "room": code,
            "name": "P5",
        })
        msg, dest = self.transport.sent[-1]
        self.assertEqual(dest, fifth)
        self.assertEqual(msg["type"], "error")
        self.assertEqual(msg["message"], "room full")

    def test_ping_echoes_client_time(self):
        addr = ("127.0.0.1", 13001)
        self.send(addr, {"type": "join", "room": "PING01", "name": "P1"})
        self.send(addr, {
            "type": "ping",
            "room": "PING01",
            "clientTime": 123456,
        })
        msg, _ = self.transport.sent[-1]
        self.assertEqual(msg["type"], "pong")
        self.assertEqual(msg["clientTime"], 123456)

    def test_relay_goes_to_other_peer(self):
        a = ("127.0.0.1", 14001)
        b = ("127.0.0.1", 14002)
        self.send(a, {"type": "join", "room": "RELAY1", "name": "A"})
        self.send(b, {"type": "join", "room": "RELAY1", "name": "B"})
        self.transport.sent.clear()

        self.send(a, {
            "type": "relay",
            "channel": "game",
            "seq": 7,
            "payload": {"frame": 99},
        })

        self.assertEqual(len(self.transport.sent), 1)
        msg, dest = self.transport.sent[0]
        self.assertEqual(dest, b)
        self.assertEqual(msg["type"], "relay")
        self.assertEqual(msg["seq"], 7)
        self.assertEqual(msg["payload"]["frame"], 99)

    def test_leave_removes_empty_room(self):
        addr = ("127.0.0.1", 15001)
        self.send(addr, {"type": "join", "room": "LEAVE1", "name": "P1"})
        self.send(addr, {"type": "leave"})
        self.assertNotIn("LEAVE1", self.server.rooms)
        self.assertNotIn(addr, self.server.addr_to_room)


if __name__ == "__main__":
    unittest.main()
