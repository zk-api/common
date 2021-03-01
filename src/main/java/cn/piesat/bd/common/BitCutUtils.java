package cn.piesat.bd.common;

/**
 * 截取bit示例
 *
 * @author zk
 * @date 2020/9/3 16:50
 * @version 1.0
 */
public class BitCutUtils {

    private BitCutUtils() {}
    //int 二进制位数
    private final static int INT_SIZE = 32;
    //byte 二进制位数
    private final static int BYTE_SIZE = 8;

    /**
     * 字节截取,最大支持截取int(32位)
     *
     * @param bytes 待截取的byte数组
     * @param start 截取开始位置,从 0 开始计数 (包含开始位置)
     * @param end   截取结束位置 (包含结束位置)
     * @return 截取后的数值
     */
    public static int bitCutout(byte[] bytes, int start, int end) {
        if (start > end) {
            throw new RuntimeException("开始位置不能大于结束位置");
        }
        if (end - start > INT_SIZE) {
            throw new RuntimeException("长度超过32位，不支持的截取长度");
        }
        //当前数据二进制总长度
        int binaryLength = bytes.length * BYTE_SIZE;
        if (start >= binaryLength || end >= binaryLength) {
            throw new RuntimeException("截取长度超过数据长度");
        }
        return doBitCutout(bytes, start, end, 0);
    }

    /**
     * 实际截取方法, ,最大支持截取int(32位)
     * |                                      |
     * 例: 10010|100_11100000_11110101_10101000_0000001|1
     * |                                      |
     * ↑                                    ↑
     * start=5                               end=38
     * 最终需要的数据为 100_11100000_11110101_10101000_0000001
     * byteIndex: end游标在byte[4]中, 即 00000011
     * bitIndex: end游标在当前byte(000000[1]1)中的位置为 6
     * needLength: 每个字节占8位,需要补位为8 - 6,因为从0开始计数,因此需要再减1,即 8-6-1
     * validBitSize: 由于从0开始计数,因此有效长度为6(bitIndex)+1
     * tempEnd: 当前byte并非所有位都有用,要把有效数据移动到当前byte的最低位,所以需要通过位移截取,
     * 需要补充的长度(needLength)即为需要右移的位数,把当前byte(00000011)变为(00000001),再通过"&"运算
     * 保证右移补充的高位为0
     * endCursor移动一次end游标,判断是否到达最顶端,查找到最顶端时分为两种情况
     * 1. end索引所在byte和start索引所在byte为相同byte,此时需要重新计算tempEnd,由于上面计算的tempEnd
     * 并未考虑是否全部有效,如本例中到达顶端时end游标在byte[1]中的索引为6,即 100101[0]0,游标之前的数据
     * 都作为tempEnd参与了计算,即1001010,因为start的游标在10010[1]00,因此实际有效位为10,所以需要重新计算tempEnd
     * 2.end索引所在byte和start索引所在byte为不同byte,此时tempEnd可以延用,只需根据start游标计算start所在字节
     * 的数据值
     * <p>
     * 当需要补充字节长度(needLength)为0时,需要特殊处理,因为不需要补位时,tempEnd就是实际值;
     * 当补充字节长度(needLength)不为0时,根据end所在字节的上一字节,计算tempStart的数值
     * 低位有效字节长度(validBitSize),即为tempStart左移的长度,如000000[1]1,有效位0000001,
     * 上一相邻字节(10101000)取出1位(1010100[0])补齐,左移低位的有效字节长度7,即为真实大小
     * <p>
     * 将低位数值(tempEnd) + 高位数值(tempStart) = 当前补充完整字节的数值
     * 补齐字节的实际数值需要在根据不同位,计算出真实值,递归1次,相当于高1位byte
     * 如 第一次补齐的0_0000001,不需要左移,第二次补齐的1_1010100,需要左移8位,以此类推
     * <p>
     * 最后将实际数值相加递归,得到最终值
     *
     * @param bytes          待截取的byte数组
     * @param start          截取开始位置,从 0 开始计数 (包含开始位置)
     * @param end            截取结束位置 (包含结束位置)
     * @param recursionLevel 记录递归层级,用于计算实际数值
     * @return 截取后的数值
     */
    private static int doBitCutout(byte[] bytes, int start, int end, int recursionLevel) {
        //定位end 所在byte[] 位置索引
        int byteIndex = end / BYTE_SIZE;
        //定位end 二进制位所在byte 位置索引
        int bitIndex = end % BYTE_SIZE;
        //取出end 所在的byte值
        byte endByte = bytes[byteIndex];
        //需要补充的字节长度
        int needLength = BYTE_SIZE - bitIndex - 1;
        //等待补齐的临时数据
        int tempEnd;
        //待补齐临时数据
        int tempStart;
        //当前字节低位有效字节长度
        int validBitSize = bitIndex + 1;
        //位移后待补充数据
        tempEnd = (endByte >>> needLength) & ((1 << validBitSize) - 1);
        //end 移动一次游标位置
        int endCursor = end - BYTE_SIZE;
        //如果end 移动后游标小于或等于 start时，从start开始到本字节结束,并且结束计算
        if (endCursor + 1 <= start) {
            //定位start所在byte[]位置索引
            int startByteIndex = start / BYTE_SIZE;
            //定位start 二进制位所在byte 位置索引
            int startBitIndex = start % BYTE_SIZE;
            //最后一个截取的长度
            int lastSize;
            //如果start所在byte[]位置索引和end所在byte[]位置索引相同，证明star和end在同一byte[]
            if (startByteIndex == byteIndex) {
                //最后数据的长度 + 1 作 “&” 运算
                lastSize = end - start + 1;
                //当start和end位置索引在同一bytes[]时,需要重新计算tempEnd
                tempEnd = (endByte >> needLength) & ((1 << lastSize) - 1);
                //当start和end位置索引在同一bytes[]时.计算完tempEnd,不需要再计算tempStart
                tempStart = 0;
            } else {
                //要截取的顶端byte
                byte top = bytes[startByteIndex];
                //最后数据在当前byte的长度
                lastSize = BYTE_SIZE - startBitIndex;
                //start游标所在byte的数据值
                tempStart = (top & ((1 << lastSize) - 1)) << validBitSize;
            }
            //返回最后一次查找的数据
            int lastNum = tempStart + tempEnd;
            //计算真实数据值
            int realNum = lastNum << (BYTE_SIZE * recursionLevel);
            return realNum;
        }
        if (needLength != 0) {
            //上一个相邻byte值
            byte beforeByte = bytes[byteIndex - 1];
            //需要补充的长度即为当前字节有效长度
            //等待补齐的临时数据,高位字节需要左移低位的有效长度
            tempStart = (beforeByte & ((1 << needLength) - 1)) << validBitSize;
        } else {
            tempStart = 0;
        }
        //一次查找后的数据
        int onceNum = tempStart + tempEnd;
        //计算真实数据值
        int realNum = onceNum << (BYTE_SIZE * recursionLevel);
        recursionLevel++;
        //递归查找的其他数据并相加
        return realNum + doBitCutout(bytes, start, endCursor, recursionLevel++);
    }

