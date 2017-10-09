package net.ion.ice.core.context;

import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.api.RequestParameter;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jaehocho on 2017. 7. 4..
 */
public class ApiContext {
    public static final String COMMON_RESPONSE = "commonResponse";
    public static final String COMMON_PARAMETERS = "commonParameters";

    public static final String PARAMETERS = "parameters";
    public static final String DATE_FORMAT = "dateFormat";
    public static final String FILE_URL_FORMAT = "fileUrlFormat";
    private Node apiCategory;
    private Node apiNode;
    private Map<String, Object> data  ;

    private Map<String, Object> config  ;

    private List<ResultField> commonResultFieldList ;
    private List<ResultField> resultFields ;

    private List<RequestParameter> parameters ;

    private NativeWebRequest httpRequest ;
    private HttpServletResponse httpResponse ;

    public static ApiContext createContext(Node apiCategory, Node apiNode, String typeId, String event, Map<String, Object> config, NativeWebRequest request, HttpServletResponse response, Map<String, Object> session) {
        Map<String, String[]> parameterMap = request.getParameterMap() ;
        MultiValueMap<String, MultipartFile> multiFileMap = null ;
        if(request.getNativeRequest() instanceof MultipartHttpServletRequest) {
            multiFileMap = ((MultipartHttpServletRequest) request.getNativeRequest()).getMultiFileMap() ;
        }
        ApiContext ctx = new ApiContext() ;
        ctx.apiCategory = apiCategory ;
        ctx.apiNode = apiNode ;
        ctx.httpRequest = request ;
        ctx.httpResponse = response ;
        ctx.data = ContextUtils.makeContextData(parameterMap, multiFileMap) ;
        ctx.data.put("session", session);
        ctx.data.put("now", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())) ;
        ctx.data.put("sysdate", new Date()) ;
        if(typeId != null) {
            ctx.data.put("typeId", typeId);
        }

        if(event != null) {
            ctx.data.put("event", event);
        }

        if(apiCategory.getId().equals("node") && apiNode.getId().equals("node>read")){
            if(!request.getParameterMap().containsKey("id")){
                ctx.data.put("id", ReadContext.getParamId(request.getParameterMap(), typeId)) ;
            }
        }


        if(apiCategory.containsKey(COMMON_RESPONSE) && apiCategory.get(COMMON_RESPONSE) != null && ((Map<String, Object>) apiCategory.get(COMMON_RESPONSE)).size() > 0) {
            ctx.makeCommonResponse((Map<String, Object>) apiCategory.get(COMMON_RESPONSE)) ;
        }

        if(apiCategory.containsKey(ApiContext.COMMON_PARAMETERS) && apiCategory.get(ApiContext.COMMON_PARAMETERS) != null && ((List<Map<String, Object>>) apiCategory.get(ApiContext.COMMON_PARAMETERS)).size() > 0) {
            checkRequiredParameter((List<Map<String, Object>>) apiCategory.get(ApiContext.COMMON_PARAMETERS), parameterMap, multiFileMap);
        }


        if(apiNode.containsKey(ApiContext.PARAMETERS) && apiNode.get(ApiContext.PARAMETERS) != null && ((List<Map<String, Object>>) apiNode.get(ApiContext.PARAMETERS)).size() > 0) {
            checkRequiredParameter((List<Map<String, Object>>) apiNode.get(ApiContext.PARAMETERS), parameterMap, multiFileMap);
        }

//        ctx.config = new HashMap<>() ;
//        ctx.config.putAll(apiCategory);
        ctx.config = config ;

