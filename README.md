# Bank AI Assistant

银行内部 AI 企业智能助手，支持制度知识库问答、文档解析入库、RAG 混合检索、业务工具调用、流式对话、写操作二次确认与全链路安全审计。

## 技术基线

- JDK 17
- Spring Boot 3.3.5
- MyBatis
- Flyway
- MySQL 8
- Redis
- RabbitMQ
- Elasticsearch 8.x
- 阿里云 OSS
- 通义千问 Qwen 与 text-embedding-v2

## 快速开始

复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

启动本地依赖：

```powershell
docker compose up -d
```

编译和测试：

```powershell
.\mvnw.cmd test
```

如果当前机器默认 Java 或 Maven 版本过低，可以使用项目脚本：

```powershell
.\scripts\build.ps1
```

启动应用：

```powershell
.\scripts\run-local.ps1
```

更多说明见 [本地开发文档](D:/workplace/codex/docs/LOCAL_DEVELOPMENT.md) 和 [详细设计文档](D:/workplace/codex/docs/DETAILED_DESIGN_AND_ROADMAP.md)。

## 数据库迁移

项目使用 Flyway 管理数据库结构，迁移脚本位于：

```text
src/main/resources/db/migration
```

已执行过的 `V*.sql` 不应再修改。后续变更请新增脚本，例如：

```text
V2__add_xxx_column.sql
```

## 本地服务地址

- 应用: http://localhost:8080
- RabbitMQ 控制台: http://localhost:15672
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601

## 当前阶段说明

阶段一工程基线已包含：

- Maven Wrapper
- Docker Compose 本地依赖环境
- Flyway 初始化迁移
- MyBatis 持久化
- 基础单元测试
- 本地构建和启动脚本
- UTF-8 配置与 Prompt 清理
