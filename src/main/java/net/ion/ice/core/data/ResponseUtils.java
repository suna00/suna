package net.ion.ice.core.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by seonwoong on 2017. 7. 10..
 */
public class ResponseUtils {

    public static Map<String, Object> response(Map<String, Object> item) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("result", "200");
        result.put("resultMessage", "SUCCESS");
        result.putAll(item);
        return result;
    }

    public static Map<String, Object> response(Collection<Map<String, Object>> items) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("result", "200");
        result.put("resultMessage", "SUCCESS");
        result.put("items", items);

        return result;
    }

    public static Map<String, Object> error(Exception e) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("result", "500");
        result.put("resultMessage", e.getMessage());

        return result;
    }
}
