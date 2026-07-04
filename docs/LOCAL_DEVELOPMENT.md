# 本地开发与可运行环境

## 1. 基础要求

- JDK 17
- Maven 3.6.3 或更高版本
- Docker Desktop
- PowerShell 7 或 Windows PowerShell

当前项目已经从 JPA 迁移到 MyBatis，并使用 Flyway 管理数据库结构。启动应用前请先启动 MySQL、Redis、RabbitMQ 和 Elasticsearch。

## 2. 启动基础设施

复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

启动依赖服务：

```powershell
docker compose up -d
```

常用控制台：

- RabbitMQ: http://localhost:15672
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601

## 3. 编译项目

推荐使用项目脚本：

```powershell
.\scripts\build.ps1
```

也可以直接使用 Maven：

```powershell
mvn clean test
```

如果本机默认 Maven 或 Java 版本较低，请显式指定 JDK 17 和 Maven 3.6.3。
项目已经包含 Maven Wrapper，也可以直接运行：

```powershell
.\mvnw.cmd test
```

## 4. 启动应用

```powershell
.\scripts\run-local.ps1
```

应用启动时 Flyway 会自动执行 `src/main/resources/db/migration` 下的迁移脚本。首次启动会创建 `flyway_schema_history` 表和业务表。

## 5. Flyway 约定

- 已执行的迁移文件不能修改。
- 新增表结构变更时增加新文件，例如 `V2__add_xxx_table.sql`。
- 已有库接入时，`baseline-on-migrate=true` 会允许 Flyway 建立基线并继续迁移。

## 6. 当前外部依赖说明

本地 Docker Compose 不包含阿里云 OSS 和通义千问。涉及真实上传、Embedding、对话生成的流程需要配置真实环境变量：

- `ALIYUN_OSS_ENDPOINT`
- `ALIYUN_OSS_ACCESS_KEY_ID`
- `ALIYUN_OSS_ACCESS_KEY_SECRET`
- `ALIYUN_OSS_BUCKET`
- `QWEN_API_KEY`

没有真实密钥时，普通编译和离线单元测试可以正常运行。

## 7. Testcontainers 冒烟测试

默认 `mvn test` 不会启动 Docker 容器。需要验证基础设施容器时设置：

```powershell
$env:RUN_TESTCONTAINERS='true'
.\mvnw.cmd test
```

该测试会临时启动 MySQL、Redis、RabbitMQ 和 Elasticsearch，用于 CI 或本机环境冒烟验证。
