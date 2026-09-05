package com.huihui.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AI track 唯一工程的入口。所有阶段的代码按包分开：hello / chat / tools / rag / agent / workflow，跨阶段共用的放 common。
 * <p>
 * 主类放在基础包 com.huihui.ai，组件扫描自然覆盖所有阶段的子包；
 * {@link ConfigurationPropertiesScan} 让各阶段的 @ConfigurationProperties record 免注册。
 *
 * @author HUIHUI
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class HuihuiAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HuihuiAiApplication.class, args);
    }

}