        return ctx ;
    }

    private static void checkRequiredParameter(List<Map<String, Object>> parameterConfig, Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap) {
        for(Map<String, Object> param : parameterConfig){
            if(param.containsKey("required") && (boolean) param.get("required")){
                String paramName = (String) param.get("paramName");
                if(!((parameterMap != null && parameterMap.containsKey(paramName) && parameterMap.get(paramName) != null && parameterMap.get(paramName).length > 0 && StringUtils.isNotEmpty(parameterMap.get(paramName) [0])) || (multiFileMap != null && multiFileMap.containsKey(paramName)))){
                    throw new ApiException("400", "Required Parameter : " + paramName) ;
                }
            }
        }
    }

    private void makeCommonResponse(Map<String, Object> response){
        commonResultFieldList = new ArrayList<>() ;
        for(String fieldName : response.keySet()) {
            Object fieldValue = response.get(fieldName);
            if (fieldValue == null) {
                commonResultFieldList.add(new ResultField(fieldName, fieldName));
            } else if (fieldValue instanceof String) {
                if (StringUtils.isEmpty((String) fieldValue)) {
                    commonResultFieldList.add(new ResultField(fieldName, fieldName));
                } else {
                    commonResultFieldList.add(new ResultField(fieldName, (String) fieldValue));
                }
            } else if (fieldValue instanceof Map) {
                if (((Map) fieldValue).containsKey("select")) {
                    commonResultFieldList.add(new ResultField(fieldName, ApiSelectContext.makeContextFromConfig((Map<String, Object>) fieldValue, data)));
                } else {
                    commonResultFieldList.add(new ResultField(fieldName, (Map<String, Object>) fieldValue));
                }
            }
        }
    }

    private Map<String, Object> makeSubApiReuslt(Map<String, Object> ctxRootConfig) {
        if(ctxRootConfig.containsKey("event")){
            ApiExecuteContext executeContext = ApiExecuteContext.makeContextFromConfig(ctxRootConfig, data, httpRequest, httpResponse) ;
            QueryResult queryResult = executeContext.makeQueryResult() ;

            addResultData(executeContext.getResult());

            return queryResult ;

        }else if(ctxRootConfig.containsKey("query")){
            ApiQueryContext queryContext = ApiQueryContext.makeContextFromConfig(ctxRootConfig, data) ;
            setApiResultFormat(queryContext);

            QueryResult queryResult = queryContext.makeQueryResult() ;

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
            addResultData(node);

            return readContext.makeResult() ;
        }else if(ctxRootConfig.containsKey("ids")){
            ApiReadsContext readsContext = ApiReadsContext.makeContextFromConfig(ctxRootConfig, data) ;
            setApiResultFormat(readsContext);

            QueryResult queryResult = readsContext.makeQueryResult() ;

            addResultData(readsContext.getResult());

            return queryResult ;
        }else if(ctxRootConfig.containsKey("endpoint")){
            ApiRelayContext relayContext = ApiRelayContext.makeContextFromConfig(ctxRootConfig, data) ;
//            setApiResultFormat(readsContext);

            QueryResult queryResult = relayContext.makeQueryResult() ;

            addResultData(relayContext.getResult());

            return queryResult ;
        }
        return null ;
    }

    private void setApiResultFormat(ReadContext queryContext) {
        if(apiCategory.containsKey(DATE_FORMAT) && apiCategory.get(DATE_FORMAT) != null){
            queryContext.dateFormat = (String) apiCategory.get(DATE_FORMAT);
        }

        if(apiCategory.containsKey(FILE_URL_FORMAT) && apiCategory.get(FILE_URL_FORMAT) != null && ((Map<String, Object>) apiCategory.get(FILE_URL_FORMAT)).size() > 0){
            queryContext.fileUrlFormat = (Map<String, Object>) apiCategory.get(FILE_URL_FORMAT);
            for(String key : queryContext.fileUrlFormat.keySet()){
                queryContext.fileUrlFormat.put(key, ContextUtils.getValue(queryContext.fileUrlFormat.get(key), data)) ;
            }
        }
    }


    public Object makeApiResult() {
        if(config.containsKey("typeId") || config.containsKey("apiType") || config.containsKey("select") || config.containsKey("endpoint")){
            if(this.commonResultFieldList != null && this.commonResultFieldList.size() > 0){
                QueryResult queryResult = getCommonResult();
                queryResult.putAll(makeSubApiReuslt(config));
                return queryResult ;
            }else{
                return makeSubApiReuslt(config);
            }
        }else {
            QueryResult queryResult  ;
            if(this.commonResultFieldList != null && this.commonResultFieldList.size() > 0){
                queryResult = getCommonResult();
            }else{
                queryResult = new QueryResult() ;
            }

            for (String key : config.keySet()) {
                Map<String, Object> ctxRootConfig = (Map<String, Object>) config.get(key);
                if("root".equals(key)) {
                    queryResult.putAll(makeSubApiReuslt(ctxRootConfig)) ;
                }else{
                    Map<String, Object> subQueryResult = makeSubApiReuslt(ctxRootConfig) ;
                    if(subQueryResult != null) {
                        queryResult.put(key, subQueryResult);
                    }
                }
            }
            return queryResult ;
        }
    }

    private QueryResult getCommonResult() {
        QueryResult queryResult = new QueryResult() ;
        for (ResultField resultField : commonResultFieldList) {
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
                _data.putAll((Map<? extends String, ?>) resultList.get(0));
            }
        }else if(result instanceof Map){
            _data.putAll((Map<? extends String, ?>) result);
        }

        this.data = _data ;
    }
}
