package net.ion.ice.core.context;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiExecuteContext extends ExecuteContext implements CacheableContext{
    protected Map<String, Object> config  ;


    public QueryResult makeQueryResult() {
        if(this.ifTest != null && !(this.ifTest.equalsIgnoreCase("true"))){
            return new QueryResult().setResult("0").setResultMessage("None Executed") ;
        }
        if (cacheable != null && cacheable ) {
            String cacheKey = makeCacheKey() ;
            return ContextUtils.makeCacheResult(cacheKey, this) ;
        }

        if(this.remote != null && this.remote){
            Map<String, Object> queryResult = ClusterUtils.callExecute(this, false) ;
            if(queryResult.containsKey("item")) {
                this.result = queryResult.get("item");
//                if(config.containsKey("response") && this.result instanceof Map) {
//                    return new QueryResult((Map<String, Object>) this.result) ;
//                }
            }else{
                if(queryResult.containsKey("result") && !queryResult.get("result").equals("200")){
                    return new QueryResult(queryResult) ;
                }
                Map<String, Object> callResult = new LinkedHashMap<>() ;
                for(String key : queryResult.keySet()){
                    if(key.equals("result") || key.equals("resultMessage")){
                        continue;
                    }
                    callResult.put(key, queryResult.get(key)) ;
                }
                this.result = callResult ;
            }
            return new QueryResult(queryResult) ;
        }else {
            return makeCacheResult();
        }
    }

    public String makeCacheKey() {
        StringBuffer params = new StringBuffer() ;
        for(String key : httpRequest.getParameterMap().keySet()){
            if(key.equals(ClusterUtils.CONFIG_) || key.equals(ClusterUtils.DATE_FORMAT_) || key.equals(ClusterUtils.FILE_URL_FORMAT_) || key.equals("now")|| key.equals("sysdate") || key.equals("session")|| (excludeCache != null && excludeCache.contains(key))) continue;

            params.append(key);
            params.append("=") ;
            params.append(httpRequest.getParameter(key)) ;
            params.append("&") ;

        }
        String keySrc = httpRequest.getRequestURI() + "?" + params;
        return keySrc;
    }


    public static ApiExecuteContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ApiExecuteContext ctx = new ApiExecuteContext();

        NodeType nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data));
        ctx.setNodeType(nodeType);

        ctx.event = (String) ContextUtils.getValue(config.get("event"), data);
        ctx.config = config ;

        checkCacheable(config, data, ctx) ;

        checkExclude(config, data, ctx) ;

        if(config.containsKey("if")){
            ctx.ifTest =  ContextUtils.getValue(config.get("if"), data).toString();
        }


        if(config.containsKey("data")){
            Map<String, Object> _data = new HashMap<>();
            _data.putAll(data);
            Map<String, Object> subData = (Map<String, Object>) config.get("data");
            for(String key : subData.keySet()){
                _data.put(key, ContextUtils.getValue(subData.get(key), data)) ;
            }
            ctx.data = _data ;
        }else{
            ctx.data = data ;
        }

        if(!ClusterUtils.getClusterService().checkClusterGroup(nodeType)){
            ctx.remote = true ;
            return ctx ;
        }


        if(config.containsKey("response")){
            ContextUtils.makeApiResponse((Map<String, Object>) config.get("response"), ctx);
        }

        if(config.containsKey(ApiContext.DATE_FORMAT)){
            ctx.dateFormat = config.get(ApiContext.DATE_FORMAT).toString() ;
        } else if(config.containsKey(ApiContext.FILE_URL_FORMAT)){
            try {
                ctx.fileUrlFormat = JsonUtils.parsingJsonToMap(config.get(ApiContext.FILE_URL_FORMAT).toString()) ;
            } catch (IOException e) {
            }
        }
        ctx.init() ;

        return ctx ;
    }


    public static ApiExecuteContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ApiExecuteContext ctx = makeContextFromConfig(config, data);
        ctx.httpRequest = httpRequest;
        ctx.httpResponse = httpResponse ;

        return ctx ;
    }


    public Map<String, Object> getConfig() {
        return config;
    }

    @Override
    public String getCacheTime() {
        return cacheTime;
    }

    @Override
    public QueryResult makeCacheResult() {
        EventService eventService = ApplicationContextManager.getBean(EventService.class);
        eventService.execute(this);

        if (subExecuteContexts != null) {
            for (ExecuteContext subExecuteContext : subExecuteContexts) {
                eventService.execute(subExecuteContext);
            }
        }
        return makeResult() ;
    }
}
