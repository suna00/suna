package net.ion.ice.core.context;

import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 8. 9..
 */
public class ApiQueryContext extends QueryContext{
    protected Map<String, Object> config  ;


    public ApiQueryContext(NodeType nodeType) {
        super(nodeType) ;
    }

    public ApiQueryContext() {
        super();
    }

    public QueryResult makeQueryResult() {
        return makeQueryResult(result, null, resultType);
    }

    public static ApiQueryContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        NodeType nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data)) ;
        ApiQueryContext queryContext = new ApiQueryContext(nodeType);

        queryContext.config = config ;
        queryContext.data = data ;

        if(ClusterUtils.getClusterService().checkClusterGroup(nodeType)){
            queryContext.remote = true ;
            return queryContext ;
        }

        for(String key : config.keySet()) {
            if(key.equals("typeId")) continue ;

            if(key.equals("query")){
                List<QueryTerm> queryTerms = QueryUtils.makeNodeQueryTerms(queryContext, config.get("query"), queryContext.nodeType);
                queryContext.setQueryTerms(queryTerms);
//                queryContext.makeSearchFields((Map<String, Object>) config.get("query"));
            }else {
                makeApiContext(config, queryContext, key);
            }
        }
        return queryContext;
    }


    public Map<String, Object> getConfig() {
        return config;
    }
}
