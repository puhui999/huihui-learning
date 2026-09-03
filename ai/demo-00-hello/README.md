# demo-00-hello

阶段 0 产出：Spring Boot 4.0.8 + Spring AI 2.0.1 + JDK 25，第一个 ChatClient 调用。

## 运行

前提：环境变量 `DEEPSEEK_API_KEY` 已配置（见 [w00 环境搭建](../notes/w00-环境搭建.md)）。

```powershell
.\mvnw.cmd spring-boot:run
```

接口测试用 IDEA HTTP Client：打开 `HelloController.java` 旁边的 `HelloController.http`，点每个请求前的运行箭头。第一个请求带断言，绿色即通过。
不用 IDEA 时：`curl.exe -s http://localhost:8080/ai/hello`。

## 骨架已有

- `application.yml`：DeepSeek OpenAI 兼容配置，Key 只从环境变量读
- `HelloController`：`GET /ai/hello`，注入 `ChatClient.Builder` 调模型
- `HelloApplicationTests`：上下文加载冒烟测试，不依赖环境变量
- `HelloController.http`：IDEA HTTP Client 请求文件，与 Controller 同目录；需要 token 的请求以后放 `http-client.private.env.json`，已全局 gitignore

## 找手感：把 /ai/hello 升级成分层的 /ai/chat（阶段 0 任务 2）

目标：不加新业务，只把现有调用按平时的分层习惯重写一遍，顺便把 Boot 4 / Jackson 3 / 测试切片的差异各踩一次。`/ai/hello` 保留，新增 `/ai/chat`。
所有类放在 `com.huihui.ai.hello` 包下，想分 `controller` / `service` / `config` 子包也行。

**1. `ChatProperties`：配置绑定**
- `@ConfigurationProperties(prefix = "app.chat")` 的 record，两个字段：`String systemPrompt`、`int maxQuestionLength`
- 默认值用 `@DefaultValue`（`org.springframework.boot.context.properties.bind.DefaultValue`）：系统提示词"你是简洁的 Java 技术助手，回答不超过 3 句话"，最大长度 200
- 主类加 `@ConfigurationPropertiesScan` 启用；application.yml 加 `app.chat.*` 覆盖一下试试
- 体会：record 天然不可变，不需要 setter

**2. `ChatAnswer`：返回值 record**
- 字段：`question, answer, model, LocalDateTime answeredAt, long elapsedMs`
- 先不加注解跑一次，看 `answeredAt` 默认输出 ISO 字符串；再给它加 `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 对比
- 体会：Jackson 3 的注解仍在 `com.fasterxml.jackson.annotation` 包，没变

**3. `ChatService`：业务层**
- 注入 `ChatClient.Builder` 和 `ChatProperties`，构造器里 `builder.defaultSystem(props.systemPrompt()).build()`
- `answer(String question)`：超过 `maxQuestionLength` 抛自定义 `QuestionTooLongException`（RuntimeException）；计时；组装 `ChatAnswer`
- 注入 `tools.jackson.databind.json.JsonMapper`（Boot 4 自动装配好的 bean），把 `ChatAnswer` 转成 JSON 打一行 INFO 日志
- 体会：这一步就是为了让你亲眼看到 Jackson 3 的核心包名从 `com.fasterxml.jackson.databind` 变成了 `tools.jackson.databind`

**4. `ChatController`**
- `GET /ai/chat?q=`，只做参数接收和调用 service，不写逻辑

**5. `GlobalExceptionHandler`：`@RestControllerAdvice`**
- `QuestionTooLongException` → 400，body 用 `ErrorResponse` record：`code, message, LocalDateTime timestamp`
- `com.openai.errors.OpenAIServiceException` → 502，message 带上 `statusCode()`。这样 Key 错误时不再是裸 500，能直接看到 401
- 其他 `Exception` → 500
- 体会：把前面踩过的"500 看不出原因"这个坑堵上

**6. `ChatControllerTest`：`@WebMvcTest` 切片测试**
- Boot 4 新位置：`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`，用法 `@WebMvcTest(ChatController.class)`
- mock 依赖用 `@MockitoBean ChatService`（`org.springframework.test.context.bean.override.mockito.MockitoBean`），旧的 `@MockBean` 已删除
- 注入 `MockMvcTester`（`org.springframework.test.web.servlet.assertj.MockMvcTester`），AssertJ 风格：

  ```java
  assertThat(mvc.get().uri("/ai/chat").param("q", "hi"))
          .hasStatusOk()
          .bodyJson().extractingPath("$.answer").isEqualTo("mocked");
  ```
- 两个用例：正常返回 200 且 `$.answer` 是 mock 值；让 mock 抛 `QuestionTooLongException`，断言 400 且 `$.code` 正确，证明 advice 生效
- 体会：切片测试只加载 MVC 层，不会触发 OpenAI 自动装配，所以不需要 Key

**验收（写完自己对一遍）**
- [ ] `GET /ai/chat?q=Spring AI 是什么` 返回 JSON，`answeredAt` 是 `2026-09-03 21:00:00` 这种格式
- [ ] q 超过 200 字返回 400，body 有 `code`
- [ ] 环境变量改成假 Key 重启，返回 502 且 message 里有 401
- [ ] `.\mvnw.cmd test` 全绿：HelloApplicationTests + ChatControllerTest
- [ ] `ChatController.http` 补上以上三种请求，正常路径带断言
