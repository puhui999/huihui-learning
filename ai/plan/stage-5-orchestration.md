# 阶段 5：编排 + 生产韧性（第 12-13 周，~50h）

> 一句话目标：会编排多角色协作，且系统在模型抽风/超时/降级时依然体面。
> 定位提醒：2026 年生产里大部分价值仍来自"单 Agent + 好工具 + 好 RAG"。多 Agent 学到"懂模式、能落一个 demo"即可，不必陷进去。

## 第 12 周：多 Agent 编排

- [ ] 三种协作模式：分工流水线 / Supervisor 调度 / Routing 意图路由
- [ ] Spring AI Alibaba Graph 引擎：节点、条件分支、并行、人机协同（Human-in-the-Loop）节点
  （不想引入 SAA 也可自实现简化版 Supervisor：一个调度 ChatClient + 若干专职 ChatClient）
- [ ] 概念阅读（各半天，不写码）：
  - [ ] A2A 协议：跨服务 Agent 互联的思路；SDK 生态仍 0.3.x，未到重仓时机
  - [ ] AgentScope Java：HarnessAgent / 权限引擎；它正成为 Spring AI Alibaba 的内核，两者在合流

## 第 13 周：生产韧性

- [ ] SemanticCacheAdvisor 语义缓存（Redis 向量存储）：命中判定、system prompt 哈希隔离、失效策略
  （注意：真名不是 RedisSemanticCacheAdvisor）
- [ ] Resilience4j：超时 / 重试（分清哪些错误可重试）/ 熔断 / 降级话术
- [ ] 多模型 Failover：主模型故障切备用模型、权重路由
- [ ] Ollama 本地链路：开发/测试环境完全离线跑通
- [ ] Java 21 虚拟线程：I/O 密集的工具调用并行化实测

## 实战任务：demo-05-workflow

三角色协作：需求分析 Agent → 代码生成 Agent → 代码审查 Agent（审查不过打回重写，最多 2 轮）：
1. Supervisor 或 Graph 编排，含条件分支与循环
2. 语义缓存对重复需求生效
3. 注入主模型故障后自动 failover，全程降级日志

### 验收标准（DoD）
- 一次完整流水线的每步输入输出可追溯
- 人为把主模型 base-url 改错，系统 30 秒内自愈到备模型
- 缓存命中率与延迟对比有数字

## 助教检查点
1. 什么任务适合拆多 Agent？什么任务拆了反而更差？
2. 语义缓存误命中（不同问题吃到同一答案）怎么防？
3. 重试策略里，哪类错误绝不能重试？为什么？

## 搜索关键词
Spring AI Alibaba Graph 工作流 ｜ multi-agent supervisor pattern ｜ Spring AI SemanticCacheAdvisor ｜ Resilience4j Spring Boot ｜ Java 21 virtual threads
