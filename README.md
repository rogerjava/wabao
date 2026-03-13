# 挖宝（Wabao）

微信小游戏《挖宝》：扫雷玩法的“山洞宝箱”版本，支持多人联机（合作/对抗），后端提供房间/对局/实时同步服务。

## 目录结构
- `game/`：微信小游戏前端（原生小游戏框架 + Canvas）
- `server/`：后端（Spring Boot + WebSocket + SQLite；预留 Redis）
- `docs/`：协议、接口、设计说明

## 快速开始

### 后端
```bash
cd server
./mvnw spring-boot:run
```
默认端口：`18080`

### 前端
使用微信开发者工具导入 `game/` 目录运行。

## Roadmap
- MVP：合作模式联机挖宝（共享棋盘 + 实时同步 + 结算）
- V1.1：对抗模式 + 放置陷阱
- V2：道具系统/怪物类型扩展/排行榜
