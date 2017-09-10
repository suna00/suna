package net.ion.ice.core.context;

import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jaeho on 2017. 7. 5..
 */
public class ContextUtils {

    public static Map<String, Object> makeContextData(Map<String, String[]> parameterMap) {
        Map<String, Object> data = new HashMap<>();
        if(parameterMap == null) return data ;
        for (String paramName : parameterMap.keySet()) {

            if(paramName.contains(".")){
                String subTypeId = StringUtils.substringBefore(paramName, ".") ;
                NodeType subNodeType = NodeUtils.getNodeType(subTypeId) ;
                if(subNodeType != null){
                    List<Map<String, Object>> subData = new ArrayList<>() ;

                }
            }

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

    public static void makeApiResponse(Map<String, Object> response, ReadContext readContext) {

//                if(response.containsKey("merge")){
//                    queryContext.responseType = "merge" ;
//                    queryContext.mergeField = (String) response.get("merge");
//                }

        for(String fieldName : response.keySet()) {
            Object fieldValue = response.get(fieldName) ;
            if(fieldName.equals("_all_")){
                readContext.addResultField(new ResultField(fieldName, fieldName));
            } else if (fieldValue == null) {
                readContext.addResultField(new ResultField(fieldName, fieldName));
            } else if (fieldValue instanceof String) {
                if(StringUtils.isEmpty((String) fieldValue)){
                    readContext.addResultField(new ResultField(fieldName, fieldName));
                }else {
                    readContext.addResultField(new ResultField(fieldName, (String) fieldValue));
                }
            } else if (fieldValue instanceof Map) {
                readContext.addResultField(new ResultField(fieldName, (Map<String, Object>) fieldValue));
            }
        }
    }

    public static String makeContextConfig(ReadContext readContext, String paramName, String value) {
        if (paramName.equals("fields") || paramName.equals("pids") || paramName.equals("response") || paramName.equals("searchFields") || paramName.equals("searchValue")) {
            return null;
        }

        if(paramName.equals("includeReferenced")){
            readContext.makeIncludeReferenced(value);
            return null;
        } else if(paramName.equals("referenceView")){
            readContext.makeReferenceView(value);
            return null;
        }

        if(readContext instanceof QueryContext){
            QueryContext queryContext = (QueryContext) readContext;
            if (paramName.equals("page")) {
                queryContext.setCurrentPage(value);
                return null;
            } else if (paramName.equals("pageSize")) {
                queryContext.setPageSize(value);
                return null;
            } else if (paramName.equals("count") || paramName.equals("limit")) {
                queryContext.setLimit(value);
                return null;
            } else if (paramName.equals("query")) {
                try {
                    Map<String, Object> query = JsonUtils.parsingJsonToMap(value);

                } catch (IOException e) {
                }
                return null;
            }
        }
        return value;
    }

}
