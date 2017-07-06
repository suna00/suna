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

            if (values != null && values.length > 0 && StringUtils.isNotEmpty(values[0])) {
                value = values[0];
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


}
