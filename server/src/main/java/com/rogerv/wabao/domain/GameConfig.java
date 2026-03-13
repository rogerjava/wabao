package com.rogerv.wabao.domain;

public record GameConfig(int rows, int cols, int dangers, int timeLimitSec, int teamHp, int shieldsPerPlayer) {
  public static GameConfig of(Difficulty d) {
    return switch (d) {
      case EASY -> new GameConfig(10, 10, 12, 180, 3, 1);
      case MEDIUM -> new GameConfig(12, 12, 24, 180, 3, 1);
      case HARD -> new GameConfig(16, 16, 40, 180, 3, 0);
    };
  }
}
