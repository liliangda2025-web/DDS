package com.example.dqcadirsystem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用上下文冒烟测试：验证 Spring Boot 的基础配置可以正常装配。
 *
 * <p>测试使用 H2 内存数据库创建完整的数据源和 MyBatis 上下文，但不执行任何业务 SQL。
 * 这样既不依赖开发机 MySQL，也能验证 Mapper、Service 和 Controller 可以完整装配。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context_load;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=never"
})
class DqcadirSystemApplicationTests {

    /** 只要 Spring 上下文成功启动，本测试即通过。 */
    @Test
    void contextLoads() {
    }

}
