# IOT 链接设备数据统计系统

从远程 IoT 数据库定时拉取设备数据，按 Product 分类统计 SN 数量，通过 WebSocket 实时推送到前端仪表盘展示。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Java 11 · Spring Boot 2.7 · Spring Data JPA · WebSocket (STOMP/SockJS) |
| 前端 | React 19 · TypeScript · Vite · ECharts |
| 数据库 | PostgreSQL 14（远程只读 + 本地缓存） |
| 部署 | Docker Compose · Nginx 反向代理 |

## 架构概览

```
远程 PostgreSQL (IoT 原始数据)
        │ 定时拉取 (每分钟)
        ▼
  Spring Boot 后端
  ┌───────────────────────────┐
  │ 1. 查询远程库 JSON 字段聚合 │
  │ 2. 写入本地库缓存           │
  │ 3. WebSocket 推送前端      │
  └──────────┬────────────────┘
             │
     ┌───────┴───────┐
     ▼               ▼
 本地 PostgreSQL   React 前端
 (统计快照缓存)    (仪表盘 + 趋势图)
```

核心设计：**读写分离 + 本地缓存**。前端请求直接读本地库，不阻塞等待远程查询；远程数据通过定时任务异步拉取。

## 项目结构

```
find_database/
├── backend/                        # Spring Boot 后端
│   └── src/main/java/.../backend/
│       ├── config/                 # 双数据源 · WebSocket · CORS · 定时任务
│       ├── entity/remote/          # 远程表映射 (ts_device_data_format)
│       ├── entity/local/           # 本地表映射 (product_stats_record)
│       ├── repository/remote/      # 远程库查询 (原生 SQL + JSON 解析)
│       ├── repository/local/       # 本地库 CRUD
│       ├── dto/                    # 响应数据结构
│       ├── service/                # 核心业务逻辑
│       └── controller/             # REST API
├── frontend/                       # React 前端
│   └── src/
│       ├── App.tsx                 # 根组件 (WebSocket + Tab 切换)
│       └── components/
│           ├── RecentHourStats.tsx  # 最近一小时统计表
│           └── ProductTrendChart.tsx # ECharts 趋势图
├── docker-compose.yml              # 三服务编排
└── README.md
```

## API 接口

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/stats/category` | 获取最新 Product 分类 SN 统计 |
| GET | `/api/stats/recent-hour` | 最近一小时统计（含时间范围） |
| POST | `/api/stats/refresh` | 手动强制从远程库刷新 |
| GET | `/api/stats/trend?product=xxx&hours=168` | 获取某 Product 历史趋势 |
| GET | `/api/stats/health` | 健康检查 |

**WebSocket**: 连接 `/ws/stats` (SockJS)，订阅 `/topic/stats`，每分钟推送最新统计。

## 快速开始

### 本地开发

```bash
# 1. 启动本地 PostgreSQL
docker run -d --name pg-local -p 5432:5432 \
  -e POSTGRES_DB=backend_stats \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=root \
  postgres:14-alpine

# 2. 启动后端 (端口 8080)
cd backend && ./mvnw spring-boot:run

# 3. 启动前端 (端口 5173，自动代理到后端)
cd frontend && npm install && npm run dev
```

### Docker Compose 部署

```bash
docker compose up --build
```

| 服务 | 端口 | 说明 |
|------|------|------|
| frontend | `88` | Nginx 静态文件 + 反向代理 |
| backend | `8088` | Spring Boot |
| postgres | `5432` (内部) | 数据持久化在 Docker volume |

## 定时任务

| 频率 | 功能 |
|------|------|
| 每分钟 | 从远程库拉取最近 1h 数据 → 按 Product 聚合去重 SN → 存本地 → WebSocket 推送 |
| 每天凌晨 3 点 | 清理超过 7 天的本地历史记录 |

## 前端功能

- **Tab 1** — 最近一小时 Product SN 统计表（按数量降序，支持展开/收起）
- **Tab 2** — 完整 Product 统计表 + 搜索过滤 + 点击行查看历史趋势图（1h/6h/24h/7d）
- Header 实时显示 WebSocket 连接状态、总 SN 数量、更新时间
