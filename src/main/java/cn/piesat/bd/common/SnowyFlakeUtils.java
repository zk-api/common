package cn.piesat.bd.common;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 雪花算法生成全局唯一ID工具类
 *
 * @author zk
 * @date 2020/9/15 10:50
 * 1. 64bit位组成
 * 2. 0 + 41位时间戳 + 10位机器ID + 12位序列号
 * 支持 (2 ^ 9) -1 = 511台机器
 */
public class SnowyFlakeUtils {
    /**
     * 上线时间(毫秒)
     */
    private final static long ONLINE_TIME = 1603263692618L;
    /**
     * 最后一次获取时间
     */
    private static long lastTime = 0L;
    /**
     * 序列号
     */
    private static long sequence;

    /**
     * 机器码长度
     */
    private final static long PC_LENGTH = 10L;
    /**
     * 序列号长度
     */
    private final static long SEQUENCE_LENGTH = 12L;
    /**
     * sequence最大值 4095
     */
    private final static long SEQUENCE_MAX_NUM = (1L << SEQUENCE_LENGTH) - 1;

    /**
     * 获取全局唯一ID
     *
     * @param pcId 机器码
     * @return 全局唯一ID
     */
    public synchronized static long getGlobalId(long pcId) {
        if (pcId >= ((1 << PC_LENGTH) - 1)) {
            throw new RuntimeException("最大支持1023机器码");
        }
        //防止同一毫秒并发超过序列号超过最大值
        if (sequence == SEQUENCE_MAX_NUM) {
            try {
                TimeUnit.MILLISECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long currentTimeMillis = System.currentTimeMillis();

        //时钟回拨处理，只处理时钟相差较少的情况
        if (currentTimeMillis < lastTime) {
            long offset = lastTime - currentTimeMillis;
            //相差5毫秒内处理
            if (offset <= 5) {
                try {
                    //等待两倍时间
                    TimeUnit.MILLISECONDS.sleep(offset << 1);
                    currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis < lastTime) {
                        throw new RuntimeException("时间差校正失败");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            } else {
                throw new RuntimeException("时间差距过大");
            }
        }
        //同一毫秒请求，序列号+1
        if (lastTime == currentTimeMillis) {
            sequence++;
        } else {
            //防止并发量少时，id全为偶数问题
            sequence = currentTimeMillis & 1L;
        }
        lastTime = currentTimeMillis;
        //时间戳
        long time = lastTime - ONLINE_TIME;
        long pcOffset = SEQUENCE_LENGTH;
        long timeOffset = SEQUENCE_LENGTH + PC_LENGTH;
        return (time << timeOffset) | (pcId << pcOffset) | sequence;
    }

    public static void main(String[] args) {
        System.out.println(Instant.now().toEpochMilli());
    }
}
