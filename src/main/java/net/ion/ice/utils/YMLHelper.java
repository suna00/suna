package net.ion.ice.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by juneyoungoh on 2017. 6. 27..
 * YML 을 읽어서 프로퍼티처럼 접근하는 것이 목표
 */

public class YMLHelper {

    private static Logger logger = Logger.getLogger(YMLHelper.class);
    private static Map<String, Map<String, Object>> YMLMAP = new HashMap<String, Map<String, Object>>();

    // Blocking constructor
    private YMLHelper(){}


    public static void loadYML(String filePath, String key) throws Exception {
        File yaml = new File(filePath);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> load = null;
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            load = mapper.readValue(yaml, Map.class);
            load(load, map, null);
            YMLMAP.put(key, map);
        } catch (Exception e) {
            logger.error("Failed to Parse YML :: ", e);
        }
    }

    public static Map<String, Object> getYML(String key) {
        return (Map) YMLMAP.get(key);
    }


    private static void load(Object original, Map<String, Object> target, String parentKey){
        String keyAppendix = parentKey == null ? "" : parentKey + ".";
        if(original.getClass().getName().endsWith("Map")) {
            Map<String, Object> originalMap = (Map) original;
            Iterator<String> iter = originalMap.keySet().iterator();
            while(iter.hasNext()){
                String key = iter.next();
                Object value = originalMap.get(key);
                String vClassName = value.getClass().getName();

                target.put(keyAppendix + key, value);
                if(vClassName.endsWith("List")) {
                    target.put(keyAppendix + key, String.valueOf(value));
                } else if(vClassName.endsWith("Map")) {
                    load((Map)value, target, keyAppendix + key);
                }
            }
        } else {
            //List
            List<Object> originalList = (List) original;
            if(((List) original).size() < 1 || originalList == null) target.put(parentKey, new String[0]);
            else {
                target.put(parentKey, originalList);
            }
        }
    }


    public static void main(String[] args) throws Exception {
        final String loc = System.getProperty("user.dir") + "/build/resources/main/application.yaml";

        YMLHelper helper = new YMLHelper();

        // 사용 케이스
        YMLHelper.loadYML(loc,"application.yml");
        Map<String, Object> map = YMLHelper.getYML("application.yml");
        logger.info("READYML1 :: " + String.valueOf(map.get("logging.config.classpath"))); // 리스트 반환
        logger.info("READYML2 :: " + String.valueOf(map.get("spring"))); // 맵 반환
        logger.info("READYML3 :: " + String.valueOf(map.get("spring.profiles"))); // 맵 반환
        logger.info("READYML4 :: " + String.valueOf(map.get("spring.profiles.active"))); //스트링 반환
        logger.info("READYML5 :: " + String.valueOf(map.get("hazelcast"))); // 맵 반환
        logger.info("READYML6 :: " + String.valueOf(map.get("hazelcast.members"))); // 리스트 반환
    }
}
