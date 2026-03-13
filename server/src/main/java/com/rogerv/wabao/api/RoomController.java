package com.rogerv.wabao.api;

import com.rogerv.wabao.domain.*;
import com.rogerv.wabao.service.AuthService;
import com.rogerv.wabao.service.RoomService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@Validated
public class RoomController {
  private final RoomService roomService;
  private final AuthService authService;

  public RoomController(RoomService roomService, AuthService authService) {
    this.roomService = roomService;
    this.authService = authService;
  }

  public record CreateRoomRequest(@NotBlank String sessionToken, RoomMode mode, Difficulty difficulty) {}
  public record JoinRoomRequest(@NotBlank String sessionToken) {}

  @PostMapping
  public Map<String, Object> create(@RequestBody CreateRoomRequest req) {
    var s = authService.requireSession(req.sessionToken());
    RoomMode mode = req.mode() == null ? RoomMode.COOP : req.mode();
    Difficulty diff = req.difficulty() == null ? Difficulty.EASY : req.difficulty();
    var room = roomService.createRoom(mode, diff, s.playerId(), s.nickname());
    return Map.of("roomCode", room.roomCode);
  }

  @PostMapping("/{code}/join")
  public Map<String, Object> join(@PathVariable String code, @RequestBody JoinRoomRequest req) {
    var s = authService.requireSession(req.sessionToken());
    roomService.joinRoom(code, s.playerId(), s.nickname());
    return Map.of("ok", true);
  }

  @GetMapping("/{code}")
  public RoomView get(@PathVariable String code) {
    return roomService.toView(roomService.getRoom(code));
  }
}
