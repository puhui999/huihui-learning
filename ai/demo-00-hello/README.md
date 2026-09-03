# demo-00-hello

阶段 0 产出：Spring Boot 4.0.8 + Spring AI 2.0.1 + JDK 25，第一个 ChatClient 调用。

## 运行

前提：环境变量 `DEEPSEEK_API_KEY` 已配置（见 [w00 环境搭建](../notes/w00-环境搭建.md)）。

```powershell
.\mvnw.cmd spring-boot:run
```

接口测试用 IDEA HTTP Client：打开 `http/hello.http`，右上角环境选 `dev`，点每个请求前的运行箭头。第一个请求带断言，绿色即通过。
不用 IDEA 时：`curl.exe -s http://localhost:8080/ai/hello`。

## 骨架已有

- `application.yml`：DeepSeek OpenAI 兼容配置，Key 只从环境变量读
- `HelloController`：`GET /ai/hello`，注入 `ChatClient.Builder` 调模型
- `HelloApplicationTests`：上下文加载冒烟测试，不依赖环境变量
- `http/hello.http` + `http-client.env.json`：IDEA HTTP Client 请求与环境，私有环境文件 `http-client.private.env.json` 已全局 gitignore

## 找手感（阶段 0 任务 2，自己写）

按平时习惯补一遍，体会 Boot 4 的差异：

- [ ] 一个 `@RestController` 返回带 `LocalDateTime` 字段的 record，看 Jackson 3 默认输出 ISO 字符串
- [ ] 一个 `@Service` 并注入到 Controller
- [ ] 一个 `@ConfigurationProperties` record 绑定自定义配置
- [ ] 一个 `@RestControllerAdvice` 统一异常返回
- [ ] `HelloApplicationTests` 之外，用 `@WebMvcTest` 给自己的 Controller 写一个切片测试
