package com.example.dqcadirsystem.common.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪花 ID 生成器的基础行为测试。
 */
class SnowflakeLongIdGeneratorTest {

    /** 连续生成的 ID 应全部为正数且互不重复。 */
    @Test
    void shouldGenerateUniquePositiveIds() {
        SnowflakeLongIdGenerator generator = new SnowflakeLongIdGenerator(0);
        Set<Long> ids = new HashSet<>();

        for (int index = 0; index < 10_000; index++) {
            long id = generator.nextId();
            assertTrue(id > 0);
            ids.add(id);
        }

        assertEquals(10_000, ids.size());
    }

    /** 节点号超出 10 位可表示范围时应在应用启动阶段立即失败。 */
    @Test
    void shouldRejectInvalidWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeLongIdGenerator(1024));
    }
}
