package com.rogerv.wabao.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
  public record Session(String playerId, String nickname, Instant createdAt) {}

  private final Map<String, Session> tokenToSession = new ConcurrentHashMap<>();

  public String createGuestSession(String nickname) {
    String playerId = "p_" + TokenService.randomToken(6);
    String token = "t_" + TokenService.randomToken(18);
    tokenToSession.put(token, new Session(playerId, nickname, Instant.now()));
    return token;
  }

  public Session requireSession(String token) {
    Session s = tokenToSession.get(token);
    if (s == null) throw new IllegalArgumentException("invalid token");
    return s;
  }
}
