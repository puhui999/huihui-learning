# Java AI 应用开发学习计划（17 周）

> 制定：2026-09-01 ｜ 学员：5 年 Java 全栈 ｜ 助教：Claude（本目录会话）
> 底稿由通义千问生成，经逐条事实核查后修订：版本已验证、评估前置、权限过滤补齐、框架做减法、作品集 4 改 2。

## 最终目标

不碰模型训练，走纯 Java 应用层路线，17 周后能独立交付企业级 AI Agent 系统。
作品集：**1 个生产级旗舰项目（企业知识库问答）+ 1 个副项目（NL2SQL 数据助手）**，外人 clone 后 30 分钟能跑起来，README 里有真实评估数据。

## 技术基线（2026-09 已核实）

| 层级 | 选型 | 版本 | 备注 |
|---|---|---|---|
| 运行时 | JDK | 25 | 最新 LTS；虚拟线程、Scoped Values 已转正。Boot 4.0.8 官方兼容到 Java 26 |
| 基础框架 | Spring Boot | 4.0.x | Spring AI 2.0 的硬依赖（Framework 7） |
| 核心框架 | Spring AI | **2.0.1+** | 不要用 2.0.0（CVE-2026-59318 提示注入） |
| 对比学习 | LangChain4j | 1.19.x | 只花 3-5 天，学 @AiService 风格 |
| 阿里生态 | Spring AI Alibaba | 1.1.2.0 | Graph 编排、DataAgent、DashScope |
| 工具协议 | MCP Java SDK | 2.0.x | 对应 2025-11-25 协议规范 |
| 向量库 | PgVector | — | 入门首选；Milvus 只做阅读 |
| 本地推理 | 不采用 | — | 2026-09-02 决定不自建模型服务，全程走 API |
| 模型 API | DeepSeek（对话）+ 通义百炼（Embedding、备用对话） | — | OpenAI 兼容；DeepSeek 无 Embedding 接口，RAG 用百炼 text-embedding-v4；全程费用 < ¥100 |
| 概念阅读 | A2A（SDK 实际仍 0.3.x）、AgentScope Java | — | 与 Spring AI Alibaba 合流中，不重仓 |

## 路线总览

| 阶段 | 周次 | 主题 | 预算 | 计划文件 | 产出 |
|---|---|---|---|---|---|
| 0 | 第 0 周 | 环境 + Spring Boot 4 前置 | ~12h | [stage-0](plan/stage-0-setup.md) | Hello ChatClient 跑通 |
| 1 | 1-2 | 裸调 API 与流式输出 | ~50h | [stage-1](plan/stage-1-api-basics.md) | demo-01-chat-sse |
| 2 | 3-4 | Spring AI 2.0 核心 | ~50h | [stage-2](plan/stage-2-spring-ai-core.md) | demo-02-tool-assistant |
| 3 | 5-8 | RAG + 评估 ⭐ | ~100h | [stage-3](plan/stage-3-rag-evals.md) | demo-03-knowledge-base + 评估报告 |
| 4 | 9-11 | Agent + MCP ⭐ | ~75h | [stage-4](plan/stage-4-agent-mcp.md) | demo-04-support-agent |
| 5 | 12-13 | 编排 + 生产韧性 | ~50h | [stage-5](plan/stage-5-orchestration.md) | demo-05-workflow |
| 6 | 14-17 | 作品集打磨 | ~100h | [stage-6](plan/stage-6-portfolio.md) | 旗舰 + 副项目两个独立仓库 |

总计 ~437h。按**在职每周 ~25h** 排期；脱产每周 40h 可压到 11-12 周（顺序不变，逐阶段提前）。

## 进度看板

| 阶段 | 状态 | 开始日期 | 验收日期 | 备注 |
|---|---|---|---|---|
| 0 前置 | ✅ 已验收 | 2026-09-01 | 2026-09-03 | runbook 见 notes/w00-环境搭建.md；Boot 4 差异速览搁置，阶段 1 遇到再补 |
| 1 裸调 API | ⬜ 未开始 | | | |
| 2 Spring AI 核心 | ⬜ 未开始 | | | |
| 3 RAG + 评估 | ⬜ 未开始 | | | |
| 4 Agent + MCP | ⬜ 未开始 | | | |
| 5 编排 + 韧性 | ⬜ 未开始 | | | |
| 6 作品集 | ⬜ 未开始 | | | |

状态：⬜ 未开始 / 🔵 进行中 / ✅ 已验收。每次验收由助教更新此表。

## 目录约定

本计划是聚合学习仓库 `huihui-learning` 的 `ai/` track（仓库总览与其他学习方向见根 [README](../README.md)）：

```
ai/
├── PLAN.md                  # 本文件：总览 + 进度看板
├── plan/                    # 各阶段详细计划
├── notes/                   # 学习笔记与踩坑记录（见 notes/README.md）
├── demo-00-hello/           # 阶段 0 产出：Hello ChatClient（Java 项目，下同）
├── demo-01-chat-sse/        # 阶段 1 产出
├── demo-02-tool-assistant/
├── demo-03-knowledge-base/
├── demo-04-support-agent/
└── demo-05-workflow/
```

demo 随学随提交到本仓库；阶段 6 的旗舰/副项目打磨完成后抽成独立仓库用于作品集展示，此处保留开发版。

## 助教协作方式

对助教说这些话即可触发对应动作：

- **"开始阶段 N"** — 按阶段文件展开本周/今天的任务清单，给起步指引
- **直接贴报错 / 贴代码** — 随时，卡住不要自己熬超过 30 分钟
- **"验收阶段 N"** — 按该阶段"助教检查点"提问 + review demo 代码 + 核验收标准 + 更新进度看板
- **"周复盘"** — 出 5-8 道本周知识小测 + 生成复盘笔记初稿进 notes/
- **"调整计划"** — 进度快/慢或方向变化时重排后续阶段

环境分工：实操与 demo 验证在 Windows 主机（Ryzen 9 9950X，JDK 25 + IDEA + Docker Desktop）上进行，Mac 只运行助教会话。验收前先 push，助教 pull 后 review 代码。

三条纪律（比计划本身重要）：
1. 每个知识点当天落成**能运行的代码**，不留"看懂了"的幻觉
2. 阶段 3 起，任何检索相关改动必须跑评估脚本，用数字说话
3. 每阶段验收通过才进下一阶段；卡住超过 2 天必须说出来，助教来拆小
