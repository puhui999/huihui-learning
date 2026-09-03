# 阶段 3：RAG + 评估（第 5-8 周，~100h）⭐ 重点攻坚

> 一句话目标：从零搭企业知识库问答，且**每一次调优都有评估数字背书**。
> 与原计划最大差异：评估不是最后一节，而是第 6 周就建好、之后每周都跑的基础设施。没有 golden set 的调优全是玄学。

## 第 5 周：文档管道（解析 → 切分 → 向量化 → 入库）

- [ ] Apache Tika 解析 PDF / Word / Markdown；DocumentReader / DocumentTransformer 接口
- [ ] 切分策略：固定长度 / 递归 / 语义切分对比；经验值 chunk 500-1500 字符、overlap 10-20%
- [ ] 元数据设计（关键）：来源文件、章节、更新时间、**权限标签（部门/角色）**——第 7 周要用
- [ ] Embedding 选型：百炼 text-embedding-v4 为主（DeepSeek 无 Embedding 接口，开工时核实最新版本），可对比 v3 或 OpenAI text-embedding-3-small；维度与建表的关系
- [ ] PgVector 入库：VectorStore 接口 + 索引；增量更新（同一文件重新上传怎么处理）

## 第 6 周：检索生成闭环 + 评估基建 ⭐ 本阶段最重要的一周

- [ ] 相似度检索：top-k、相似度阈值；QuestionAnswerAdvisor 快速起步
- [ ] RetrievalAugmentationAdvisor 与 2.0 四阶段架构（Pre-Retrieval / Retrieval / Post-Retrieval / Augmentation）
- [ ] 返回引用来源（文件名 + 章节），前端可点开
- [ ] **建 golden set**：从自己的文档造 50-100 条标准问答对（含 10% "该拒答"的负例）
- [ ] **评估脚本**（独立可重复执行）：
  - 检索命中率@k：标准答案所在 chunk 是否进了 top-k
  - 答案忠实度：LLM-as-judge，用另一个模型按"是否忠于引用内容"打 1-5 分
  - 输出 Markdown 报告（日期、配置、分数）
- [ ] 跑出第一版基线分数，存 `docs/eval/` 留档

## 第 7 周：检索增强（每改一处 → 跑评估 → 记录数字）

- [ ] 混合检索：向量 + 全文/BM25（PG 的 tsvector 或关键词召回），加权融合
- [ ] Reranker 重排：DashScope rerank API 或本地 cross-encoder；对比加与不加的分数
- [ ] 元数据过滤：SQL 风格 filter（分类、日期）
- [ ] **权限过滤（企业 RAG 真需求）**：用户 → 角色 → 可见文档标签，检索时强制注入 filter；写自动化测试证明 A 用户永远查不到 B 部门文档
- [ ] 查询改写：多轮对话里把"它呢？"改写成完整查询再检索

## 第 8 周：对话记忆 + 调优冲刺

- [ ] ChatMemory：MessageWindowChatMemory、JDBC/Redis 持久化、会话隔离
- [ ] 记忆与检索的配合：带着历史怎么检索才不跑偏
- [ ] 调优实验矩阵（评估脚本跑）：chunk size × embedding 模型 × top-k × 有无 rerank，至少 8 组
- [ ] 产出《评估报告》：哪个变量影响最大、最终配置、遗留问题

## 实战任务：demo-03-knowledge-base

1. 上传文档 → 自动解析入库（异步，带进度）
2. 问答 + 流式输出 + 引用来源
3. 权限过滤（至少两个角色、两批文档）
4. 拒答机制：检索不到相关内容时明确说不知道，不编
5. `docs/eval/` 下有基线与最终对比报告

### 验收标准（DoD）
- golden set ≥ 50 条且含负例；最终配置相对基线有可解释的提升
- 权限隔离有自动化测试
- 同一文件重复上传不产生重复 chunk

## 助教检查点
1. 拿评估报告讲：哪次改动提升最大？哪次是负优化？为什么？
2. 权限过滤为什么必须在检索层做，而不能生成后再过滤？
3. 拒答阈值怎么定的？对企业场景，误拒和误答哪个代价更高？
4. chunk 切太大/太小分别在哪个指标上暴露出来？

## 搜索关键词
Spring AI RAG RetrievalAugmentationAdvisor ｜ Spring AI PgVector ｜ RAG evaluation golden dataset ｜ LLM as judge ｜ hybrid search rerank ｜ Spring AI ChatMemory
