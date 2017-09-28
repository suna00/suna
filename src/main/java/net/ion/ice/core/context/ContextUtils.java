package net.ion.ice.core.context;

import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
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
        Map<String, Object> dataMap = new HashMap<>();
        if(parameterMap == null) return dataMap ;
        Map<String, Object> subDataMap = new HashMap<>();

        for (String paramName : parameterMap.keySet()) {
            if(paramName.startsWith("_") && paramName.endsWith("_")) continue;
            if(paramName.contains(".")){
                String subTypeId = StringUtils.substringBefore(paramName, ".") ;
                if(dataMap.containsKey(subTypeId) && dataMap.get(subTypeId) instanceof List){
                    List<Map<String, Object>> subList = (List<Map<String, Object>>) dataMap.get(subTypeId);
                    String[] values = parameterMap.get(paramName);
                    for(int i=0; i< subList.size(); i++){
                        Map<String, Object> subData = subList.get(i) ;
                        subData.put(StringUtils.substringAfterLast(paramName, "."), values.length == 1 ? values[0] : (values.length > i ? values[i] : null)) ;
                    }
                    continue;
                } else if(StringUtils.contains(subTypeId, "[") && StringUtils.contains(subTypeId, "]")) {
                    if (!subDataMap.containsKey(subTypeId)) subDataMap.put(subTypeId, new HashMap<>());
                    String[] values = parameterMap.get(paramName);
                    String value = null;

                    if (values == null || StringUtils.isEmpty(values[0])) {
                        continue;
                    }else if ( values.length == 1 ) {
                        value = values[0];
                    }else {
                        value = StringUtils.join(values, ',');
                    }

                    Map<String, Object> subData = (Map<String, Object>) subDataMap.get(subTypeId);
                    subData.put(StringUtils.substringAfterLast(paramName, "."), value);
                    continue;
                } else {
                    NodeType subNodeType = NodeUtils.getNodeType(subTypeId);
                    if (subNodeType != null) {
                        List<Map<String, Object>> subList = new ArrayList<>();
                        String[] values = parameterMap.get(paramName);
                        for (int i = 0; i < values.length; i++) {
                            Map<String, Object> subData = new HashMap<String, Object>();
                            subData.put(StringUtils.substringAfterLast(paramName, "."), values[i]);
                            subList.add(subData);
                        }
                        dataMap.put(subTypeId, subList) ;
                        continue;
                    }
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

            dataMap.put(paramName, value);
        }

        for (String subDataKey : subDataMap.keySet()) {
            String subTypeId = StringUtils.substringBefore(subDataKey, "[");
            if (!dataMap.containsKey(subTypeId)) dataMap.put(subTypeId, new ArrayList<>());
            List<Map<String, Object>> subList = (List<Map<String, Object>>) dataMap.get(subTypeId);
            subList.add((Map<String, Object>) subDataMap.get(subDataKey));
        }

        return dataMap;
    }

    public static Map<String, Object> makeContextData(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap){
        Map<String, Object> dataMap = ContextUtils.makeContextData(parameterMap);
        if(multiFileMap == null || multiFileMap.size() == 0) return dataMap ;
        Map<String, Object> subDataMap = new HashMap<>();

        for(String paramName : multiFileMap.keySet()){
            List<MultipartFile> multipartFiles = multiFileMap.get(paramName) ;

            if(paramName.contains(".")){
                String subTypeId = StringUtils.substringBefore(paramName, ".") ;
                if(dataMap.containsKey(subTypeId) && dataMap.get(subTypeId) instanceof List){
                    List<Map<String, Object>> subList = (List<Map<String, Object>>) dataMap.get(subTypeId);
                    for(int i=0; i< subList.size(); i++){
                        Map<String, Object> subData = subList.get(i) ;
                        subData.put(StringUtils.substringAfterLast(paramName, "."), multipartFiles.size() == 1 ? multipartFiles.get(0) : (multipartFiles.size() > i ? multipartFiles.get(i) : null)) ;
                    }
                    continue;
                }else if(StringUtils.contains(subTypeId, "[") && StringUtils.contains(subTypeId, "]")) {
                    if(multipartFiles != null && multipartFiles.size() > 0){
                        if (!subDataMap.containsKey(subTypeId)) subDataMap.put(subTypeId, new HashMap<>());
                        Map<String, Object> subData = (Map<String, Object>) subDataMap.get(subTypeId);
                        subData.put(StringUtils.substringAfterLast(paramName, "."), multipartFiles.get(0));
                    }
                    continue;
                } else {
                    NodeType subNodeType = NodeUtils.getNodeType(subTypeId);
                    if (subNodeType != null) {
                        List<Map<String, Object>> subList = new ArrayList<>();

                        for (MultipartFile file : multipartFiles) {
                            Map<String, Object> subData = new HashMap<String, Object>();
                            subData.put(StringUtils.substringAfterLast(paramName, "."), file);
                            subList.add(subData);
                        }
                        dataMap.put(subTypeId, subList) ;
                        continue;
                    }
                }
            }

            if(multipartFiles != null && multipartFiles.size() > 0){
                dataMap.put(paramName, multipartFiles.get(0)) ;
            }
        }

        return dataMap ;
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

    public static Object getValue(Object staticValue, Map<String, Object> data, ReadContext readContext, NodeType nodeType, Node node) {
        if(staticValue instanceof String && StringUtils.contains((String) staticValue, "{{:") && StringUtils.contains((String) staticValue, "}}")){
            Template template = new Template((String) staticValue) ;
            template.parsing();
            return template.format(data, readContext, nodeType, node) ;
        }else{
            return staticValue ;
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
        } else if(paramName.equals(ApiContext.DATE_FORMAT)){
            readContext.dateFormat = value ;
            return null;
        } else if(paramName.equals(ApiContext.FILE_URL_FORMAT)){
            try {
                readContext.fileUrlFormat = JsonUtils.parsingJsonToMap(value) ;
            } catch (IOException e) {
            }
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
