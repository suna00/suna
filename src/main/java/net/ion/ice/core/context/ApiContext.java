package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.ResultField;
import net.ion.ice.core.response.JsonResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 7. 4..
 */
public class ApiContext {

    private Node apiNode;
    private Map<String, Object> data  ;

    private Map<String, Object> config  ;

    private List<ResultField> resultFieldList ;

    public static ApiContext createContext(Node apiNode, Map<String, Object> config,  Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, Map<String, Object> session) {
        ApiContext ctx = new ApiContext() ;
        ctx.apiNode = apiNode ;
        ctx.data = ContextUtils.makeContextData(parameterMap, multiFileMap) ;
        ctx.data.put("session", session);
        ctx.config = config;

        return ctx ;
    }


    private Map<String, Object> makeSubApiReuslt(Map<String, Object> ctxRootConfig) {
        if(ctxRootConfig.containsKey("event")){
            ExecuteContext executeContext = ExecuteContext.makeContextFromConfig(ctxRootConfig, data) ;
            executeContext.execute();
            Node node = executeContext.getNode() ;
            addResultData(node.clone());

            return executeContext.makeResult() ;
        }else if(ctxRootConfig.containsKey("query")){
            ApiQueryContext queryContext = ApiQueryContext.makeContextFromConfig(ctxRootConfig, data) ;
            QueryResult queryResult = queryContext.makeQueryResult(null, null) ;

            addResultData(queryContext.getResult());

            return queryResult ;
        }else if(ctxRootConfig.containsKey("select")){
            ApiSelectContext selectContext = ApiSelectContext.makeContextFromConfig(ctxRootConfig, data) ;
            QueryResult queryResult =  selectContext.makeQueryResult(null, null) ;
            addResultData(selectContext.getResult());

            return queryResult ;
        }else if(ctxRootConfig.containsKey("id")){
            ApiReadContext readContext = ApiReadContext.makeContextFromConfig(ctxRootConfig, data) ;
            Node node = readContext.getNode() ;
            addResultData(node.clone());

            return readContext.makeResult() ;
        }
        return null ;
    }


    public Object makeApiResult() {

        if(config.containsKey("typeId") || config.containsKey("apiType")){
            return makeSubApiReuslt(config);
        }else {
            QueryResult queryResult = new QueryResult() ;
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
