package com.huihui.ai.hello;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// api-key 用占位值覆盖，让测试不依赖环境变量；contextLoads 不会真的调用模型
@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
class HelloApplicationTests {

	@Test
	void contextLoads() {
	}

}
