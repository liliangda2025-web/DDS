package com.example.dqcadirsystem.common.id;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 基于时间戳、节点号和毫秒内序列号生成 BIGINT 主键。
 *
 * <p>ID 的位布局为：41 位时间差、10 位节点号、12 位序列号。单个节点每毫秒最多生成 4096 个 ID，
 * 节点号范围为 0～1023。多实例部署时，每个实例必须配置不同的 {@code app.id-generator.worker-id}，
 * 否则不同实例可能生成相同 ID。</p>
 *
 * <p>方法使用 {@code synchronized} 保护同一 JVM 内的时间戳和序列号状态。检测到系统时钟回拨时立即失败，
 * 避免静默生成重复主键；该异常会由全局异常处理器记录完整堆栈并返回系统错误。</p>
 */
@Component
public class SnowflakeLongIdGenerator implements LongIdGenerator {

    /** 自定义纪元：2025-01-01 00:00:00 UTC，使当前生成的 ID 保持为正数。 */
    private static final long EPOCH_MILLIS = 1_735_689_600_000L;

    private static final int SEQUENCE_BITS = 12;
    private static final int WORKER_ID_BITS = 10;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 当前应用实例的节点号。 */
    private final long workerId;

    /** 上一次生成 ID 时使用的毫秒时间戳。 */
    private long lastTimestamp = -1L;

    /** 同一毫秒内递增的序列号。 */
    private long sequence;

    public SnowflakeLongIdGenerator(@Value("${app.id-generator.worker-id:0}") long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("雪花ID节点号必须在0到" + MAX_WORKER_ID + "之间");
        }
        this.workerId = workerId;
    }

    /**
     * 生成下一个雪花 ID。
     *
     * <p>同一毫秒序列耗尽时会主动等待到下一毫秒。等待只会发生在单节点一毫秒超过 4096 次调用的极端情况下。</p>
     */
    @Override
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("系统时钟发生回拨，暂时无法生成唯一ID");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitUntilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;
        return ((timestamp - EPOCH_MILLIS) << TIMESTAMP_SHIFT)
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }

    /** 等待系统时间进入下一毫秒，保证序列号归零后仍不会与上一毫秒的 ID 重复。 */
    private long waitUntilNextMillis(long previousTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= previousTimestamp) {
            Thread.onSpinWait();
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
