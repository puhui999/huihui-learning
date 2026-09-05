# w01 裸调 API 与多轮对话（阶段 1 · 第 1 周）

> 开始：2026-09-03 ｜ 目标：[stage-1](../plan/stage-1-api-basics.md) 第 1 周清单全勾，huihui-ai 工程 `chat` 包的多轮对话 + usage 入库跑通 ｜ 预算 ~25h，拆成 5 个工作日 + 周末收尾
> 实操在 Windows 主机，命令按 **PowerShell** 写；接口一律用 IDEA HTTP Client 的 `.http` 文件。
> 第 2 周（SSE 流式 + Prompt 工程）另开 w02，本文末尾只给预告和现在就要定下的设计。

## 决策记录（2026-09-03 核实）

| 决策 | 内容 | 为什么 |
|---|---|---|
| 模型改用 `deepseek-v4-flash` | DeepSeek 2026-04-24 公告：`deepseek-chat` / `deepseek-reasoner` 于 2026-07-24 停用，文档现在只列 V4 三个模型。demo-00 的 `deepseek-chat` 还能通，只说明别名暂未真正下线 | 新代码不写在已宣布停用的别名上；Day 1 先 `GET /models` 看账号实际能用什么 |
| 显式关思考模式 `"thinking": {"type": "disabled"}` | V4 系列**默认开启**思考模式，思考 token 计入 completion 计费，首字延迟明显 | 本阶段学的是非思考对话；Day 2 做一次开思考的对照实验就够 |
| Web MVC + `spring-boot-starter-webclient`，不上 WebFlux 服务端 | Boot 4.0.8 有独立的 webclient starter（只带 reactor-netty-http，不带 Netty 服务器）；MVC 控制器可以直接返回 `Flux<ServerSentEvent>`；DB 用普通 JDBC | 只在流式那一条链路碰 Reactor，其余保持你熟悉的阻塞式写法，不把时间花在响应式上 |
| usage 表放在 pgvector 容器里的 Postgres `ai` 库 | 容器已在跑，不新起 | 阶段 3 的向量表也在这个库，一套连接配置用到底 |
| 成本折算要带时段 | DeepSeek 2026-08-17 起峰谷定价：**工作日北京时间 9:00-12:00、14:00-18:00 为高峰，其余时段半价** | 你晚上学习全在闲时；不带时段算出来的钱和平台账单必然对不上 |
| 供应商切换靠 profile | `application.yml` 默认 DeepSeek，`application-bailian.yml` 覆盖 base-url / key / model / 定价 | 实战任务第 5 条"只改配置不改代码"从 Day 3 的配置设计就开始满足 |
| 所有阶段共用一个工程 `ai/huihui-ai`，按阶段分包 | 2026-09-05 你的决定：demo 太多、每次都要重配环境。demo-00 已并入 `hello` 包，阶段 1 写在 `chat` 包，跨阶段共用的放 `common` | 一个 IDEA 窗口、一份 yml、一个库、一套 Run Configuration |

## 核实过的事实（写代码前先知道）

**DeepSeek**（文档 <https://api-docs.deepseek.com/zh-cn/>，2026-09-03 查）

| 模型 | 上下文 | 最大输出 | 输入·缓存命中 高峰/闲时 | 输入·未命中 | 输出 |
|---|---|---|---|---|---|
| deepseek-v4-flash | 1M | 384K | ¥0.1 / ¥0.05 | ¥3 / ¥1.5 | ¥9 / ¥4.5 |
| deepseek-v4-pro | 1M | 384K | ¥0.3 / ¥0.15 | ¥9 / ¥4.5 | ¥27 / ¥13.5 |

单位：元 / 百万 token。定价页数字以你 Day 1 抄进配置的为准，本表只是核实时的快照。

