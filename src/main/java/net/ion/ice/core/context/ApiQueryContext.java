package net.ion.ice.core.context;

import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import net.ion.ice.core.query.ResultField;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 8. 9..
 */
public class ApiQueryContext extends QueryContext implements CacheableContext{
    protected Map<String, Object> config  ;
    protected HttpServletRequest httpRequest ;
    protected HttpServletResponse httpResponse ;

    public ApiQueryContext(NodeType nodeType) {
        super(nodeType) ;
    }

    public ApiQueryContext() {
        super();
    }

    public QueryResult makeQueryResult() {
        if(cacheable != null && cacheable){
            String cacheKey = makeCacheKey() ;
            return ContextUtils.makeCacheResult(cacheKey, this) ;
        }
        return makeQueryResult(result, null, resultType);
    }


    public String makeCacheKey(){
        if(this.httpRequest != null) {
            String keySrc = httpRequest.getRequestURI() + "?" + httpRequest.getParameterMap().toString();
            return keySrc;
        }
        return this.config.toString() + this.data.toString() ;
    }

    public static ApiQueryContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        NodeType nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data)) ;
        ApiQueryContext queryContext = new ApiQueryContext(nodeType);
        queryContext.config = config ;
        queryContext.data = data ;

        checkCacheable(config, data, queryContext) ;
        checkExclude(config, data, queryContext); ;


        if(!ClusterUtils.getClusterService().checkClusterGroup(nodeType)){
            queryContext.remote = true ;
            if(config.containsKey("if")) {
                queryContext.ifTest = ContextUtils.getValue(config.get("if"), queryContext.data).toString();
            }

            if(config.containsKey("resultType")){
                queryContext.resultType = ResultField.ResultType.valueOf(config.get("resultType").toString().toUpperCase());
            }
            return queryContext ;
        }

        for(String key : config.keySet()) {
            if(key.equals("typeId")) continue ;

            if(key.equals("query")) {
                List<QueryTerm> queryTerms = QueryUtils.makeNodeQueryTerms(queryContext, config.get("query"), queryContext.nodeType);
                queryContext.setQueryTerms(queryTerms);
//                queryContext.makeSearchFields((Map<String, Object>) config.get("query"));
            }else if(key.equals("naviId")){
                queryContext.naviIdTerm = QueryUtils.makeNodeQueryTerms(queryContext, config.get("naviId"), queryContext.nodeType);
            }else {
                makeApiContext(config, queryContext, key);
            }
        }
        return queryContext;
    }

    public static ApiQueryContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ApiQueryContext queryContext = makeContextFromConfig(config, data) ;
        queryContext.httpRequest = httpRequest ;
        queryContext.httpResponse = httpResponse ;
        return queryContext ;
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
        return makeQueryResult(result, null, resultType);
    }
}
