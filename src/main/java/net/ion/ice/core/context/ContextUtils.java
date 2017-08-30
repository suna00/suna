package net.ion.ice.core.context;

import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.query.ResultField;
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
        if(parameterMap == null) return data ;
        for (String paramName : parameterMap.keySet()) {

            String[] values = parameterMap.get(paramName);
            String value = null;

            if (values == null || StringUtils.isEmpty(values[0])) {
                continue;
            }else if ( values.length == 1 ) {
                value = values[0];
            }else {
                value = StringUtils.join(values, ',');
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
            if(StringUtils.contains((String) configValue, "{{:") && StringUtils.contains((String) configValue, "}}")){
                Template template = new Template((String) configValue) ;
                template.parsing();
                return template.format(data) ;
            }else{
                return configValue ;
            }
//            return JsonUtils.getValue(data, (String) configValue) ;
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

    public static void makeApiResponse(Map<String, Object> response, Map<String, Object> data, ReadContext readContext) {

//                if(response.containsKey("merge")){
//                    queryContext.responseType = "merge" ;
//                    queryContext.mergeField = (String) response.get("merge");
//                }

        for(String fieldName : response.keySet()) {
            Object fieldValue = response.get(fieldName) ;
            if (fieldValue == null) {
                readContext.addResultField(new ResultField(fieldName, fieldName));
            } else if (fieldValue instanceof String) {
                if(StringUtils.isEmpty((String) fieldValue)){
                    readContext.addResultField(new ResultField(fieldName, fieldName));
                }else {
                    readContext.addResultField(new ResultField(fieldName, (String) fieldValue));
                }
            } else if (fieldValue instanceof Map) {
                if(((Map) fieldValue).containsKey("query")) {
                    readContext.addResultField(new ResultField(fieldName, ApiQueryContext.makeContextFromConfig((Map<String, Object>) fieldValue, data)));
                }else if(((Map) fieldValue).containsKey("select")){
                    readContext.addResultField(new ResultField(fieldName, ApiSelectContext.makeContextFromConfig((Map<String, Object>) fieldValue, data)));
                }else{
                    readContext.addResultField(new ResultField(fieldName, (Map<String, Object>) fieldValue));
                }
            }
        }
    }
}
