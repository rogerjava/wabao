package com.rogerv.wabao.api;

import com.rogerv.wabao.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  public record GuestLoginRequest(@NotBlank String nickname) {}

  @PostMapping("/guest")
  public Map<String, Object> guest(@RequestBody GuestLoginRequest req) {
    String token = authService.createGuestSession(req.nickname());
    AuthService.Session s = authService.requireSession(token);
    return Map.of(
        "playerId", s.playerId(),
        "nickname", s.nickname(),
        "sessionToken", token
    );
  }
}
