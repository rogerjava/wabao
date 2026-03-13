package com.rogerv.wabao.service;

import com.rogerv.wabao.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

  public static class Player {
    public final String playerId;
    public final String nickname;
    public boolean ready;
    public int shields;

    public Player(String playerId, String nickname, int shields) {
      this.playerId = playerId;
      this.nickname = nickname;
      this.shields = shields;
    }
  }

  public static class Room {
    public final String roomCode;
    public final RoomMode mode;
    public final Difficulty difficulty;
    public RoomStatus status = RoomStatus.LOBBY;
    public final String ownerId;
    public final Map<String, Player> players = new LinkedHashMap<>();

    // game runtime
    public MinesGame game;

    public Room(String roomCode, RoomMode mode, Difficulty difficulty, String ownerId) {
      this.roomCode = roomCode;
      this.mode = mode;
      this.difficulty = difficulty;
      this.ownerId = ownerId;
    }
  }

  private final Map<String, Room> rooms = new ConcurrentHashMap<>();

  public Room createRoom(RoomMode mode, Difficulty difficulty, String ownerId, String ownerNick) {
    String code = randomRoomCode();
    Room r = new Room(code, mode, difficulty, ownerId);
    GameConfig cfg = GameConfig.of(difficulty);
    r.players.put(ownerId, new Player(ownerId, ownerNick, cfg.shieldsPerPlayer()));
    rooms.put(code, r);
    return r;
  }

  public Room getRoom(String code) {
    Room r = rooms.get(code);
    if (r == null) throw new NoSuchElementException("room not found");
    return r;
  }

  public Room joinRoom(String code, String playerId, String nickname) {
    Room r = getRoom(code);
    synchronized (r) {
      if (r.players.containsKey(playerId)) return r;
      if (r.players.size() >= 8) throw new IllegalStateException("room full");
      GameConfig cfg = GameConfig.of(r.difficulty);
      r.players.put(playerId, new Player(playerId, nickname, cfg.shieldsPerPlayer()));
      return r;
    }
  }

  public Room setReady(String code, String playerId, boolean ready) {
    Room r = getRoom(code);
    synchronized (r) {
      Player p = r.players.get(playerId);
      if (p == null) throw new NoSuchElementException("player not in room");
      p.ready = ready;
      return r;
    }
  }

  public Room startGame(String code, String playerId) {
    Room r = getRoom(code);
    synchronized (r) {
      if (!r.ownerId.equals(playerId)) throw new IllegalStateException("only owner can start");
      if (r.status != RoomStatus.LOBBY) return r;
      boolean allReady = r.players.values().stream().allMatch(p -> p.ready || p.playerId.equals(r.ownerId));
      if (!allReady) throw new IllegalStateException("not all ready");

      GameConfig cfg = GameConfig.of(r.difficulty);
      r.game = MinesGame.create(cfg.rows(), cfg.cols(), cfg.dangers(), cfg.timeLimitSec(), cfg.teamHp());
      r.status = RoomStatus.IN_GAME;
      return r;
    }
  }

  public RoomView toView(Room r) {
    List<PlayerView> ps = r.players.values().stream()
        .map(p -> new PlayerView(p.playerId, p.nickname, p.ready, p.shields))
        .toList();
    return new RoomView(r.roomCode, r.mode, r.difficulty, r.status, r.ownerId, ps);
  }

  private static String randomRoomCode() {
    String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 6; i++) {
      int idx = (int) (Math.random() * alphabet.length());
      sb.append(alphabet.charAt(idx));
    }
    return sb.toString();
  }
}
