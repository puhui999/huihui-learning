# huihui-ai

AI track 的**唯一** Java 工程：Spring Boot 4.0.8 + Spring AI 2.0.1 + JDK 25。所有阶段的代码都在这里按包分开，环境只配一次（2026-09-05 由 demo-00-hello 合并而来）。

## 运行

前提：环境变量 `DEEPSEEK_API_KEY` 已配置；PgVector 容器在跑（`docker start pgvector`）。都见 [w00 环境搭建](../notes/w00-环境搭建.md)。

```powershell
.\mvnw.cmd spring-boot:run                                          # 默认 DeepSeek
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=bailian"     # 切到阿里云百炼，需要 DASHSCOPE_API_KEY
.\mvnw.cmd test                                                     # 冒烟测试不依赖 Key 和数据库
```

IDEA 里切供应商：Run Configuration 的 Active profiles 填 `bailian`。

## 阶段 → 包 → 入口

| 阶段 | 包 | 接口前缀 | 页面 | 状态 |
|---|---|---|---|---|
| 0 | `hello` | `/ai/hello` | — | ✅ |
| 1 | `chat`（+ `common/usage`） | `/api/chat`、`/api/sessions`、`/api/usage` | `static/chat.html` | 🔵 |
| 2 | `tools` | `/api/tools` | | ⬜ |
| 3 | `rag` | `/api/rag` | | ⬜ |
| 4 | `agent` | `/api/agent` | | ⬜ |
| 5 | `workflow` | `/api/workflow` | | ⬜ |

## 约定

- **包结构**：`com.huihui.ai.<阶段包>`；跨阶段共用的放 `common/`（异常处理、usage 记账等）。每个阶段用自己的接口前缀，不撞路径
- **接口测试**：`.http` 与 Controller 同目录同名；裸调上游协议的请求集放在它封装的类旁边（如 `chat/llm/OpenAiCompatClient.http`）。Key 用 `{{$env.DEEPSEEK_API_KEY}}` 直接读系统环境变量，改过变量要重启 IDEA
- **配置**：`application.yml` 只有一份；供应商差异放 `application-<provider>.yml`；Key 只走环境变量，绝不入库
- **依赖与表**：阶段用到新依赖时往 pom 里加，不新建工程；`schema.sql` 用 `CREATE TABLE IF NOT EXISTS` 累加
- **作品集**：阶段 6 再从这里抽旗舰 / 副项目成独立仓库，这里保留开发版

## 附录：Boot 4 分层写法速查（原 demo-00 找手感任务，已搁置，需要时查）

目标曾是把 `/ai/hello` 按平时分层习惯重写成 `/ai/chat`，顺便把 Boot 4 / Jackson 3 / 测试切片的差异各踩一次。现在只当速查表用。

**1. `ChatProperties`：配置绑定**
- `@ConfigurationProperties(prefix = "app.chat")` 的 record；默认值用 `@DefaultValue`（`org.springframework.boot.context.properties.bind.DefaultValue`）
- 主类已加 `@ConfigurationPropertiesScan`，record 不用再注册
- 体会：record 天然不可变，不需要 setter

**2. `ChatAnswer`：返回值 record**
- 含 `LocalDateTime` 字段先不加注解跑一次，看默认输出 ISO 字符串；再加 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 对比
- 体会：Jackson 3 的注解仍在 `com.fasterxml.jackson.annotation` 包，没变

**3. `ChatService`：业务层**
- 注入 `ChatClient.Builder`，构造器里 `builder.defaultSystem(...).build()`
- 注入 `tools.jackson.databind.json.JsonMapper`（Boot 4 自动装配好的 bean）把对象转 JSON 打日志
- 体会：Jackson 3 的核心包名从 `com.fasterxml.jackson.databind` 变成了 `tools.jackson.databind`

**4. `Controller`**：只做参数接收和调用 service，不写逻辑

**5. `GlobalExceptionHandler`：`@RestControllerAdvice`**
- 自定义业务异常 → 400，body 用 `ErrorResponse` record：`code, message, LocalDateTime timestamp`
- `com.openai.errors.OpenAIServiceException` → 502，message 带上 `statusCode()`，Key 错误时能直接看到 401
- 其他 `Exception` → 500

**6. `@WebMvcTest` 切片测试**
- Boot 4 新位置：`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`，用法 `@WebMvcTest(XxxController.class)`
- mock 依赖用 `@MockitoBean`（`org.springframework.test.context.bean.override.mockito.MockitoBean`），旧的 `@MockBean` 已删除
- 注入 `MockMvcTester`（`org.springframework.test.web.servlet.assertj.MockMvcTester`），AssertJ 风格：

  ```java
  assertThat(mvc.get().uri("/ai/chat").param("q", "hi"))
          .hasStatusOk()
          .bodyJson().extractingPath("$.answer").isEqualTo("mocked");
  ```
- 体会：切片测试只加载 MVC 层，不会触发 OpenAI 自动装配，所以不需要 Key
