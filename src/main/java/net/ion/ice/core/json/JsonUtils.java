package net.ion.ice.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonUtils {
    private static Logger logger = LoggerFactory.getLogger(JsonUtils.class);
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static boolean isJson(String jsonString) {
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static Object parsingJsonToObject(String jsonString) {
        Object result = null;
        try {
            if (isList(jsonString)) {
                result = parsingJsonToList(jsonString);
            } else {
                result = parsingJsonToMap(jsonString);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return result;
    }

    public static List<Map<String,Object>> parsingJsonToList(String jsonString) throws IOException {
        return objectMapper.readValue(jsonString, List.class);
    }


    public static Map<String, Object> parsingJsonToMap(String jsonString) throws IOException {
        return objectMapper.readValue(jsonString, Map.class);
    }

    public static boolean isList(String jsonString) throws IOException {
        return StringUtils.startsWith(jsonString.trim(), "[");
    }

    public static Map<String,Object> parsingJsonFileToMap(File jsonFile, String encoding) throws IOException {
        return parsingJsonToMap(FileUtils.readFileToString(jsonFile, encoding)) ;
    }

    public static Map<String,Object> parsingJsonFileToMap(File jsonFile) throws IOException {
        return parsingJsonToMap(FileUtils.readFileToString(jsonFile, "UTF-8")) ;
    }

    public static Collection<Map<String,Object>> parsingJsonFileToList(File jsonFile, String encoding) throws IOException {
        return parsingJsonToList(FileUtils.readFileToString(jsonFile, encoding)) ;
    }

    public static Collection<Map<String,Object>> parsingJsonFileToList(File jsonFile) throws IOException {
        return parsingJsonFileToList(jsonFile, "UTF-8") ;
    }

    public static Collection<Map<String,Object>> parsingJsonResourceToList(Resource jsonFile) throws IOException {
        return parsingJsonStreamToList(jsonFile.getInputStream(), "UTF-8") ;
    }

    public static Collection<Map<String,Object>> parsingJsonStreamToList(InputStream inputStream, String encoding) {
        try {
            String jsonString = IOUtils.toString(inputStream, encoding) ;
            return parsingJsonToList(jsonString) ;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return null ;
    }

    public static void writeJsonFile(File file, Object data) throws IOException {
        objectMapper.writeValue(file, data);
    }

    public static boolean contains(Map<String, Object> jsonData, String key, Object value) {
        String[] keys = StringUtils.split(key, ".") ;

        if(! jsonData.containsKey(keys[0])) return false ;
        Object val = jsonData.get(keys[0]) ;
        if(val instanceof List){
            for(Object listVal : (List) val){
                if(listVal instanceof Map){
                    if(contains((Map<String, Object>) listVal, StringUtils.substringAfter(key, "."), value)){
                        return true ;
                    }
                }else if(listVal != null && listVal.equals(value)){
                    return true;
                }
            }
            return false ;
        }else if(val instanceof Map){
            return contains((Map<String, Object>) val, StringUtils.substringAfter(key, "."), value) ;
        }else if(val != null){
            return val.equals(value) ;
        }
        return false ;
    }

    public static Object getValue(Map<String, Object> jsonData, String key) {
        String[] keys = StringUtils.split(key, ".") ;

        if(! jsonData.containsKey(keys[0])) return null ;
        Object val = jsonData.get(keys[0]) ;
        if(val instanceof List){
            String result = "" ;
            for(Object listVal : (List) val){
                if(listVal instanceof Map){
                    result += getValue((Map<String, Object>) listVal, StringUtils.substringAfter(key, ".")) + "," ;
                }else if(listVal != null){
                    result += listVal + ",";
                }
            }
            if(StringUtils.isNotEmpty(result)){
                result = StringUtils.substringBeforeLast(result, ",") ;
            }
            return result ;
        }else if(val instanceof Map){
            return getValue((Map<String, Object>) val, StringUtils.substringAfter(key, ".")) ;
        }else if(val != null){
            return val ;
        }
        return null ;
    }

    public static String toJsonString(Map<String, Object> jsonData){
        try {
            return objectMapper.writeValueAsString(jsonData) ;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null ;
        }
    }

    public static String toJsonString(List<?> jsonData){
        try {
            return objectMapper.writeValueAsString(jsonData) ;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null ;
        }
    }
}
