# AI 企业智能助手本地运行操作文档

本文档说明如何在 Windows 本地把 `bank-ai-assistant` 跑起来，并验证基础接口、对话接口和知识库链路。

## 1. 本地目录

项目根目录：

```powershell
D:\workplace\codex\bank-ai-assistant
```

进入项目：

```powershell
cd D:\workplace\codex\bank-ai-assistant
```

## 2. 必备环境

建议安装：

- JDK 17
- Docker Desktop
- Git
- PowerShell 5+

项目已经包含 Maven Wrapper，但当前脚本会优先使用以下本机路径：

```text
D:\software\Java\jdk17
D:\software\apache-maven-3.6.3\bin
```

如果你的 JDK 17 安装路径不同，可以修改 `scripts/build.ps1` 中的 `$jdkHome`。

验证 Java：

```powershell
java -version
```

需要看到 Java 17。Spring Boot 3.x 不能用 Java 8 编译运行。

## 3. 准备环境变量

复制模板：

```powershell
Copy-Item .env.example .env
```

打开 `.env`，至少确认这些本地依赖配置：

```properties
MYSQL_USERNAME=root
MYSQL_PASSWORD=root123456
MYSQL_URL=jdbc:mysql://localhost:3306/bank_ai_assistant?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
REDIS_HOST=localhost
RABBITMQ_HOST=localhost
ELASTICSEARCH_URIS=http://localhost:9200
```

如果要测试真实 AI 对话、文档解析入库、Embedding 和 OSS 上传，需要配置真实密钥：

```properties
QWEN_API_KEY=你的通义千问APIKey
ALIYUN_OSS_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com
ALIYUN_OSS_ACCESS_KEY_ID=你的AccessKeyId
ALIYUN_OSS_ACCESS_KEY_SECRET=你的AccessKeySecret
ALIYUN_OSS_BUCKET=你的Bucket
```

只验证工程启动、健康检查、部分 Mock 业务能力时，可以暂时保留模板里的 dummy 值；涉及 Qwen 或 OSS 的接口会因为密钥不可用而失败。

## 4. 启动本地中间件

启动 MySQL、Redis、RabbitMQ、Elasticsearch、Kibana：

```powershell
docker compose up -d
```

查看容器状态：

```powershell
docker compose ps
```

常用地址：

- RabbitMQ 控制台：http://localhost:15672，账号 `guest`，密码 `guest`
- Elasticsearch：http://localhost:9200
- Kibana：http://localhost:5601
- MySQL：`localhost:3306`，库名 `bank_ai_assistant`
- Redis：`localhost:6379`

首次启动 Elasticsearch 会比较慢，可以等待 1-2 分钟。

### 4.1 RabbitMQ 安装与启用

本项目推荐用 Docker Compose 启动 RabbitMQ，镜像已在 `docker-compose.yml` 中配置：

```yaml
rabbitmq:
  image: rabbitmq:3.13-management
  container_name: bank-ai-rabbitmq
  ports:
    - "5672:5672"
    - "15672:15672"
```

启动 RabbitMQ：

```powershell
docker compose up -d rabbitmq
```

确认容器健康：

```powershell
docker compose ps rabbitmq
docker compose logs -f rabbitmq
```

访问管理控制台：

```text
http://localhost:15672
```

默认账号密码：

```text
username: guest
password: guest
```

Spring Boot 连接 RabbitMQ 使用 `.env` 中的配置：

```properties
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VHOST=/
```

应用启动后会自动创建文档解析交换机、主队列和死信队列。可以在 RabbitMQ 控制台查看：

- Exchanges：`ai.document.exchange`、`ai.document.parse.dlx`
- Queues：`ai.document.parse.queue`、`ai.document.parse.dlq`

如果你想单独验证 RabbitMQ 是否可用，可以执行：

```powershell
docker exec bank-ai-rabbitmq rabbitmq-diagnostics -q ping
```

返回 `Ping succeeded` 表示 RabbitMQ 正常。

### 4.2 Elasticsearch 安装与启用

本项目使用 Elasticsearch 8.15.3，Docker Compose 已关闭本地安全认证，便于开发：

```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.15.3
  environment:
    discovery.type: single-node
    xpack.security.enabled: "false"
    ES_JAVA_OPTS: -Xms1g -Xmx1g
```

启动 Elasticsearch：

```powershell
docker compose up -d elasticsearch
```

查看启动日志：

```powershell
docker compose logs -f elasticsearch
```

健康检查：

```powershell
Invoke-RestMethod http://localhost:9200
Invoke-RestMethod http://localhost:9200/_cluster/health
```

正常情况下可以看到集群名、版本号和健康状态。单节点开发环境常见状态：

```json
{
  "status": "green"
}
```

Spring Boot 连接 Elasticsearch 使用 `.env` 中的配置：

