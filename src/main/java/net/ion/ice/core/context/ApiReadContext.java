package net.ion.ice.core.context;

import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class ApiReadContext extends ReadContext implements CacheableContext {

    protected Map<String, Object> config;
    protected HttpServletRequest httpRequest;
    protected HttpServletResponse httpResponse;


    public ApiReadContext() {
        super();
    }

    public QueryResult makeQueryResult() {
        if (cacheable != null && cacheable) {
            String cacheKey = makeCacheKey();
            return ContextUtils.makeCacheResult(cacheKey, this);
        }
        return makeQueryResult(result, null, resultType);
    }


    public String makeCacheKey() {
        String keySrc = httpRequest.getRequestURI() + "?" + httpRequest.getQueryString();
        return keySrc;
    }


    public static ApiReadContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ApiReadContext readContext = new ApiReadContext();
        readContext.nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data));
        readContext.config = config;
        readContext.data = data;
        if(!ClusterUtils.getClusterService().checkClusterGroup(readContext.nodeType)){
            readContext.remote = true ;
            return readContext ;
        }
        for (String key : config.keySet()) {
            if (key.equals("typeId")) continue;

            if (key.equals("id")) {
                readContext.id = ContextUtils.getValue(config.get(key), data).toString();
            } else {
                makeApiContext(config, readContext, key);
            }
        }

        return readContext;
    }

    public static ApiReadContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ApiReadContext readContext = makeContextFromConfig(config, data) ;
        readContext.httpRequest = httpRequest ;
        readContext.httpResponse = httpResponse ;
        return readContext ;
    }

    public Node getNode() {
        Node node = NodeUtils.getNode(nodeType.getTypeId(), id);
        this.result = node;
        return node;
    }

    @Override
    public String getCacheTime() {
        return cacheTime;
    }

    @Override
    public QueryResult makeCacheResult() {
        return makeQueryResult(result, null, resultType);
    }
}
