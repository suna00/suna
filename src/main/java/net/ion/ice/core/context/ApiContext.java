package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jaehocho on 2017. 7. 4..
 */
public class ApiContext {
    public static final String COMMON_RESPONSE = "commonResponse";
    public static final String DATE_FORMAT = "dateFormat";
    public static final String FILE_URL_FORMAT = "fileUrlFormat";
    private Node apiCategory;
    private Node apiNode;
    private Map<String, Object> data  ;

    private Map<String, Object> config  ;

    private List<ResultField> resultFieldList ;

    public static ApiContext createContext(Node apiCategory, Node apiNode, Map<String, Object> config, Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, Map<String, Object> session) {
        ApiContext ctx = new ApiContext() ;
        ctx.apiCategory = apiCategory ;
        ctx.apiNode = apiNode ;
        ctx.data = ContextUtils.makeContextData(parameterMap, multiFileMap) ;
        ctx.data.put("session", session);
        ctx.data.put("now", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())) ;
        ctx.data.put("sysdate", new Date()) ;


        if(apiCategory.containsKey(COMMON_RESPONSE) && apiCategory.get(COMMON_RESPONSE) != null && ((Map<String, Object>) apiCategory.get(COMMON_RESPONSE)).size() > 0) {
            ctx.makeCommonResponse((Map<String, Object>) apiCategory.get(COMMON_RESPONSE)) ;
        }


//        ctx.config = new HashMap<>() ;
//        ctx.config.putAll(apiCategory);
        ctx.config = config ;

        return ctx ;
    }

    private void makeCommonResponse(Map<String, Object> response){
        resultFieldList = new ArrayList<>() ;
        for(String fieldName : response.keySet()) {
            Object fieldValue = response.get(fieldName);
            if (fieldValue == null) {
                resultFieldList.add(new ResultField(fieldName, fieldName));
            } else if (fieldValue instanceof String) {
                if (StringUtils.isEmpty((String) fieldValue)) {
                    resultFieldList.add(new ResultField(fieldName, fieldName));
                } else {
                    resultFieldList.add(new ResultField(fieldName, (String) fieldValue));
                }
            } else if (fieldValue instanceof Map) {
                if (((Map) fieldValue).containsKey("select")) {
                    resultFieldList.add(new ResultField(fieldName, ApiSelectContext.makeContextFromConfig((Map<String, Object>) fieldValue, data)));
                } else {
                    resultFieldList.add(new ResultField(fieldName, (Map<String, Object>) fieldValue));
                }
            }
        }
    }

    private Map<String, Object> makeSubApiReuslt(Map<String, Object> ctxRootConfig) {
        if(ctxRootConfig.containsKey("event")){
            ExecuteContext executeContext = ExecuteContext.makeContextFromConfig(ctxRootConfig, data) ;
            if(executeContext.execute()) {
                Node node = executeContext.getNode();
                addResultData(node.clone());

                return executeContext.makeResult() ;
            }else{
                return new QueryResult().setResult("0").setResultMessage("None Executed") ;
            }

        }else if(ctxRootConfig.containsKey("query")){
            ApiQueryContext queryContext = ApiQueryContext.makeContextFromConfig(ctxRootConfig, data) ;
            setApiResultFormat(queryContext);

            QueryResult queryResult = queryContext.makeQueryResult(null, null) ;

            addResultData(queryContext.getResult());

            return queryResult ;
        }else if(ctxRootConfig.containsKey("select")){
            ApiSelectContext selectContext = ApiSelectContext.makeContextFromConfig(ctxRootConfig, data) ;
            setApiResultFormat(selectContext);

            QueryResult queryResult =  selectContext.makeQueryResult(null, null) ;
            addResultData(selectContext.getResult());

            return queryResult ;
        }else if(ctxRootConfig.containsKey("id")){
            ApiReadContext readContext = ApiReadContext.makeContextFromConfig(ctxRootConfig, data) ;
            setApiResultFormat(readContext);

            Node node = readContext.getNode() ;
            addResultData(node.clone());

            return readContext.makeResult() ;
        }
        return null ;
    }

    private void setApiResultFormat(ReadContext queryContext) {
        if(apiCategory.containsKey(DATE_FORMAT) && apiCategory.get(DATE_FORMAT) != null){
            queryContext.dateFormat = (String) apiCategory.get(DATE_FORMAT);
        }

        if(apiCategory.containsKey(FILE_URL_FORMAT) && apiCategory.get(FILE_URL_FORMAT) != null && ((Map<String, Object>) apiCategory.get(FILE_URL_FORMAT)).size() > 0){
            queryContext.fileUrlFormat = (Map<String, Object>) apiCategory.get(FILE_URL_FORMAT);
        }
    }


    public Object makeApiResult() {
        if(config.containsKey("typeId") || config.containsKey("apiType")){
            if(this.resultFieldList != null && this.resultFieldList.size() > 0){
                QueryResult queryResult = getCommonResult();
                queryResult.putAll(makeSubApiReuslt(config));
                return queryResult ;
            }else{
                return makeSubApiReuslt(config);
            }
        }else {
            QueryResult queryResult  ;
            if(this.resultFieldList != null && this.resultFieldList.size() > 0){
                queryResult = getCommonResult();
            }else{
                queryResult = new QueryResult() ;
            }

            for (String key : config.keySet()) {
                Map<String, Object> ctxRootConfig = (Map<String, Object>) config.get(key);
                if("root".equals(key)) {
                    queryResult.putAll(makeSubApiReuslt(ctxRootConfig)) ;
                }else{
                    queryResult.put(key, makeSubApiReuslt(ctxRootConfig)) ;
                }
            }
            return queryResult ;
        }
    }

    private QueryResult getCommonResult() {
        QueryResult queryResult = new QueryResult() ;
        for (ResultField resultField : resultFieldList) {
            if(resultField.isStaticValue()){
                queryResult.put(resultField.getFieldName(), ContextUtils.getValue(resultField.getStaticValue(), data));
            } else {
                queryResult.put(resultField.getFieldName(), ContextUtils.getValue(resultField.getFieldValue(), data));
            }
        }
        return queryResult ;
    }


    public void addResultData(Object result) {

        Map<String, Object> _data = new HashMap<>() ;
        _data.putAll(data);

        if(result instanceof List){
            List resultList = (List) result;
            if(resultList.size() > 0) {
                _data.putAll((Map<? extends String, ?>) resultList.get(resultList.size() - 1));
            }
        }else{
            _data.putAll((Map<? extends String, ?>) result);
        }

        this.data = _data ;
    }
}
