# 阶段五：对话体验与前端协议

## 非流式对话

接口：`POST /api/assistant/chat`

请求：

```json
{
  "sessionId": "c-001",
  "clientMessageId": "m-001",
  "message": "外包人员入场需要哪些材料？"
}
```

响应中的 `data` 增加：

- `messageId`：后端使用前端传入的 `clientMessageId`，未传则自动生成。
- `conversationState`：`NORMAL`、`WAITING_SLOT`、`WAITING_CONFIRM`、`COMPLETED`。
- `pendingOperationId`：待确认操作 ID。
- `suggestedActions`：前端建议展示动作，如 `confirm`、`cancel`、`reply`。

## 流式对话 SSE

接口：`POST /api/assistant/chat/stream`

所有 SSE 的 `data` 都是统一 envelope：

```json
{
  "eventId": "uuid",
  "type": "message",
  "conversationId": "c-001",
  "messageId": "m-001",
  "sequence": 3,
  "payload": {},
  "timestamp": "2026-07-05T14:00:00"
}
```

事件类型：

- `conversation_start`：流开始，返回会话 ID 和消息 ID。
- `status`：后端处理阶段，如 `intent_recognized`、`retrieving`。
- `message`：模型文本增量，payload 为 `{ "delta": "文本" }`。
- `retrieval_result`：RAG 命中数量、引用和 trace。
- `low_confidence`：低置信兜底。
- `tool_start`：工具调用开始。
- `tool_result`：工具调用完成。
- `slot_required`：需要用户补充槽位。
- `confirm_required`：需要展示确认卡片。
- `done`：本轮结束。
- `error`：异常或超时。

## 确认卡片

新版接口：`POST /api/assistant/confirm`

```json
{
  "pendingId": "pending-001",
  "action": "confirm",
  "comment": "确认提交"
}
```

`action=cancel` 表示取消。旧接口 `/api/assistant/confirm/{pendingId}` 和
`/api/assistant/confirm/{pendingId}/cancel` 保留兼容。

## 会话状态查询

接口：`GET /api/assistant/conversations/{conversationId}`

用于前端刷新页面后恢复会话状态、确认卡片、缺失槽位和最近消息历史。
