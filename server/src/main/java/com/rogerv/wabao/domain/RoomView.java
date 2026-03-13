package com.rogerv.wabao.domain;

import java.util.List;

public record RoomView(String roomCode, RoomMode mode, Difficulty difficulty, RoomStatus status,
                       String ownerId, List<PlayerView> players) {}
