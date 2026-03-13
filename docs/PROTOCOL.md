# 实时协议（Draft）

WebSocket JSON 消息格式：

```json
{ "type": "OPEN_CELL", "payload": {"r": 1, "c": 2} }
```

## Client -> Server
- `JOIN_ROOM` {roomCode}
- `READY` {ready}
- `START_GAME` {}
- `OPEN_CELL` {r,c}
- `FLAG_CELL` {r,c,flag}
- `PING` {ts}

## Server -> Client
- `ROOM_STATE` {...}
- `GAME_INIT` {...}
- `CELL_UPDATE` {...}
- `GAME_SNAPSHOT` {...}
- `GAME_OVER` {...}
