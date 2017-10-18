package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiReadsContext extends ApiQueryContext implements CacheableContext{
    protected List<String> ids ;

    public QueryResult makeQueryResult() {
        if (cacheable != null && cacheable) {
            String cacheKey = makeCacheKey();
            return ContextUtils.makeCacheResult(cacheKey, this);
        }
        return makeIdsQueryResult();
    }


    public static ApiReadsContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ApiReadsContext readsContext = new ApiReadsContext();

        readsContext.nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data)) ;
        readsContext.config = config ;
        readsContext.data = data ;
        checkCacheable(config, data, readsContext) ;

        for(String key : config.keySet()) {
            if(key.equals("typeId")) continue ;

            if(key.equals("ids")){
                String idsStr = ContextUtils.getValue(config.get(key), data).toString() ;
                readsContext.ids = new ArrayList<>() ;
                for(String id : StringUtils.split(idsStr, ",")){
                    if(StringUtils.isNotEmpty(id)){
                        readsContext.ids.add(id.trim()) ;
                    }
                }
            }else {
                ApiReadContext.makeApiContext(config, readsContext, key);
            }
        }
        return readsContext;
    }

    public static ApiReadsContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ApiReadsContext readsContext = makeContextFromConfig(config, data) ;
        readsContext.httpRequest = httpRequest ;
        readsContext.httpResponse = httpResponse ;
        return readsContext ;
    }


    public QueryResult makeIdsQueryResult() {
        List<Node> nodes = new ArrayList<>() ;

        for(String _id : ids){
            Node _node = NodeUtils.getNode(nodeType, _id ) ;
            if(_node != null) {
                nodes.add(_node);
            }
        }
        this.result = nodes;

        return makeQueryResult(result, null, resultType, nodes);
    }

    @Override
    public QueryResult makeCacheResult() {
        return makeIdsQueryResult();
    }

}
