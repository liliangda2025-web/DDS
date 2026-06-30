package com.example.dqcadirsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用上下文冒烟测试：验证 Spring Boot 的基础配置可以正常装配。
 *
 * <p>当前项目尚未提供真实数据库连接信息，因此仅在本测试中排除数据源自动配置。
 * 这不会影响生产环境；以后补齐测试数据库或专用测试配置后，可以移除该排除项。</p>
 */
@SpringBootTest(properties =
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration")
class DqcadirSystemApplicationTests {

    /** 只要 Spring 上下文成功启动，本测试即通过。 */
    @Test
    void contextLoads() {
    }

}