```properties
ELASTICSEARCH_URIS=http://localhost:9200
ELASTICSEARCH_USERNAME=
ELASTICSEARCH_PASSWORD=
```

应用启动时会初始化知识库切片索引。索引名默认：

```text
bank-ai-document-chunk
```

验证索引：

```powershell
Invoke-RestMethod http://localhost:9200/_cat/indices?v
Invoke-RestMethod http://localhost:9200/bank-ai-document-chunk/_mapping
```

如果还没有上传并解析文档，索引可能存在但没有数据。查看文档数量：

```powershell
Invoke-RestMethod http://localhost:9200/bank-ai-document-chunk/_count
```

### 4.3 Kibana 安装与启用

Kibana 用于查看 Elasticsearch 索引、Mapping 和检索数据。Docker Compose 已配置：

```yaml
kibana:
  image: docker.elastic.co/kibana/kibana:8.15.3
  environment:
    ELASTICSEARCH_HOSTS: http://elasticsearch:9200
  ports:
    - "5601:5601"
```

启动 Kibana：

```powershell
docker compose up -d kibana
```

Kibana 依赖 Elasticsearch 健康，建议先确认 ES 正常后再打开：

```powershell
Invoke-RestMethod http://localhost:9200/_cluster/health
```

访问 Kibana：

```text
http://localhost:5601
```

进入 Kibana 后，打开：

```text
Management -> Dev Tools
```

常用查询：

```text
GET _cat/indices?v
GET bank-ai-document-chunk/_mapping
GET bank-ai-document-chunk/_search
{
  "size": 5,
  "_source": ["chunkId", "documentId", "documentName", "chapterPath", "content"]
}
```

如果 Kibana 页面提示无法连接 Elasticsearch，通常是 ES 还没启动完成。等待 1-2 分钟后刷新，或查看日志：

```powershell
docker compose logs -f kibana
docker compose logs -f elasticsearch
```

### 4.4 一键重启 RabbitMQ/ES/Kibana

只重启这三个服务：

```powershell
docker compose restart rabbitmq elasticsearch kibana
```

只查看这三个服务状态：

```powershell
docker compose ps rabbitmq elasticsearch kibana
```

如果本地数据损坏，需要清空重建，慎用：

```powershell
docker compose down -v
docker compose up -d rabbitmq elasticsearch kibana
```

## 5. 编译与测试

推荐先跑测试，确认依赖和 JDK 正确：

```powershell
.\mvnw.cmd -q test
```

如果你的系统默认 Java/Maven 版本不对，使用项目脚本：

```powershell
.\scripts\build.ps1
```

脚本会优先把 `D:\software\Java\jdk17` 放到 `JAVA_HOME`。

## 6. 启动应用

推荐使用脚本启动：

```powershell
.\scripts\run-local.ps1
```

脚本会自动：

1. 读取 `.env` 环境变量。
2. 使用 JDK 17 编译。
3. 设置 `SPRING_PROFILES_ACTIVE=local`。
4. 执行 `mvn spring-boot:run`。

应用启动成功后，默认监听：

```text
http://localhost:8080
```

Flyway 会在启动时自动创建和升级数据库表。

## 7. 健康检查

浏览器或 PowerShell 访问：

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

正常会看到：

```json
{
  "status": "UP"
}
```

Prometheus 指标：

```text
http://localhost:8080/actuator/prometheus
```

## 8. 验证对话接口

本地默认启用 Mock 用户，不传身份 Header 也可以访问。

非流式对话：

```powershell
$body = @{
  sessionId = "local-session-001"
  clientMessageId = "msg-001"
  message = "你好，你能做什么？"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/assistant/chat" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

注意：该接口会调用通义千问。如果 `.env` 中 `QWEN_API_KEY` 仍是 `replace-me` 或 dummy 值，AI 生成会失败。此时说明工程已启动，但模型能力未配置真实密钥。

会话状态查询：

```powershell
Invoke-RestMethod http://localhost:8080/api/assistant/conversations/local-session-001
```

流式 SSE 接口可用前端 EventSource 或 Postman/Apifox 测试：

```text
POST http://localhost:8080/api/assistant/chat/stream
Content-Type: application/json

