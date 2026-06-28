# WorkBuddy 专属开发指南

> DevOps工程师角色 - 负责自动化运维和部署监控

---

## 一、角色定位

**角色**：DevOps工程师

**擅长领域**：自动化脚本、运维部署、监控

**负责模块**：Docker部署、监控告警

---

## 二、负责任务

| 任务ID | 任务名称 | 优先级 | 依赖任务 |
|--------|---------|-------|---------|
| TASK-OPS01 | Docker容器化部署 | medium | 各模块完成后 |
| TASK-OPS02 | 自动化备份脚本 | low | TASK-OPS01 |
| TASK-OPS03 | 监控告警配置 | low | TASK-OPS01 |

---

## 三、技术栈规范

### 3.1 DevOps技术栈

| 技术 | 版本 | 说明 |
|-----|------|-----|
| Docker | 24.x | 容器化部署 |
| Docker Compose | 2.x | 编排工具 |
| Nginx | 1.24.x | 反向代理 |
| Prometheus | 2.x | 监控系统 |
| Grafana | 10.x | 可视化仪表盘 |
| Shell | - | 自动化脚本 |

### 3.2 部署目录结构

```
SuperStudy1.0/
├── docker/
│   ├── backend/
│   │   └── Dockerfile
│   ├── frontend/
│   │   └── Dockerfile
│   ├── mysql/
│   │   └── Dockerfile
│   └── nginx/
│       ├── Dockerfile
│       └── nginx.conf
├── docker-compose.yml
├── scripts/
│   ├── backup.sh
│   ├── restore.sh
│   └── deploy.sh
└── monitoring/
    ├── prometheus.yml
    ├── grafana/
    │   └── dashboards/
    └── alertmanager.yml
```

---

## 四、Docker配置规范

### 4.1 后端Dockerfile

```dockerfile
FROM openjdk:8-jdk-alpine

WORKDIR /app

COPY target/superstudy-backend.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.2 前端Dockerfile

```dockerfile
FROM node:18-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### 4.3 docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: superstudy-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: superstudy
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - superstudy-network

  redis:
    image: redis:6.2
    container_name: superstudy-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - superstudy-network

  backend:
    build: ./docker/backend
    container_name: superstudy-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/superstudy
      SPRING_REDIS_HOST: redis
    depends_on:
      - mysql
      - redis
    networks:
      - superstudy-network

  frontend:
    build: ./docker/frontend
    container_name: superstudy-frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - superstudy-network

volumes:
  mysql-data:
  redis-data:

networks:
  superstudy-network:
    driver: bridge
```

---

## 五、自动化脚本规范

### 5.1 备份脚本

```bash
#!/bin/bash

BACKUP_DIR="/backup"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="superstudy"

mkdir -p $BACKUP_DIR

docker exec superstudy-mysql mysqldump -u root -proot $DB_NAME > $BACKUP_DIR/$DB_NAME_$DATE.sql

find $BACKUP_DIR -name "*.sql" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR/$DB_NAME_$DATE.sql"
```

### 5.2 部署脚本

```bash
#!/bin/bash

echo "Stopping existing containers..."
docker-compose down

echo "Building backend..."
cd backend && mvn clean package -DskipTests && cd ..

echo "Building frontend..."
cd frontend && npm install && npm run build && cd ..

echo "Starting containers..."
docker-compose up -d

echo "Deployment completed!"
```

---

## 六、监控配置规范

### 6.1 Prometheus配置

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql:9104']
  - job_name: 'redis'
    static_configs:
      - targets: ['redis:9121']
```

### 6.2 监控指标

| 指标 | 说明 | 阈值 |
|-----|------|-----|
| API响应时间 | 平均响应时间 | >200ms告警 |
| 数据库连接数 | 活跃连接数 | >80%告警 |
| Redis内存使用率 | 内存占用 | >80%告警 |
| 容器CPU使用率 | CPU占用 | >90%告警 |
| 容器内存使用率 | 内存占用 | >90%告警 |

---

## 七、验收标准

### 7.1 部署验收

- [ ] Docker构建成功
- [ ] 容器启动正常
- [ ] 服务可访问
- [ ] 数据持久化正常

### 7.2 监控验收

- [ ] 监控指标采集正常
- [ ] 告警规则配置正确
- [ ] 备份脚本执行正常

---

## 八、参考文档

| 文档 | 用途 |
|-----|------|
| [AI快捷执行手册.md](AI快捷执行手册.md) | 任务卡片和执行指南 |
| [项目目录结构规范.md](项目目录结构规范.md) | 目录结构和命名规范 |
| [迭代任务映射表.md](迭代任务映射表.md) | 迭代计划和任务分配 |