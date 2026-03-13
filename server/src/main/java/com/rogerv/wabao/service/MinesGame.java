package com.rogerv.wabao.service;

import java.util.*;

/**
 * 扫雷核心：服务端权威棋盘。
 * 规则（MVP）：
 * - 首点不死：第一次 open 必定不是危险
 * - 打开安全格：返回数字/连开
 * - 打开危险：teamHp--，并把该格打开为危险
 */
public class MinesGame {
  public record OpenResult(boolean hitDanger, List<CellUpdate> updates, int teamHp, boolean won, boolean lost) {}
  public record CellUpdate(int r, int c, String kind, int number) {}

  public final int rows;
  public final int cols;
  public final int dangers;
  public final int timeLimitSec;

  private boolean generated = false;
  private final boolean[][] danger;
  private final boolean[][] open;
  private final boolean[][] flag;

  private int teamHp;
  private int safeLeft;

  private MinesGame(int rows, int cols, int dangers, int timeLimitSec, int teamHp) {
    this.rows = rows;
    this.cols = cols;
    this.dangers = dangers;
    this.timeLimitSec = timeLimitSec;
    this.teamHp = teamHp;
    this.danger = new boolean[rows][cols];
    this.open = new boolean[rows][cols];
    this.flag = new boolean[rows][cols];
    this.safeLeft = rows * cols - dangers;
  }

  public static MinesGame create(int rows, int cols, int dangers, int timeLimitSec, int teamHp) {
    return new MinesGame(rows, cols, dangers, timeLimitSec, teamHp);
  }

  public synchronized void toggleFlag(int r, int c, boolean f) {
    if (!in(r,c) || open[r][c]) return;
    flag[r][c] = f;
  }

  public synchronized OpenResult open(int r, int c) {
    if (!in(r,c)) return new OpenResult(false, List.of(), teamHp, false, false);
    if (open[r][c] || flag[r][c]) return new OpenResult(false, List.of(), teamHp, false, false);

    if (!generated) {
      generate(r, c);
      generated = true;
    }

    List<CellUpdate> updates = new ArrayList<>();
    if (danger[r][c]) {
      open[r][c] = true;
      teamHp = Math.max(0, teamHp - 1);
      updates.add(new CellUpdate(r, c, "DANGER", -1));
      boolean lost = teamHp == 0;
      return new OpenResult(true, updates, teamHp, false, lost);
    }

    floodOpen(r, c, updates);
    boolean won = safeLeft == 0;
    return new OpenResult(false, updates, teamHp, won, false);
  }

  private void floodOpen(int r, int c, List<CellUpdate> updates) {
    Deque<int[]> dq = new ArrayDeque<>();
    dq.add(new int[]{r,c});
    while(!dq.isEmpty()) {
      int[] cur = dq.removeFirst();
      int cr = cur[0], cc = cur[1];
      if (!in(cr,cc) || open[cr][cc] || flag[cr][cc]) continue;
      if (danger[cr][cc]) continue;

      open[cr][cc] = true;
      safeLeft--;
      int n = countAround(cr,cc);
      updates.add(new CellUpdate(cr, cc, "SAFE", n));
      if (n == 0) {
        for (int dr=-1; dr<=1; dr++) {
          for (int dc=-1; dc<=1; dc++) {
            if (dr==0 && dc==0) continue;
            dq.add(new int[]{cr+dr, cc+dc});
          }
        }
      }
    }
  }

  private int countAround(int r, int c) {
    int cnt=0;
    for (int dr=-1; dr<=1; dr++) {
      for (int dc=-1; dc<=1; dc++) {
        if (dr==0 && dc==0) continue;
        int nr=r+dr, nc=c+dc;
        if (in(nr,nc) && danger[nr][nc]) cnt++;
      }
    }
    return cnt;
  }

  private void generate(int safeR, int safeC) {
    // place dangers randomly, excluding (safeR,safeC)
    List<int[]> cells = new ArrayList<>();
    for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) {
      if (r==safeR && c==safeC) continue;
      cells.add(new int[]{r,c});
    }
    Collections.shuffle(cells, new Random());
    for (int i=0; i<dangers; i++) {
      int[] p = cells.get(i);
      danger[p[0]][p[1]] = true;
    }
  }

  private boolean in(int r, int c) {
    return r>=0 && r<rows && c>=0 && c<cols;
  }
}
