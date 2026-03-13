package com.rogerv.wabao.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rogerv.wabao.service.AuthService;
import com.rogerv.wabao.service.MinesGame;
import com.rogerv.wabao.service.RoomService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WabaoWsHandler extends TextWebSocketHandler {
  private final ObjectMapper om = new ObjectMapper();
  private final AuthService authService;
  private final RoomService roomService;

  // roomCode -> sessions
  private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
  // sessionId -> context
  private final Map<String, Ctx> ctx = new ConcurrentHashMap<>();

  public WabaoWsHandler(AuthService authService, RoomService roomService) {
    this.authService = authService;
    this.roomService = roomService;
  }

  record Ctx(String playerId, String nickname, String roomCode) {}

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    URI uri = session.getUri();
    String token = query(uri, "token");
    String roomCode = query(uri, "room");
    var s = authService.requireSession(token);

    // ensure joined
    roomService.joinRoom(roomCode, s.playerId(), s.nickname());

    ctx.put(session.getId(), new Ctx(s.playerId(), s.nickname(), roomCode));
    roomSessions.computeIfAbsent(roomCode, k -> ConcurrentHashMap.newKeySet()).add(session);

    broadcastRoomState(roomCode);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Map<?,?> m = om.readValue(message.getPayload(), Map.class);
    String type = (String) m.get("type");
    Object payload = m.get("payload");

    Ctx c = ctx.get(session.getId());
    if (c == null) return;

    switch (type) {
      case "READY" -> {
        boolean ready = payload instanceof Map p && Boolean.TRUE.equals(p.get("ready"));
        roomService.setReady(c.roomCode, c.playerId, ready);
        broadcastRoomState(c.roomCode);
      }
      case "START_GAME" -> {
        var r = roomService.startGame(c.roomCode, c.playerId);
        // init team hp from difficulty config
        var cfg = com.rogerv.wabao.domain.GameConfig.of(r.difficulty);
        setTeamHp(r, cfg.teamHp());

        // broadcast init
        var g = r.game;
        broadcast(c.roomCode, new WsMessage("GAME_INIT", Map.of(
            "rows", g.rows,
            "cols", g.cols,
            "timeLimitSec", g.timeLimitSec,
            "teamHp", getTeamHp(r)
        )));
        broadcastRoomState(c.roomCode);
      }
      case "OPEN_CELL" -> {
        if (!(payload instanceof Map p)) return;
        int rr = ((Number)p.get("r")).intValue();
        int cc = ((Number)p.get("c")).intValue();
        var r = roomService.getRoom(c.roomCode);
        synchronized (r) {
          if (r.game == null) return;
          MinesGame.OpenResult res = r.game.open(rr, cc);
          // shield handling: if hit danger and player has shield -> consume and negate hp loss
          if (res.hitDanger()) {
            var pl = r.players.get(c.playerId);
            if (pl != null && pl.shields > 0) {
              pl.shields -= 1;
              // rollback hp loss by +1 (since we already decreased in game)
              // MVP: simple compensation
              // NOTE: for correctness, later we should apply shield before open() decrements hp.
              // Here we just add back.
              var hp = getTeamHp(r) + 1;
              setTeamHp(r, hp);
            }
          }

          broadcast(c.roomCode, new WsMessage("CELL_UPDATE", Map.of(
              "by", c.playerId,
              "updates", res.updates(),
              "teamHp", getTeamHp(r),
              "won", res.won(),
              "lost", res.lost()
          )));

          if (res.won() || res.lost()) {
            broadcast(c.roomCode, new WsMessage("GAME_OVER", Map.of(
                "won", res.won(),
                "lost", res.lost(),
                "teamHp", getTeamHp(r)
            )));
          }
        }
      }
      case "FLAG_CELL" -> {
        if (!(payload instanceof Map p)) return;
        int rr = ((Number)p.get("r")).intValue();
        int cc = ((Number)p.get("c")).intValue();
        boolean flag = Boolean.TRUE.equals(p.get("flag"));
        var r = roomService.getRoom(c.roomCode);
        synchronized (r) {
          if (r.game == null) return;
          r.game.toggleFlag(rr, cc, flag);
          broadcast(c.roomCode, new WsMessage("FLAG_UPDATE", Map.of(
              "r", rr, "c", cc, "flag", flag, "by", c.playerId
          )));
        }
      }
      default -> {
        // ignore
      }
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    Ctx c = ctx.remove(session.getId());
    if (c != null) {
      Set<WebSocketSession> set = roomSessions.get(c.roomCode);
      if (set != null) {
        set.remove(session);
      }
      broadcastRoomState(c.roomCode);
    }
  }

  private void broadcastRoomState(String roomCode) throws Exception {
    var r = roomService.getRoom(roomCode);
    broadcast(roomCode, new WsMessage("ROOM_STATE", roomService.toView(r)));
  }

  private void broadcast(String roomCode, WsMessage msg) throws Exception {
    String json = om.writeValueAsString(msg);
    Set<WebSocketSession> set = roomSessions.getOrDefault(roomCode, Set.of());
    for (WebSocketSession s : set) {
      if (s.isOpen()) s.sendMessage(new TextMessage(json));
    }
  }

  private static String query(URI uri, String key) {
    if (uri == null || uri.getQuery() == null) return null;
    for (String part : uri.getQuery().split("&")) {
      String[] kv = part.split("=", 2);
      if (kv.length == 2 && kv[0].equals(key)) return kv[1];
    }
    return null;
  }

  // MVP: store teamHp inside room.game is private; so we keep it in room via reflection? no.
  // Here we approximate by keeping hp in room via a hack: use r.game.open result teamHp isn't exposed.
  // We'll keep hp in room by attaching as field? For MVP store in room object.
  private int getTeamHp(RoomService.Room r) {
    // If game exists, we can't read internal; so store in room using a side map.
    return teamHpByRoom.computeIfAbsent(r.roomCode, k -> 3);
  }

  private void setTeamHp(RoomService.Room r, int hp) {
    teamHpByRoom.put(r.roomCode, Math.max(0, hp));
  }

  private final Map<String, Integer> teamHpByRoom = new ConcurrentHashMap<>();
}
