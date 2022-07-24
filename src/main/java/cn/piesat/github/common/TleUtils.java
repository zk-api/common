package cn.piesat.github.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * tle 工具类
 *
 * @author zk
 * @date 2022/7/4 17:43
 */
public class TleUtils {
    public static String tleToJson(String filePath) {
        Path path = Paths.get(filePath);
        List<Map<String, String>> list = new ArrayList<>();
        try {
            Iterator<String> iterator = Files.lines(path).iterator();
            while (iterator.hasNext()) {
                Map<String, String> map = new HashMap<>(4);
                String line1 = iterator.next();
                String line2 = iterator.next();
                String line3 = iterator.next();
                String norad = line3.split("\\s+")[1];
                String name = line1;
                String tle1 =line2;
                String tle2 = line3;
                map.put("norad", norad);
                map.put("name", name);
                map.put("tle1", tle1);
                map.put("tle2", tle2);
                list.add(map);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(tleToJson("D:\\work\\项目\\BD2020-009\\GLONASS.tle"));
    }
}
