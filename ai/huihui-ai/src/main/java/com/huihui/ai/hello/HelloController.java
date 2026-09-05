package com.huihui.ai.hello;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 阶段 0：第一个 ChatClient 调用。
 * <p>
 * ChatClient.Builder 由 Spring AI 自动装配（原型作用域，每个注入点拿到独立实例），
 * 这里在构造器里 build 出一个 ChatClient 复用。
 *
 * @author HUIHUI
 */
@RestController
public class HelloController {

    private final ChatClient chatClient;

    public HelloController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/ai/hello")
    public String hello(@RequestParam(defaultValue = "用一句话介绍 Spring AI") String q) {
        return chatClient.prompt().user(q).call().content();
    }

}
