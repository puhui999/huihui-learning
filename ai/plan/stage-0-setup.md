# 阶段 0：环境与 Spring Boot 4 前置（第 0 周，~12h）

> 一句话目标：所有基础设施打通，第一个 ChatClient 调用跑起来。
> 这是原计划完全遗漏的一步：**Spring AI 2.0 硬依赖 Spring Boot 4.0 + Spring Framework 7**。如果你日常在 Boot 2.7/3.x（yudao 那套），先补差异再开工，否则第 1 周会浪费在依赖报错上。

## 任务清单

### 1. 基础环境（~3h）
- [x] JDK 25 安装并设好 JAVA_HOME，IDEA 确认支持 Boot 4（Temurin 25.0.4.1）
- [x] Maven 直连已能拉到 `org.springframework.ai` 坐标，未配阿里云镜像，慢了再配
- [x] Docker Desktop 可用

### 2. Spring Boot 4 差异速览（~4h，只看跟自己相关的）
- [ ] Framework 7 + Boot 4 基线要求（JDK 17+，兼容到 26，我们直接用 25）
- [ ] 对照官方 migration guide 扫一遍配置属性与自动装配的主要变化（不用背）
- [ ] Spring Initializr 生成 Boot 4 空项目，把熟悉的 Controller/Service/配置写一遍找手感

### 3. 模型与中间件（~3h）
- [x] 注册 DeepSeek 开放平台或阿里云百炼，API Key 配成环境变量（**绝不写进会提交的文件**）
- 不自建模型服务（2026-09-02 决定），Ollama 从计划移除，全程走 API
- [ ] Docker 跑通 PgVector（`pgvector/pgvector:pg17` 镜像 + 数据卷），客户端能连并 `CREATE EXTENSION vector`

### 4. 第一个调用（~2h）
- [x] 新建 Boot 4 项目，引入 Spring AI BOM（**≥ 2.0.1**）+ OpenAI 兼容 starter（demo-00-hello）
- [x] application.yml 把 base-url 指向 DeepSeek/百炼，注入 ChatClient，写一个 GET 接口返回模型回答（2026-09-03 跑通）

## 验收标准（DoD）
- `curl localhost:8080/ai/hello` 返回模型生成内容
- PgVector 容器重启后数据仍在
- API Key 不出现在任何会提交的文件里

## 踩坑预警
- Spring AI 2.0.0 有 CVE-2026-59318（提示注入可触发未披露工具调用），版本必须 ≥ 2.0.1
- Boot 3.x 项目直接加 Spring AI 2.0 依赖会失败——不是网络问题，是硬依赖 Boot 4
- 国内拉 Maven/镜像慢，先配好镜像源再开始，不要边下边学

## 搜索关键词
Spring Boot 4 migration guide ｜ Spring AI 2.0 getting started ｜ pgvector docker
