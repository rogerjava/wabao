package com.rogerv.wabao.service;

import java.time.Instant;

public class GameRuntime {
  public final MinesGame game;
  public final Instant startedAt;
  public final int timeLimitSec;
  public int teamHp;

  public GameRuntime(MinesGame game, int timeLimitSec, int teamHp) {
    this.game = game;
    this.timeLimitSec = timeLimitSec;
    this.teamHp = teamHp;
    this.startedAt = Instant.now();
  }

  public boolean isTimedOut() {
    long elapsed = Instant.now().getEpochSecond() - startedAt.getEpochSecond();
    return elapsed >= timeLimitSec;
  }
}