    /**
     * 测试
     * @param args
     */
    public static void main(String[] args) {
        int i = 0b10010100_11100000_11110101_10101000;
        //10010100_11100000_11110101_10101000_00000011
        byte[] bytes = new byte[5];
        bytes[4] = 3;
        bytes[3] = (byte) (i & 0xFF);
        bytes[2] = (byte) ((i >> 8) & 0xFF);
        bytes[1] = (byte) ((i >> 16) & 0xFF);
        bytes[0] = (byte) ((i >> 24) & 0xFF);

        long startTime = System.nanoTime();
        System.out.println("7~38:" + bitCutout(bytes, 7, 38));
        long endTime = System.nanoTime();
        System.out.println("耗时: " + (endTime - startTime) + "纳秒");
        startTime = System.nanoTime();
        System.out.println("3~29:" + bitCutout(bytes, 3, 29));
        endTime = System.nanoTime();
        System.out.println("耗时: " + (endTime - startTime) + "纳秒");
        startTime = System.nanoTime();
        System.out.println("8~20:" + bitCutout(bytes, 8, 20));
        endTime = System.nanoTime();
        System.out.println("耗时: " + (endTime - startTime) + "纳秒");
        startTime = System.nanoTime();
        System.out.println("8~8:" + bitCutout(bytes, 8, 8));
        endTime = System.nanoTime();
        System.out.println("耗时: " + (endTime - startTime) + "纳秒");
        startTime = System.nanoTime();
        System.out.println("7~15:" + bitCutout(bytes, 7, 15));
        endTime = System.nanoTime();
        System.out.println("耗时: " + (endTime - startTime) + "纳秒");
    }
}