- 端点 `POST https://api.deepseek.com/chat/completions`，Bearer 鉴权；`/models` 列模型
- `temperature` 默认 1.0，范围 0-2；官方场景建议：代码/数学 0.0，数据抽取 1.0，通用对话 1.3，翻译 1.3，创意写作 1.5
- token 经验值：1 个中文字 ≈ 0.6 token，1 个英文字符 ≈ 0.3 token；以响应里的 `usage` 为准
- `usage` 字段：`prompt_tokens` / `completion_tokens` / `total_tokens` / `prompt_cache_hit_tokens` / `prompt_cache_miss_tokens` / `completion_tokens_details.reasoning_tokens`
- `finish_reason` 取值：`stop` / `length` / `content_filter` / `tool_calls` / `insufficient_system_resource`
- `stream_options.include_usage: true` 时，每个 chunk 都有 `usage` 字段但都是 `null`，只有 `[DONE]` 前最后一个 chunk 带完整用量
- JSON 模式：`response_format: {"type": "json_object"}`，且 prompt 里必须出现 `json` 字样并给样例，否则报错；偶发空内容要重试（第 2 周）
- 错误码：400 请求体格式错 ｜ 401 Key 错 ｜ **402 余额不足** ｜ 422 参数错 ｜ 429 限速 ｜ 500/503 服务端，稍后重试
- `frequency_penalty` / `presence_penalty` 已标记废弃，不要用

**阿里云百炼**（OpenAI 兼容，2026-09-03 查）

- base-url 老域名 `https://dashscope.aliyuncs.com/compatible-mode/v1` 仍可用（新的业务空间专属域名形如 `https://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/compatible-mode/v1`，用哪个都行）
- Key 环境变量约定 `DASHSCOPE_API_KEY`；Key 与地域绑定，北京地域的 Key 只能打北京域名
- `qwen-plus`：128K 上下文，输入 ¥0.8 / 输出 ¥2 每百万 token，缓存命中按输入价 10%，北京地域新用户有 100 万 token 免费额度
- 缓存命中字段是 OpenAI 风格的 `prompt_tokens_details.cached_tokens`，不是 DeepSeek 的 `prompt_cache_hit_tokens`——所以 usage 要**存原始 JSON**，映射两种字段名

**Spring Boot 4.0.8**

- Initializr 依赖 ID：`web`（→ `spring-boot-starter-webmvc`）、`spring-webclient`（→ `spring-boot-starter-webclient`）、`jdbc`、`postgresql`、`validation`
- HTTP 客户端全局超时属性统一为 `spring.http.clients.connect-timeout` / `read-timeout`（3.x 的 `spring.http.reactiveclient.*` 不要再用）
- `spring.threads.virtual.enabled: true`：Tomcat 与应用线程池走虚拟线程，Boot 4 还会顺手把 Reactor 的 boundedElastic 调度器切到虚拟线程
- Jackson 3：`@JsonNaming` 搬到了 `tools.jackson.databind.annotation`，`PropertyNamingStrategies` 在 `tools.jackson.databind`；`@JsonProperty` / `@JsonInclude` 仍在 `com.fasterxml.jackson.annotation`。**未知字段默认不再报错**（3.0 把 `FAIL_ON_UNKNOWN_PROPERTIES` 默认改成 false），所以响应 record 只声明你关心的字段即可

## 阶段 1 在 huihui-ai 里的目标结构（整个阶段；本周只做 ①②③）

