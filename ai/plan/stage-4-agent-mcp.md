# 阶段 4：Agent + MCP（第 9-11 周，~75h）⭐ 核心突破

> 一句话目标：做出能自主多步规划、通过 MCP 调工具、带记忆和防注入的 Agent。

## 第 9 周：Agent 范式

- [ ] ReAct（Thought → Action → Observation 循环）/ Plan-and-Execute / Reflection 三种范式与适用场景
- [ ] 用 ToolCallingAdvisor 组合出多步 Agent：最大迭代数、循环终止条件、每步可观测
- [ ] **ToolSearchToolCallingAdvisor** 渐进式工具披露：工具多（30+）时先索引、模型按需语义检索工具（官方基准省 34-64% token）；从配置 `spring.ai.chat.client.tool-search-advisor.enabled=true` 起步
- [ ] Agent 三大失败模式各写一个复现 + 防御：死循环 / 工具幻觉（调不存在的工具）/ 参数编造

## 第 10 周：MCP 协议

- [ ] MCP 概念：为什么要标准化工具协议；tools / resources / prompts 三类能力；Client-Server 架构
- [ ] **用 MCP Java SDK 2.0 亲手写一个 MCP Server**：把 demo-03 的知识库检索包装成 MCP 工具（这一步价值最大，比用现成 server 学得多）
- [ ] Spring AI 作为 MCP Client：接自己的 server + 一个现成 server（如文件系统）
- [ ] MCP 安全：鉴权（mcp-security 的 OAuth2 / API Key）；警惕恶意 server 返回内容也是注入面

## 第 11 周：LangChain4j 对比（3-5 天）+ 记忆分层

- [ ] LangChain4j @AiService 声明式：@SystemMessage / @UserMessage / @V / @MemoryId
- [ ] 用它复刻 demo-02 的一个子集，写一篇《Spring AI vs LangChain4j》笔记进 notes/（面试高频题）
- [ ] 记忆分层：短期（对话窗口）/ 工作（当前任务上下文）/ 长期（用户画像，向量库存摘要）
- [ ] Token 预算：历史过长时的摘要压缩策略

## 实战任务：demo-04-support-agent

智能客服 Agent（电商场景 mock）：
1. 多轮对话 + 记忆持久化（跨会话记住用户偏好）
2. 工具经 MCP 接入：订单查询、退款政策检索（连 demo-03）、工单创建
3. 多步任务："我上周买的东西坏了怎么办" → 查订单 → 查政策 → 建工单，全程可观测
4. 防注入：用户消息与外部内容（商品评论等）分层处理；危险操作（退款）要求二次确认

### 验收标准（DoD）
- 一次多步任务的每轮 Thought/Action/Observation 可回放
- 拔掉 MCP server，Agent 优雅降级而不是 500
- 注入攻击测试用例 ≥ 5 条且全部有防御行为

## 助教检查点
1. 手画你的 Agent 一次完整任务的时序图（含 MCP 往返）
2. MCP 接入 vs 直接 @Tool，什么时候选哪个？
3. ToolSearch 渐进披露在你的场景省了多少 token？（要有数字）
4. "工单创建"这类写操作，确认机制怎么防绕过？

## 搜索关键词
Spring AI agentic patterns ｜ MCP Java SDK server tutorial ｜ Spring AI MCP client ｜ LangChain4j AiService ｜ prompt injection agent defense
