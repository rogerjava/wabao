-- SQLite schema (minimal)

create table if not exists users (
  id integer primary key autoincrement,
  openid text,
  nickname text,
  avatar text,
  created_at text default (datetime('now')),
  last_login_at text
);

create table if not exists rooms (
  id integer primary key autoincrement,
  code text not null,
  mode text not null,
  status text not null,
  max_players integer not null,
  created_at text default (datetime('now'))
);

create table if not exists games (
  id integer primary key autoincrement,
  room_id integer not null,
  seed text not null,
  rows integer not null,
  cols integer not null,
  danger_count integer not null,
  time_limit_sec integer not null,
  status text not null,
  created_at text default (datetime('now')),
  ended_at text
);
