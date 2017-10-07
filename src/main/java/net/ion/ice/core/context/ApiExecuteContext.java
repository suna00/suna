package net.ion.ice.core.context;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiExecuteContext extends ExecuteContext{
    protected Map<String, Object> config  ;


    public QueryResult makeQueryResult() {
        if(this.ifTest != null && !(this.ifTest.equalsIgnoreCase("true"))){
            return new QueryResult().setResult("0").setResultMessage("None Executed") ;
        }
        if(this.remote != null && this.remote){
            Map<String, Object> queryResult = ClusterUtils.callExecute(this) ;
            if(queryResult.containsKey("item")) {
                this.result = (Map<String, Object>) queryResult.get("item");
            }else{
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


    public static ApiExecuteContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data, NativeWebRequest httpRequest, HttpServletResponse httpResponse) {
        ApiExecuteContext ctx = new ApiExecuteContext();
        ctx.httpRequest = httpRequest.getNativeRequest(HttpServletRequest.class);
        ctx.httpResponse = httpResponse ;


        NodeType nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data));
        ctx.setNodeType(nodeType);

        ctx.event = (String) ContextUtils.getValue(config.get("event"), data);
        ctx.config = config ;

        if(config.containsKey("if")){
            ctx.ifTest =  ContextUtils.getValue(config.get("if"), data).toString();
        }

        if(!ClusterUtils.getClusterService().checkClusterGroup(nodeType)){
            ctx.remote = true ;
            return ctx ;
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

        if(config.containsKey("response")){
            ContextUtils.makeApiResponse((Map<String, Object>) config.get("response"), ctx);
        }

        ctx.init() ;

        return ctx ;
    }


}
