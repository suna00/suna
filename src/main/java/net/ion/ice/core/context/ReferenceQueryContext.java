package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryResult;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReferenceQueryContext extends QueryContext{
    protected PropertyType refPt ;


    public ReferenceQueryContext(NodeType nodeType) {
        super(nodeType);
    }

    public static ReferenceQueryContext makeQueryContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        ReferenceQueryContext queryContext = new ReferenceQueryContext(nodeType);

        makeContextFromParameter(parameterMap, nodeType, queryContext) ;

        if(parameterMap.containsKey("tid") && parameterMap.containsKey("pid")){
            NodeType refNt = NodeUtils.getNodeType(parameterMap.get("tid")[0]) ;
            queryContext.refPt = refNt.getPropertyType(parameterMap.get("pid")[0]) ;
        }

        return queryContext;
    }

    public QueryResult makeQueryResult(Object result, String fieldName) {
        NodeType nodeType = getNodetype() ;

        if(fieldName == null){
            fieldName = "items" ;
        }

        List<Object> resultList = NodeUtils.getNodeService().executeQuery(this) ;

        List<QueryResult> subList = new ArrayList<>();


        for(Object obj : resultList){
            Node node = (Node) obj;
            QueryResult code = new QueryResult() ;
            List<String> labelablePids = nodeType.getLabelablePIds();
            Object label = labelablePids.isEmpty() ? "" : node.get(labelablePids.get(0));

            code.put("refId", node.getId()) ;
            code.put("value", StringUtils.substringAfterLast(node.getId(), Node.ID_SEPERATOR));
            code.put("label", label) ;
        }
        return makePaging(fieldName, subList);
    }
}
