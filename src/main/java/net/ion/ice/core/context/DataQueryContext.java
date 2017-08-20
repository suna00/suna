package net.ion.ice.core.context;

import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 8. 20..
 */
public class DataQueryContext extends QueryContext{
    protected NodeBindingInfo nodeBindingInfo ;


    public DataQueryContext(NodeBindingInfo nodeBindingInfo, NodeType nodeType) {
        this.nodeBindingInfo = nodeBindingInfo;
        this.nodeType = nodeType ;
    }

    public static DataQueryContext createQueryContextFromParameter(NodeBindingInfo nodeBindingInfo, Map<String, String[]> parameterMap, NodeType nodeType) {
        DataQueryContext queryContext = new DataQueryContext(nodeBindingInfo, nodeType);

        ReadContext.makeContextFromParameter(parameterMap, nodeType, queryContext);
        queryContext.queryTermType = QueryTerm.QueryTermType.DATA ;

        makeQueryTerm(nodeType, queryContext) ;

        return queryContext;
    }


    public QueryResult makeQueryResult(Object result, String fieldName) {
        List<Map<String, Object>> resultList = nodeBindingInfo.list(this);
        List<Node> resultNodeList = NodeUtils.initDataNodeList(nodeType.getTypeId(), resultList) ;

        return makeQueryResult(result, fieldName, resultNodeList);
    }

    public Integer getLimit() {
        return getMaxResultSize();
    }

    public Integer getOffset() {
        return getStart();
    }
}
