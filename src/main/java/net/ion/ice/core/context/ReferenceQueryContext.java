package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReferenceQueryContext extends QueryContext{
    protected PropertyType refPt ;


    public ReferenceQueryContext(NodeType nodeType) {
        super(nodeType);
    }

    public static ReferenceQueryContext createQueryContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        ReferenceQueryContext queryContext = new ReferenceQueryContext(nodeType);
        makeContextFromParameter(parameterMap, nodeType, queryContext) ;

        if(parameterMap.containsKey("tid") && parameterMap.containsKey("pid")){
            NodeType refNt = NodeUtils.getNodeType(parameterMap.get("tid")[0]) ;
            queryContext.refPt = refNt.getPropertyType(parameterMap.get("pid")[0]) ;
        }

        queryContext.queryTermType = QueryTerm.QueryTermType.NODE ;
        queryContext.makeQueryTerm(nodeType) ;

        return queryContext;
    }

    public QueryResult makeQueryResult() {
        NodeType nodeType = getNodetype() ;

        List<Object> resultList = NodeUtils.getNodeService().executeQuery(this) ;

        List<QueryResult> subList = new ArrayList<>();


        for(Object obj : resultList){
            Node node = (Node) obj;
            QueryResult code = new QueryResult() ;

            code.put("refId", node.getId()) ;
            code.put("value", StringUtils.contains(node.getId(), Node.ID_SEPERATOR) ? StringUtils.substringAfterLast(node.getId(), Node.ID_SEPERATOR) : node.getId());
            code.put("label", node.getLabel( this)) ;

            subList.add(code) ;
        }
        return makePaging("items", subList);
    }
}
