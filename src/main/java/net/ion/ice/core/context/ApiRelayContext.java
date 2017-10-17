package net.ion.ice.core.context;

import net.ion.ice.core.api.ApiUtils;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.query.QueryResult;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiRelayContext {
    private Object result ;


    private Map<String, Object> parameters ;
    private String endpoint ;

    private String relayMethod ;

    private Integer connectTimeout;
    private Integer readTimeout ;


    public static ApiRelayContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ApiRelayContext ctx = new ApiRelayContext() ;
        ctx.endpoint = (String) ContextUtils.getValue(config.get("endpoint"), data);

        if(config.containsKey("parameters")){
            Map<String, Object> paramsMap = new LinkedHashMap<>() ;
            Object params = config.get("parameters") ;
            if (params instanceof java.util.List) {
                for (Map<String, Object> _p : (java.util.List<Map<String, Object>>) params) {
                    makeRequestParameters(paramsMap, _p, data) ;
                }
            } else if (params instanceof Map) {
                makeRequestParameters(paramsMap, (Map<String, Object>) params, data) ;
            }
            ctx.parameters = paramsMap ;
        }

        ctx.relayMethod =  (String) ContextUtils.getValue(config.get("relayMethod"), data);

        ctx.connectTimeout = config.containsKey("connectTimeout") ? Integer.parseInt(config.get("connectTimeout").toString()) : 5000 ;
        ctx.readTimeout = config.containsKey("readTimeout") ? Integer.parseInt(config.get("readTimeout").toString()) : 10000 ;
        return ctx ;
    }

    private static void makeRequestParameters(Map<String, Object> paramsMap, Map<String, Object> _p, Map<String, Object> data) {
        if(_p.containsKey("paramName")){
            paramsMap.put((String) ContextUtils.getValue(_p.get("paramName"), data), ContextUtils.getValue(_p.get("paramValue"), data));
        }else{
            for(String key : _p.keySet()){
                paramsMap.put((String) ContextUtils.getValue(key, data), (String) ContextUtils.getValue(_p.get(key), data)) ;
            }
        }
    }

    public Object getResult() {
        return result;
    }

    public QueryResult makeQueryResult() {
        try {
            String result = ApiUtils.callApiMethod(endpoint, parameters, connectTimeout, readTimeout, relayMethod) ;
            QueryResult queryResult = new QueryResult(JsonUtils.parsingJsonToMap(result)) ;
            return queryResult ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null ;
    }
}
