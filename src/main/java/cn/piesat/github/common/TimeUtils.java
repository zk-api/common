package cn.piesat.github.common;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时间工具类
 *
 * @author zk
 * @date 2021/3/24 17:53
 */
public class TimeUtils {

    /** 年积日位数 */
    private final static String DOY_DECIMAL_FORMAT = "000";
    /** 小时位数 */
    private final static String HOUR_DECIMAL_FORMAT = "00";

    /**
     * 字符串转时间
     *
     * @param time   时间字符串
     * @param format 时间格式
     * @return
     */
    public static LocalDateTime parseStringToDateTime(String time, String format) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.parse(time, df);
    }

    /**
     * 字符串转日期
     *
     * @param time   日期字符串
     * @param format 日期格式
     * @return
     */
    public static LocalDate parseStringToDate(String time, String format) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(format);
        return LocalDate.parse(time, df);
    }

    /**
     * 时间转字符串
     *
     * @param localDateTime 时间
     * @param format        时间格式
     * @return
     */
    public static String parseDateTimeToString(LocalDateTime localDateTime, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return localDateTime.format(formatter);
    }

    /**
     * 时间字符串转换为另一种时间字符串格式
     *
     * @param time     时间字符串
     * @param format   当前格式
     * @param toFormat 要转换的格式
     * @return
     */
    public static String parseTimeStringToTimeString(String time, String format, String toFormat) {
        LocalDateTime localDateTime = parseStringToDateTime(time, format);
        return parseDateTimeToString(localDateTime, toFormat);
    }

    /**
     * 年积日补齐位数
     *
     * @param doy 年积日
     * @return
     */
    public static String parseDoyCompletion(int doy) {
        DecimalFormat df = new DecimalFormat(DOY_DECIMAL_FORMAT);
        return df.format(doy);
    }

    /**
     * 小时补齐位数
     *
     * @param hour
     * @return
     */
    public static String parseHourCompletion(int hour) {
        DecimalFormat df = new DecimalFormat(HOUR_DECIMAL_FORMAT);
        return df.format(hour);
    }

    /**
     * 时间段按照步长进行分割
     *
     * @param stime 开始时间
     * @param etime 结束时间
     * @param step  步长
     * @return
     */
    public static List<String> parseDateTimeCompletionByStep(LocalDateTime stime, LocalDateTime etime, long step) {
        List<String> list = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        list.add(stime.format(formatter));
        while (stime.isBefore(etime)) {
            stime = stime.plusSeconds(step);
            if (stime.isBefore(etime)) {
                list.add(stime.format(formatter));
            }
        }
        return list;
    }

    public static List<String> parseDateTimeCompletionByStep(LocalDate date, long step) {
        LocalDateTime stime = date.atTime(0, 0, 0);
        LocalDateTime etime = date.atTime(23, 59, 59);
        return parseDateTimeCompletionByStep(stime, etime, step);
    }

    /**
     * 将对象格式的数据补齐时间序列
     * @param date 日期
     * @param step 步长
     * @param list 待补齐数据
     * @param timeField 时间字段
     * @param <T> 对象泛型
     * @return
     */
    public static <T> List<T> parseDateTimeCompletionByObject(LocalDate date, long step, List<T> list, String timeField) {
        List<String> allDateTime = parseDateTimeCompletionByStep(date, step);
        for (int i = 0; i < allDateTime.size(); i++) {

            try {
                if (i < list.size()) {
                    T t = list.get(i);
                    Class<T> clazz = (Class<T>) t.getClass();
                    Field declaredField = clazz.getDeclaredField(timeField);
                    declaredField.setAccessible(true);
                    if (!allDateTime.get(i).equals(declaredField.get(t))) {
                        Constructor<T> constructor = clazz.getConstructor();
                        T instance = constructor.newInstance();
                        Class<T> aClass = (Class<T>) instance.getClass();
                        Field declaredField1 = aClass.getDeclaredField(timeField);
                        declaredField1.setAccessible(true);
                        declaredField1.set(instance, allDateTime.get(i));
                        list.add(i, instance);
                    }
                } else {
                    T t = list.get(0);
                    Class<T> clazz = (Class<T>) t.getClass();
                    Constructor<T> constructor = clazz.getConstructor();
                    T instance = constructor.newInstance();
                    Class<T> aClass = (Class<T>) instance.getClass();
                    Field declaredField1 = aClass.getDeclaredField(timeField);
                    declaredField1.setAccessible(true);
                    declaredField1.set(instance, allDateTime.get(i));
                    list.add(i, instance);
                }
            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    /**
     * 将Map格式的数据补齐时间序列
     * @param date 日期
     * @param step 步长
     * @param list 待补齐数据
     * @param timeField 时间字段
     * @return
     */
    public static List<Map<String, String>> parseDateTimeCompletionByMap(LocalDate date, long step, List<Map<String, String>> list, String timeField) {
        List<String> allDateTime = parseDateTimeCompletionByStep(date, step);
        for (int i = 0; i < allDateTime.size(); i++) {
            //当完整时间在数据时间范围内时
            if (i < list.size()) {
                //时间字段值不同时，代表时间有空缺
                if (!allDateTime.get(i).equals(list.get(i).get(timeField))) {
                    Map<String, String> map = new HashMap<>(1);
                    map.put(timeField, allDateTime.get(i));
                    list.add(i, map);
                }
            } else {
                //当完整时间超过数据范围时，直接补充新对象
                Map<String, String> map = new HashMap<>(1);
                map.put(timeField, allDateTime.get(i));
                list.add(i, map);
            }
        }
        return list;
    }

    public static void main(String[] args) {
        List<DatePojo> list = new ArrayList<>();
        DatePojo datePojo = new DatePojo();
        datePojo.setTime("2021-05-26 00:00:00");
        datePojo.setNum(1);
        DatePojo datePojo1 = new DatePojo();
        datePojo1.setTime("2021-05-26 00:00:10");
        datePojo1.setNum(2);
        list.add(datePojo);
        list.add(datePojo1);
        List<DatePojo> datePojos = parseDateTimeCompletionByObject(LocalDate.now(), 5, list, "time");
        System.out.println(datePojos);
    }

}

class DatePojo {
    private String time;
    private Integer num;

    public DatePojo() {}

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }
}
