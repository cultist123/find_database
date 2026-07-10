# 展品数据统计后端服务

## 项目概述

这是一个用于统计展品数据的后端服务，能够按展品分类统计每小时不同的SN数量，并通过WebSocket实时推送数据到前端页面。

## 功能特性

- **按展品分类统计**：支持多维度统计（古代文物、艺术品、科技展品、历史文献、民俗文化、自然历史等）
- **每小时数据刷新**：定时任务每小时自动从数据库抓取并统计数据
- **实时推送**：通过WebSocket将统计数据实时推送到前端
- **可视化展示**：前端使用ECharts图表展示统计数据

## 技术栈

### 后端
- **Spring Boot 3.2.0**: 核心框架
- **Spring Data JPA**: 数据持久化
- **Spring WebSocket**: 实时通信
- **MySQL**: 数据库存储
- **Lombok**: 代码简化
- **Maven**: 构建工具

### 前端
- **React 19**: UI框架
- **TypeScript**: 类型安全
- **ECharts**: 图表展示
- **STOMP.js + SockJS**: WebSocket客户端
- **Vite**: 构建工具

## 项目结构

```
find_database/
├── backend/
│   ├── src/main/java/com/example/backend/
│   │   ├── entity/          # 实体类
│   │   ├── dto/             # 数据传输对象
│   │   ├── repository/      # 数据访问层
│   │   ├── service/         # 业务逻辑层
│   │   ├── controller/      # 控制层
│   │   └── config/          # 配置类
│   └── src/main/resources/
│       ├── application.properties  # 应用配置
│       └── data.sql                # 初始化数据
│
└── frontend/
    └── src/
        ├── App.tsx          # 主组件
        ├── App.css          # 样式
        └── ...
```

## API接口

### RESTful API

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/stats/category` | GET | 获取按展品分类的每小时SN统计 |
| `/api/stats/refresh` | POST | 手动刷新统计数据 |
| `/api/stats/total` | GET | 获取总数据量 |
| `/api/stats/health` | GET | 健康检查 |

### WebSocket 端点

| 端点 | 描述 |
|------|------|
| `/ws/stats` | WebSocket连接端点 |
| `/topic/stats` | 统计数据订阅主题 |

## 快速开始

### 1. 数据库配置

1. 安装MySQL并创建数据库：
```sql
CREATE DATABASE exhibition_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：
```bash
mysql -u root -p exhibition_db < backend/src/main/resources/data.sql
```

3. 修改 `backend/src/main/resources/application.properties` 中的数据库配置：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/exhibition_db
spring.datasource.username=root
spring.datasource.password=你的密码
```

### 2. 启动后端服务

```bash
cd backend
./mvnw spring-boot:run
# 或
mvn spring-boot:run
```

后端服务将启动在 `http://localhost:8080`

### 3. 启动前端

```bash
cd ../frontend
npm install
npm run dev
```

前端服务将启动在 `http://localhost:5173`

### 4. 访问应用

打开浏览器访问 `http://localhost:5173`，即可看到展品数据统计页面。

## 数据模型

### ExhibitionRecord（展品记录）

```java
@Entity
public class ExhibitionRecord {
    private Long id;           // 主键
    private String sn;         // SN编号
    private String category;   // 展品分类
    private LocalDateTime visitTime;  // 访问时间
}
```

### 响应数据结构

```json
{
  "timestamp": "2026-04-21 15:30:00",
  "data": {
    "古代文物": [
      { "hour": "08:00-09:00", "count": 3 },
      { "hour": "09:00-10:00", "count": 2 },
      ...
    ],
    "艺术品": [
      { "hour": "08:00-09:00", "count": 2 },
      ...
    ]
  },
  "totalRecords": 47
}
```

## 定时任务

系统每小时自动执行一次统计任务（cron表达式：`0 0 * * * *`），统计当天的数据并按分类汇总。

## 前端界面功能

1. **实时连接状态显示**：WebSocket连接状态指示器
2. **数据总览**：显示总数据量
3. **ECharts图表**：堆叠柱状图展示每小时各分类的SN数量
4. **分类统计卡片**：展示各分类今日总SN数
5. **详细数据表格**：每小时各分类的详细数据
6. **手动刷新按钮**：支持手动触发数据刷新

## 开发说明

### 添加新的展品分类

只需在数据库中插入新分类的数据即可，系统会自动识别并统计。

### 修改定时任务频率

在 `ExhibitionStatsService.java` 中修改 `@Scheduled` 注解：
```java
@Scheduled(cron = "0 0/30 * * * *")  // 每30分钟执行一次
```

## 常见问题

1. **数据库连接失败**：检查数据库配置是否正确，MySQL服务是否启动
2. **WebSocket连接失败**：检查后端服务是否正常运行，端口是否被占用
3. **前端无法获取数据**：检查后端CORS配置，确保允许跨域访问

## 联系方式

如有问题，请提交Issue或联系开发团队。
