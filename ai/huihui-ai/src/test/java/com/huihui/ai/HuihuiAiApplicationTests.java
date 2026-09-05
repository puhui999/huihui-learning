package com.huihui.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 冒烟测试不依赖环境：api-key 用占位值（不会真的调模型），跳过 schema.sql 初始化（不需要数据库在跑）
@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "spring.sql.init.mode=never"
})
class HuihuiAiApplicationTests {

    @Test
    void contextLoads() {
    }

}
