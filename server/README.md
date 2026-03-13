# wabao-server

Spring Boot 3 + WebSocket，数据落 SQLite（Flyway 迁移），房间/棋盘状态先用内存实现（单机可用），后续可切 Redis。

## 运行
```bash
./mvnw spring-boot:run
```

环境变量（可选）：
- `SERVER_PORT` (default 18080)
- `WABAO_DB_PATH` (default ./data/wabao.db)
