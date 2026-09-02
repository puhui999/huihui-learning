# 阶段 6：作品集打磨（第 14-17 周，~100h）

> 一句话目标：两个"外人 clone 后 30 分钟能跑起来"的生产级仓库。
> 与原计划最大差异：4 个项目砍成 2 个。2026 年人人都有 AI 生成的 demo，**深度 + 真实评估数字 + 在线可访问**才是差异化。

## 第 14-16 周：旗舰——企业知识库问答（基于 demo-03 升级）

按生产标准补齐。每一项都是你全栈老本行的主场：

### 安全与多租户
- [ ] Spring Security：登录鉴权、接口权限
- [ ] 多租户：租户级数据隔离（向量库 filter + 表隔离），文档级 ACL 延续阶段 3
- [ ] 敏感数据脱敏；审计日志（谁问了什么、引用了哪些文档）
- [ ] Prompt Injection 深度防护：外部文档内容与指令分层

### 可观测与成本
- [ ] OpenTelemetry + Micrometer：调用链、延迟 P95、工具调用成功率
- [ ] Token 成本面板：按租户/按天统计，预算告警
- [ ] Actuator 暴露 AI 指标；Prompt 版本管理（改动可追溯、可回滚）

### 交付
- [ ] Docker 镜像 + Docker Compose 一键起（app + PgVector + Redis + Ollama 可选）
- [ ] GitHub Actions CI：构建 + 测试 + 镜像
- [ ] 在线 demo（云服务器或内网穿透，可访问即可）
- [ ] 文档四件套：README（30 分钟跑通指引）/ 架构图 / 评估报告（阶段 3 数字的更新版）/ 技术取舍说明（为什么 PgVector 不是 Milvus、为什么这样切分……）

## 第 17 周：副项目——NL2SQL 数据助手

克制范围，一周做完：
- [ ] Schema 注入 + 自然语言生成 SQL（Spring AI Alibaba DataAgent 或自实现）
- [ ] **只读执行**：数据库账号锁死 SELECT，防注入防误删（面试必问的安全点）
- [ ] 结果解释：查询结果转人话 + 简单图表数据
- [ ] README + 一键启动

## 收尾清单
- [ ] 两个仓库独立出去（脱离学习仓库），命名、License、截图/GIF
- [ ] 找一个不了解项目的人按 README 冒烟一遍，卡点全修掉
- [ ] （强烈建议）3-4 篇技术博客：评估驱动的 RAG 调优 / 手写 MCP Server / Advisor 链实践 / 从 Demo 到生产清单
- [ ] 简历表述打磨：每个项目一句"业务价值 + 技术难点 + 数字"

## 助教检查点（终验）
1. 助教扮演面试官，对两个项目做 30 分钟深挖（架构、取舍、失败案例、数字来源）
2. 助教按 README 从零跑一遍，计时
3. 安全走查：注入、越权、成本失控三个方向各出 3 个攻击场景

## 搜索关键词
Spring AI production deployment ｜ Spring AI observability OpenTelemetry ｜ NL2SQL security read-only ｜ multi-tenant RAG