```
com.huihui.ai
├── HuihuiAiApplication             已有：@SpringBootApplication + @ConfigurationPropertiesScan
├── hello/                          已有：阶段 0 的 /ai/hello
├── common/                         跨阶段共用
│   ├── web/GlobalExceptionHandler     ① LlmUpstreamException → 502 带上游状态码；参数错 → 400
│   └── usage/                         ③ 用量与成本（阶段 2 起 Spring AI 的调用也往这记）
│       ├── UsageRecord、UsageRepository   JdbcClient 读写 chat_usage 表
│       ├── CostCalculator                 峰谷判断 + 三段单价折算
│       ├── UsageService                   按天 / 按会话汇总
│       └── UsageController (+ .http)      GET /api/usage/daily、/api/usage/sessions/{id}
└── chat/                           阶段 1
    ├── config/
    │   ├── LlmProperties               ① record，prefix app.llm（yml 里这一段已放好，绑定即可）
    │   └── LlmClientConfig             ① WebClient bean：baseUrl + Authorization 头
    ├── llm/                            ① 裸协议层：字段与 OpenAI 兼容接口一一对应，不掺业务
    │   ├── ChatMessage                    record(role, content)，静态工厂 system()/user()/assistant()
    │   ├── ChatCompletionRequest          model、messages、temperature、maxTokens、stream、streamOptions、responseFormat、thinking
    │   ├── ChatCompletionResponse         id、model、created、choices[]（message、finishReason）、usage
    │   ├── ChatCompletionChunk            第 2 周：流式 chunk，choices[].delta.content + 末块 usage
    │   ├── Usage                          见上面字段清单，附 cachedTokens() 统一两家字段名
    │   ├── LlmUpstreamException           上游非 2xx：带 status + 原始 body
    │   ├── OpenAiCompatClient             complete(req)；第 2 周加 stream(req) → Flux<ChatCompletionChunk>
    │   └── OpenAiCompatClient.http        Day 1 手点协议的请求集（放在它要封装的类旁边）
    ├── conversation/                   ② 多轮对话
    │   ├── Conversation                   sessionId、history、每会话一把锁
    │   ├── InMemoryConversationStore      ConcurrentHashMap
    │   ├── ContextWindowPolicy            只保留最近 N 轮（先做）/ 按 token 预算截断（选做）
    │   └── ChatService                    组装 messages → 调 client → 追加历史 → 记 usage
    ├── prompt/                         第 2 周：模板加载与变量替换
    └── web/
        ├── ChatController (+ .http)       POST /api/chat；GET/DELETE /api/sessions/{id}
        └── (第 2 周) 流式端点；打字机页放 resources/static/chat.html
```

## 执行清单（按天）

### Day 1（~3h）：协议先用手点一遍，再生成骨架

**1. 拉代码，确认环境一次配好**（10 分钟）

```powershell
cd huihui-learning
git status                      # demo-00-hello 下若有未提交改动，先 git stash 再 pull
git pull
cd ai\huihui-ai
.\mvnw.cmd spring-boot:run        # /ai/hello 通 = 环境 OK，后面各阶段都不用再配
```

工程里已经有：pom（webmvc、webclient、jdbc、postgresql、validation、Spring AI 全在）、`application.yml`（DeepSeek、数据源、`app.llm` 三段）、`application-bailian.yml`、主类 `HuihuiAiApplication`。IDEA 打开 `ai/huihui-ai`，确认 Project SDK 是 25。
阶段 1 的代码全部写在 `com.huihui.ai.chat` 包和 `common` 包里，不再新建工程。如果这两天已经在本地生成了 demo-01-chat-sse：包名一样是 `com.huihui.ai.chat`，把 `src` 下写好的文件按原路径复制进来即可，然后删掉 demo-01 目录。

**2. 建 `chat/llm/OpenAiCompatClient.http`，手点协议**（1.5h）

IDEA HTTP Client 能直接读系统环境变量：`{{$env.DEEPSEEK_API_KEY}}`，不需要 env 文件（IDEA 启动时快照环境，改过变量要重启 IDEA，和 w00 那个坑同源）。

```http
### 0. 账号能用的模型列表：看 deepseek-chat 还在不在
GET https://api.deepseek.com/models
Authorization: Bearer {{$env.DEEPSEEK_API_KEY}}

### 1. 最小对话：非流式、关思考。逐个字段看响应：choices[0].message、finish_reason、usage
POST https://api.deepseek.com/chat/completions
Authorization: Bearer {{$env.DEEPSEEK_API_KEY}}
Content-Type: application/json

{
  "model": "deepseek-v4-flash",
  "messages": [
    {"role": "system", "content": "你是简洁的 Java 技术助手，回答不超过 3 句话。"},
    {"role": "user", "content": "Spring AI 是什么？"}
  ],
  "temperature": 1.0,
  "max_tokens": 512,
  "thinking": {"type": "disabled"}
}

> {%
    client.test("finish_reason 是 stop", () => client.assert(response.body.choices[0].finish_reason === "stop"));
    client.log(JSON.stringify(response.body.usage));
%}
```

在这个文件里继续加请求，每个都点一遍并把观察写进本文末尾"实验记录"：

