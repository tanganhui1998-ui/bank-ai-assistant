# 阶段四：真实身份权限与业务系统接入

## 身份接入

系统现在支持由网关透传真实用户身份，默认 Header 如下：

- `X-User-Id`：用户 ID
- `X-User-Name`：用户姓名，支持 URL 编码
- `X-Tenant-Id`：租户 ID
- `X-User-Roles`：角色列表，逗号分隔
- `X-User-Departments`：部门列表，逗号分隔
- `X-Data-Scope`：数据范围，如 `SELF`、`DEPARTMENT`、`ALL`
- `X-Branch-No`：机构号

本地开发默认启用 Mock fallback。生产环境必须配置：

```yaml
app:
  security:
    identity:
      mock-fallback-enabled: false
```

## 权限控制

业务工具权限现在同时参考角色、本人操作和数据范围：

- 薪资查询：本人可查；HR 且数据范围为 `DEPARTMENT` 或 `ALL` 可查范围内他人。
- 审批写操作：`APPROVER` 或 `APPROVAL_MANAGER` 可提交审批意见。
- 外包写操作：`OUTSOURCING_MANAGER` 或 `PROJECT_MANAGER` 可创建入场申请。
- `ADMIN` 默认具备全部业务工具权限。

## 业务系统接入

业务查询和写入已经抽象为 Client 接口：

- `LeaveBusinessClient`
- `SalaryBusinessClient`
- `ApprovalBusinessClient`
- `OutsourcingBusinessClient`
- `BusinessWriteClient`

默认 `app.business-integration.enabled=false` 使用 Mock Client。
设置为 `true` 后使用 HTTP Client，并向下游透传当前用户身份 Header。

写操作仍然只能通过 AI 二次确认接口触发，确认后由 `BusinessWriteClient` 调用真实业务系统，
同时保留本地 `ai_business_order` 记录用于对账和审计。
