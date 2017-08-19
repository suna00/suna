package net.ion.ice.core.context;

import net.ion.ice.core.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 7. 5..
 */
public class ContextUtils {

    public static Map<String, Object> makeContextData(Map<String, String[]> parameterMap) {
        Map<String, Object> data = new HashMap<>();
        for (String paramName : parameterMap.keySet()) {

            String[] values = parameterMap.get(paramName);
            String value = null;

            if (values == null || StringUtils.isEmpty(values[0])) {
                continue;
            }else if ( values.length == 1 ) {
                value = values[0];
            }else {
                value = StringUtils.join(values, ' ');
            }

            data.put(paramName, value);
        }
        return data;
    }

    public static Map<String, Object> makeContextData(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap){
        Map<String, Object> data = ContextUtils.makeContextData(parameterMap);
        if(multiFileMap == null || multiFileMap.size() == 0)
            return data ;

        for(String paramName : multiFileMap.keySet()){
            List<MultipartFile> multipartFiles = multiFileMap.get(paramName) ;
            if(multipartFiles != null && multipartFiles.size() > 0){
                data.put(paramName, multipartFiles.get(0)) ;
            }
        }

        return data ;
    }


    public static Object getValue(Object configValue, Map<String, Object> data) {
        if(configValue instanceof Template){
            return ((Template)configValue).format(data) ;
        }else if(configValue instanceof String){
//            return JsonUtils.getValue(data, (String) configValue) ;
            return configValue ;
        }else{
            return configValue ;
        }
    }

    public static String getParameterValue(String paramName, Map<String, String[]> parameterMap){
        if(!parameterMap.containsKey(paramName)){
            return null ;
        }
        String[] values = parameterMap.get(paramName);
        if (values == null || StringUtils.isEmpty(values[0])) {
            return null ;
        }
        if(values.length > 1) {
            return StringUtils.join(values, ',');
        }
        return values[0] ;
    }
}