| # | 改什么 | 看什么 |
|---|---|---|
| 2 | `temperature: 0`，同一问题连点 3 次；再改 1.3 连点 3 次 | 0 也不保证逐字相同（只是更稳定）；1.3 明显发散 |
| 3 | `max_tokens: 20` | `finish_reason` 变 `length`，内容被截断——工程上要检查这个字段而不是只拿 content |
| 4 | messages 里塞 system + user + assistant + user 四条，assistant 那句是你编的 | 模型把编的话当成自己说过的：**模型无状态，上下文全靠请求体带**，也意味着历史可以被篡改 |
| 5 | 同一个长 system prompt（贴 500 字）连发两次 | 第二次 `prompt_cache_hit_tokens` > 0，命中部分按 1/30 价格计费 |
| 6 | 把 `thinking` 改成 `enabled` | 响应多了 `reasoning_content`，`completion_tokens_details.reasoning_tokens` 不为 0，耗时和费用都涨 |
| 7 | Key 改成假的 / model 写成 `deepseek-chat-x` | 401 与 400 的响应体长什么样，Day 3 的异常映射要原样透出这个 body |

**3. 读文档**（45 分钟，只读这几页）：[Token 用量](https://api-docs.deepseek.com/zh-cn/quick_start/token_usage)、[参数设置](https://api-docs.deepseek.com/zh-cn/quick_start/parameter_settings)、[模型与价格](https://api-docs.deepseek.com/zh-cn/quick_start/pricing)、[上下文硬盘缓存](https://api-docs.deepseek.com/zh-cn/guides/kv_cache)、[创建对话补全](https://api-docs.deepseek.com/zh-cn/api/create-chat-completion)。读完把两家当前单价抄到本文"实验记录"里，Day 3 写配置用。

### Day 2（~3h）：Token 直觉与"概率性输出"的工程含义

继续用 `.http` 做，不写 Java：

1. 分别发 100 个中文字、100 个英文字符、100 位数字，记录 `prompt_tokens`，和官方经验值（中文 0.6、英文 0.3）比一比
2. 把 Day 1 第 4 个请求的历史堆到 10 轮，看 `prompt_tokens` 怎么涨；想一想 1M 上下文对工程意味着什么：费用线性涨、延迟涨、注意力被稀释，所以多轮**必须截断**
3. `temperature` 与 `top_p` 二选一调，不要同时动；写下你打算给"通用助手 / 翻译 / JSON 提取器"三个模板各配多少
4. 概率性输出的工程对策，先形成自己的话：低 temperature 只是降低方差；真正的对策是结构化输出 + 校验 + 重试（第 2 周）、以及评估集用数字说话（阶段 3）

产出：在本文"概念小结"填 4 行（Token 与窗口 / 采样参数 / 概率性输出 / messages 角色），用自己的话写，验收时检查点 1 会照着问。

### Day 3（~4h）：Java 骨架：配置 + WebClient + 非流式单轮

**配置**——`application.yml` 里的 `app.llm` 段和 `application-bailian.yml` 我已经放好（默认 DeepSeek，Key 只从环境变量读；HTTP 客户端超时、虚拟线程、数据源也都配了）。你要做的：读一遍这两个文件，把 Day 1 抄的单价核对进去，然后写 `chat/config/LlmProperties`：`@ConfigurationProperties(prefix = "app.llm")` 的 record，字段 `provider、baseUrl、apiKey、model、thinking、Pricing pricing`，嵌套 `Pricing(cacheHit, cacheMiss, output, offPeakRatio, List<String> peakWindows)`。主类已有 `@ConfigurationPropertiesScan`，不用注册。

切换供应商：`.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=bailian"`，或 IDEA Run Configuration 的 Active profiles 填 `bailian`。

**WebClient bean**——Boot 给的 `WebClient.Builder` 是原型作用域，直接在上面加 baseUrl 和鉴权头：

```java
@Configuration(proxyBeanMethods = false)
class LlmClientConfig {

    @Bean
    WebClient llmWebClient(WebClient.Builder builder, LlmProperties props) {
        return builder.baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                .build();
    }
}
```

**协议 record**——字段名与接口一一对应，用 snake_case 策略省掉一堆 `@JsonProperty`（注意注解不会传给嵌套 record，每个都要标）：

```java
import com.fasterxml.jackson.annotation.JsonInclude;                 // 没变
import tools.jackson.databind.PropertyNamingStrategies;              // Jackson 3 新包名
import tools.jackson.databind.annotation.JsonNaming;                 // Jackson 3 新包名

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatCompletionRequest(
        String model, List<ChatMessage> messages, Double temperature, Integer maxTokens,
        Boolean stream, StreamOptions streamOptions, ResponseFormat responseFormat, Thinking thinking) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record StreamOptions(boolean includeUsage) {}
    public record ResponseFormat(String type) {}
    public record Thinking(String type) {}
}
```

响应侧同样处理：`ChatCompletionResponse(id, model, created, choices, usage)`、`Choice(index, message, finishReason)`、`Usage(...)`。`Usage` 里同时声明 `promptCacheHitTokens`（DeepSeek）和 `promptTokensDetails.cachedTokens`（百炼/OpenAI），加一个 `cachedTokens()` 方法二选一返回。

**调用与错误透出**：

```java
public ChatCompletionResponse complete(ChatCompletionRequest request) {
    return webClient.post().uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> new LlmUpstreamException(resp.statusCode().value(), body)))
            .bodyToMono(ChatCompletionResponse.class)
            .block();   // 在 Tomcat 的（虚拟）请求线程上 block 是合法的；在 reactor-http-nio-* 线程上 block 会直接抛异常
}
```

**Controller 与异常处理**：`POST /api/chat`，请求体 `{"message": "..."}`（先单轮），返回 `ChatReply(sessionId, content, finishReason, usage, latencyMs)`；`common/web/GlobalExceptionHandler` 把 `LlmUpstreamException` 映射成 502，message 里带上游状态码和 body 前 200 字（沿用 huihui-ai README 附录第 5 条那套设计）；`@Valid` 校验 message 非空、长度上限。`ChatController.http` 放同目录：正常路径带断言 + 一条超长消息看 400。

Day 3 完成标准：`.http` 打通，返回里能看到 `usage` 和 `finishReason`；把环境变量改成假 Key 重启，返回 502 且 message 里有 401。

### Day 4（~4h）：多轮对话与上下文控制

1. `Conversation`：`sessionId`、`List<ChatMessage> history`、`ReentrantLock`；`InMemoryConversationStore`：`ConcurrentHashMap`，`getOrCreate(sessionId)`；sessionId 由服务端生成 UUID 并在响应里返回
2. `ChatService.chat(sessionId, userText)`：加锁 → messages = system + `ContextWindowPolicy.apply(history)` + 本轮 user → 调 client → 把 user 和 assistant 两条**追加**进 history → 返回。同一会话两个请求并发时，锁保证历史不交错
3. `ContextWindowPolicy` 先实现"只保留最近 N 轮"（`app.chat.max-rounds: 10`）；每轮打一行 INFO：`session=… turn=… prompt_tokens=… cache_hit=… completion_tokens=…`
4. 接口：`POST /api/chat` 带 `sessionId`（可空）、`GET /api/sessions/{id}/messages`（看内存里到底存了什么）、`DELETE /api/sessions/{id}`

`.http` 里用响应脚本把 sessionId 串起来：

```http
### 第 1 轮：不带 sessionId，服务端生成并返回
POST http://localhost:8080/api/chat
Content-Type: application/json

{"message": "我叫 HUIHUI，是 Java 工程师，记住我的名字"}

> {% client.global.set("sid", response.body.sessionId); %}

### 第 2 轮：带上 sessionId
POST http://localhost:8080/api/chat
Content-Type: application/json

{"sessionId": "{{sid}}", "message": "我叫什么？做什么工作？"}
```

**实验（对应验收检查点 1）**：复制到 12 轮，看日志：`prompt_tokens` 应逐轮上涨；`cache_hit` 也逐轮上涨——因为每轮的前缀（system + 之前的历史）和上一轮完全相同，命中了硬盘缓存。这就是"历史只追加、不改写"的价值。把 `max-rounds` 改成 4 重跑，`prompt_tokens` 应在第 5 轮后趋于平稳。把这两组数字记进"实验记录"。

### Day 5（~4h）：usage 入库与成本面板

`src/main/resources/schema.sql`（`CREATE TABLE IF NOT EXISTS`，可重复执行）：

```sql
CREATE TABLE IF NOT EXISTS chat_usage (
    id                BIGSERIAL PRIMARY KEY,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    provider          VARCHAR(32)    NOT NULL,
    model             VARCHAR(64)    NOT NULL,
    session_id        VARCHAR(64)    NOT NULL,
    request_id        VARCHAR(64),                     -- 响应里的 id，对账时能定位到单次请求
    stream            BOOLEAN        NOT NULL DEFAULT FALSE,
    finish_reason     VARCHAR(32),                     -- stop / length / cancelled(第 2 周) / error
    prompt_tokens     INTEGER        NOT NULL DEFAULT 0,
    cache_hit_tokens  INTEGER        NOT NULL DEFAULT 0,
    completion_tokens INTEGER        NOT NULL DEFAULT 0,
    reasoning_tokens  INTEGER        NOT NULL DEFAULT 0,
    peak              BOOLEAN        NOT NULL DEFAULT FALSE,
    cost_cny          NUMERIC(12, 6) NOT NULL DEFAULT 0,
    latency_ms        INTEGER,
    raw_usage         JSONB                            -- 原始 usage，字段名两家不同，别丢
);
CREATE INDEX IF NOT EXISTS idx_chat_usage_created_at ON chat_usage (created_at);
CREATE INDEX IF NOT EXISTS idx_chat_usage_session ON chat_usage (session_id);
```

`UsageRepository` 用 Boot 自动装配的 `JdbcClient`（Spring 6.1 起的流式 API，record 可直接当参数源和结果映射）：

```java
jdbcClient.sql("""
        INSERT INTO chat_usage (provider, model, session_id, request_id, stream, finish_reason,
            prompt_tokens, cache_hit_tokens, completion_tokens, reasoning_tokens, peak, cost_cny, latency_ms, raw_usage)
        VALUES (:provider, :model, :sessionId, :requestId, :stream, :finishReason,
            :promptTokens, :cacheHitTokens, :completionTokens, :reasoningTokens, :peak, :costCny, :latencyMs, CAST(:rawUsage AS jsonb))
        """)
        .paramSource(record)
        .update();
```

按天汇总用 `(created_at AT TIME ZONE 'Asia/Shanghai')::date` 分组，`.query(DailyUsage.class).list()` 自动把 snake_case 列映射到 record。

`CostCalculator`：`miss = prompt_tokens - cache_hit`；`cost = (hit × 命中价 + miss × 未命中价 + completion × 输出价) / 1_000_000`，闲时再乘 `off-peak-ratio`。峰谷判断：请求时刻转成 `Asia/Shanghai`，周六日直接闲时，否则落在任一 `peak-windows` 内为高峰。`BigDecimal` 保留 6 位。

接口：`GET /api/usage/daily?days=7`、`GET /api/usage/sessions/{id}`，加 `UsageController.http`。

**对账**（DoD 第 2 条）：DeepSeek 开放平台"用量"页按天看 tokens，与 `GET /api/usage/daily` 比。差异来源先列在纸上再去查：平台按 UTC 还是北京时间分天；4xx 失败请求不计费（本地不应落表，或落表但 tokens 为 0）；思考 token 计在 completion 里；第 2 周还会加上"流式中途取消"这一项。差异说得清就算过。

### Day 6-7（~4h）：测试、README、复盘

- 不依赖 Key 的测试：`ContextWindowPolicyTest`、`CostCalculatorTest`（高峰边界 9:00 / 12:00 / 周六）、`ChatControllerTest`（`@WebMvcTest(ChatController.class)` + `@MockitoBean ChatService` + `MockMvcTester`，写法见 huihui-ai README 附录第 6 条）。真调 API 的测试可选，用 `@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")` 标记，没 Key 自动跳过
- 冒烟测试已把 `spring.sql.init.mode` 设为 never，不依赖容器；真连库的 Repository 测试需要容器在跑，README 里写明
- README：在"阶段 → 包 → 入口"表里把阶段 1 的接口补全，需要的话补启动说明
- 本文"踩坑记录"补齐，然后说"周复盘"，我出小测
- 提交：`git add ai/huihui-ai ai/notes; git commit -m "feat(ai): 阶段 1 多轮对话与 usage 入库"; git push`

## 第 2 周预告（现在只需知道，w02 再展开）

- 流式：请求加 `stream: true` + `stream_options.include_usage: true`；WebClient 用 `bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})` 拿到每条 `data:`，`takeWhile` 到 `[DONE]` 为止再手工反序列化——**不要**直接用 `ServerSentEvent<ChatCompletionChunk>`，`[DONE]` 不是 JSON 会炸
- MVC 控制器直接返回 `Flux<ServerSentEvent<String>>`；要设 `spring.mvc.async.request-timeout`（Tomcat 异步默认 30 秒，长回答会被切断）
- 断线：客户端断开 → 下一次写失败 → Spring 取消订阅 → WebClient 取消上游请求；用 `doOnCancel` 打日志观测，这是 DoD 第 1 条
- 前端用 `fetch` + `ReadableStream` + `AbortController`，不用 `EventSource`（它会自动重连，服务端一关流它就再发一次请求，重复扣费）
- Prompt 模板存 yml，`{{var}}` 替换；JSON 模式 + 解析失败重试；注入实验；`--spring.profiles.active=bailian` 跑通同一套代码

## 踩坑预警

- V4 默认开思考：忘了 `thinking: disabled` 会看到首字等好几秒、`completion_tokens` 暴涨，钱花在你看不见的 `reasoning_content` 上
- 402 是余额不足，不是鉴权问题；429 先看 message 里是 RPM 还是 TPM
- `{{$env.X}}` 读的是 IDEA 启动时的环境快照，`setx` 之后要重启 IDEA
- 千万别把 `Authorization` 头打进日志；WebClient 的 `ExchangeFilterFunction` 日志过滤器要脱敏
- `.block()` 只能在 Tomcat 请求线程上调；在 Reactor 线程上会抛 `block()/blockFirst()/blockLast() are blocking, which is not supported in thread reactor-http-nio-*`
- `@JsonNaming` 不会应用到嵌套 record，`include_usage` 漏了下划线上游会静默忽略这个参数（不报错，就是不给 usage）
- 峰谷定价：同一请求"高峰 / 闲时"由请求时刻决定，跨 12:00 边界的那一分钟就当高峰算，误差可解释即可

## 概念小结（Day 2 自己填）

| 概念 | 我的理解（用自己的话） | 工程上怎么对待 |
|---|---|---|
| Token 与上下文窗口 | | |
| temperature / top_p / max_tokens | | |
| 概率性输出 | | |
| system / user / assistant 分工 | | |

## 实验记录（做的过程中随手记）

- 两家当前单价（Day 1 抄）：
- `GET /models` 返回的模型名：
- temperature 0 三连 vs 1.3 三连：
- 中文 / 英文 / 数字各 100 字符的 prompt_tokens：
- 12 轮对话 prompt_tokens 与 cache_hit 曲线（max-rounds=10 vs 4）：
- 对账：本地按天 tokens vs 平台，差异与原因：

## DoD 自检（第 1 周，全过后进入 w02）

- [ ] 同一 session 连打 10 轮，日志里 `prompt_tokens` 逐轮上涨，开截断后趋稳
- [ ] `GET /api/usage/daily` 当天 tokens 与 DeepSeek 平台用量一致，或差异能说清
- [ ] `.\mvnw.cmd test` 全绿且不需要 Key
- [ ] `git grep -nE 'sk-[A-Za-z0-9]{20,}'` 无输出
- [ ] 申请到 `DASHSCOPE_API_KEY` 后用 bailian profile 跑通一次（可放到第 2 周）

## 踩坑记录（现象 → 排查 → 根因 → 解决）

_做的过程中随手记，验收时一起 review。_