{
  "conversationId": "local-stream-001",
  "clientMessageId": "msg-stream-001",
  "message": "请介绍一下你能办理哪些事项"
}
```

## 9. 验证业务工具 Mock 能力

业务工具会走当前 Mock 用户：

```text
用户ID：u1001
用户姓名：管理员
租户：tenant_001
角色：ADMIN、KNOWLEDGE_MANAGER、CONFIDENTIAL_ACCESS、SECRET_ACCESS
部门：总行、科技部
```

如果配置了真实身份 Header，可以模拟真实用户：

```powershell
$headers = @{
  "X-User-Id" = "u2001"
  "X-User-Name" = [uri]::EscapeDataString("张三")
  "X-Tenant-Id" = "tenant_001"
  "X-User-Roles" = "EMPLOYEE,APPROVER"
  "X-User-Departments" = "科技部"
  "X-Data-Scope" = "SELF"
  "X-Branch-No" = "0101"
}
```

生产环境务必关闭 Mock fallback：

```properties
SECURITY_MOCK_FALLBACK_ENABLED=false
```

## 10. 验证文档上传

文档上传接口依赖真实 OSS。确认 `.env` 已配置阿里云 OSS 后再测试。

接口：

```text
POST http://localhost:8080/api/documents
Content-Type: multipart/form-data
```

表单字段：

- `file`：PDF/DOC/DOCX 文件
- `metadata`：JSON 字符串

`metadata` 示例：

```json
{
  "displayName": "外包人员管理办法",
  "documentType": "POLICY",
  "versionNo": "v1.0",
  "department": "科技部",
  "confidentialityLevel": "INTERNAL",
  "applicableScope": "全行",
  "publishingUnit": "科技部"
}
```

上传成功后流程：

1. 文件上传 OSS。
2. 文档元数据写入 MySQL。
3. 发送 RabbitMQ 解析消息。
4. 消费者异步解析 PDF/Word。
5. 切片入库并写入 Elasticsearch。

如果没有真实 Qwen Embedding API Key，解析后 ES 向量入库会失败或进入重试。

## 11. 常用排查命令

查看应用日志：看启动应用的 PowerShell 窗口。

查看 Docker 服务：

```powershell
docker compose ps
docker compose logs -f mysql
docker compose logs -f rabbitmq
docker compose logs -f elasticsearch
```

重启本地依赖：

```powershell
docker compose restart
```

彻底清理本地依赖数据，慎用：

```powershell
docker compose down -v
```

## 12. 常见问题

### 12.1 Maven 或 Java 版本不对

现象：提示 Maven 3.5.2、Java 8、`release version 17 not supported`。

处理：

```powershell
$env:JAVA_HOME='D:\software\Java\jdk17'
.\mvnw.cmd -q test
```

或者修改 `scripts/build.ps1` 中的 JDK 路径。

### 12.2 MySQL 连接失败

确认 Docker 容器启动：

```powershell
docker compose ps
```

确认 `.env` 中密码与 `docker-compose.yml` 一致，默认是：

```properties
MYSQL_PASSWORD=root123456
```

### 12.3 Elasticsearch 启动慢或 unhealthy

Elasticsearch 首次启动需要更多时间和内存。Docker Desktop 建议至少分配 4GB 内存。

如果长时间 unhealthy，按顺序检查：

```powershell
docker compose logs -f elasticsearch
Invoke-RestMethod http://localhost:9200/_cluster/health
```

常见原因：

- Docker Desktop 内存不足，建议分配 4GB 以上。
- 本机 9200 端口被其他 ES 占用。
- 首次启动正在初始化数据目录，需要等待。

端口占用检查：

```powershell
netstat -ano | findstr :9200
```

### 12.4 Kibana 无法打开

先确认 Kibana 容器和 ES 容器都在运行：

```powershell
docker compose ps kibana elasticsearch
```

查看日志：

```powershell
docker compose logs -f kibana
```

如果日志中出现连接 ES 失败，先等待 ES 健康：

```powershell
Invoke-RestMethod http://localhost:9200/_cluster/health
```

### 12.5 RabbitMQ 控制台打不开

确认 15672 端口没有被占用：

```powershell
netstat -ano | findstr :15672
```

确认 RabbitMQ 容器健康：

```powershell
docker compose ps rabbitmq
docker exec bank-ai-rabbitmq rabbitmq-diagnostics -q ping
```

如果登录失败，确认 `.env` 和 `docker-compose.yml` 中的账号密码一致。默认是 `guest/guest`。

### 12.6 AI 对话失败

多数情况下是 `QWEN_API_KEY` 未配置真实值。配置后重新启动应用。

### 12.7 文档上传失败

文档上传依赖阿里云 OSS，检查：

- `ALIYUN_OSS_ENDPOINT`
- `ALIYUN_OSS_ACCESS_KEY_ID`
- `ALIYUN_OSS_ACCESS_KEY_SECRET`
- `ALIYUN_OSS_BUCKET`

### 12.8 Git 提示 `.git/index.lock`

如果 Windows 下出现残留 lock，确认没有 Git 进程后删除：

```powershell
Get-Process git -ErrorAction SilentlyContinue
Remove-Item -LiteralPath .git\index.lock -Force
```

## 13. 推荐本地启动顺序

```powershell
cd D:\workplace\codex\bank-ai-assistant
Copy-Item .env.example .env
docker compose up -d
.\mvnw.cmd -q test
.\scripts\run-local.ps1
Invoke-RestMethod http://localhost:8080/actuator/health
```
