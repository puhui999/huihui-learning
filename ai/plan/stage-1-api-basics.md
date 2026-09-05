# 阶段 1：裸调 API 与流式输出（第 1-2 周，~50h）

> 一句话目标：不借助 Spring AI，徒手理解 OpenAI 兼容协议的每一个字段，做出流式对话应用。
> 为什么裸调：框架的每层抽象你都见过"裸"的样子，后面 Debug 才不心虚。
> 2026-09-03 开始。第 1 周 runbook：[w01](../notes/w01-裸调API与多轮对话.md)。决策：模型 `deepseek-v4-flash` 并显式关思考模式；Web MVC + `spring-boot-starter-webclient`（不上 WebFlux 服务端）；usage 表放 pgvector 容器的 Postgres；成本折算带 DeepSeek 峰谷时段。

## 第 1 周：协议与多轮对话

### 核心概念（Day 1-2）
- [ ] Token 与上下文窗口：中文 token 特点、上下文上限对工程意味着什么
- [ ] temperature / top_p / max_tokens 各自影响什么；temperature=0 ≠ 完全确定
- [ ] "概率性输出"思维转换：同样输入可能不同输出，工程上如何对待
- [ ] messages 结构：system / user / assistant 角色分工

### API 直调（Day 3-5）
- [ ] WebClient 调 `/chat/completions`（DeepSeek 与百炼同一套写法，只换 base-url + key）
- [ ] 读懂响应：choices / finish_reason / usage（prompt_tokens、completion_tokens）
- [ ] 多轮对话：自己维护 messages 列表，体会"模型无状态、上下文靠自己带"
- [ ] usage 入库（一张表：时间、模型、tokens、折算成本）

## 第 2 周：流式 + Prompt 基础

### SSE 流式（Day 1-3）
- [ ] `stream: true` + `text/event-stream`，解析 `data:` 行与 `[DONE]` 结束标记
- [ ] WebFlux `Flux<ServerSentEvent>` 透传前端，简单 HTML/JS 打字机效果
- [ ] 断线/超时处理：客户端断开时取消上游请求，不留僵尸连接

### Prompt 工程入门（Day 4-7）
- [ ] Prompt 四段式：Role / Task / Context / Format
- [ ] System 与 User 的分工；few-shot 示例注入；CoT 思维链
- [ ] 结构化输出直调版：response_format 要 JSON，解析失败的重试兜底
- [ ] Prompt Injection 初识：亲手试"忽略以上指令"能否攻破自己的 system prompt
- [ ] Prompt 模板管理：模板存库/配置文件，支持变量替换

## 实战任务：`chat` 包（huihui-ai 工程）

在 huihui-ai 里新增 `chat` 包，跨阶段共用的 usage 记账放 `common`（2026-09-05 起不再每阶段建 demo 工程）：
1. 多轮对话（会话隔离，内存存储即可）
2. SSE 流式输出 + 打字机前端页
3. Token 用量面板接口：按天/按会话统计费用
4. Prompt 模板管理：至少 3 个模板（通用助手 / 翻译 / JSON 提取器）
5. 切换 DeepSeek ↔ 通义只改配置不改代码

### 验收标准（DoD）
- 流式响应中途断开不留僵尸请求
- usage 统计与平台后台账单对得上（误差可解释）
- README 写清启动方式与 API Key 注入方式

## 助教检查点（验收时会问）
1. 一次多轮对话进行到第 10 轮时，请求体里有什么？token 怎么涨的？怎么控制？
2. SSE 和 WebSocket 各适合什么场景？这里为什么选 SSE？
3. 打字机效果卡顿/乱序时，你会从哪几层排查？
4. system prompt 被注入攻破的实验结果是什么？你的初步对策？

## 搜索关键词
DeepSeek API 文档 ｜ 阿里云百炼 OpenAI 兼容接口 ｜ WebFlux SSE ServerSentEvent ｜ Prompt Engineering Guide
