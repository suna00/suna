package net.ion.ice.core.context;

import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 8. 20..
 */
public class DataReadContext extends ReadContext{
    protected NodeBindingInfo nodeBindingInfo ;

    public static DataReadContext createContextFromParameter(NodeBindingInfo nodeBindingInfo, Map<String, String[]> parameterMap, NodeType nodeType, String id) {
        DataReadContext context = new DataReadContext();
        context.nodeType = nodeType ;
        context.nodeBindingInfo = nodeBindingInfo ;

        context.id = getId(parameterMap, nodeType, id);

        makeContextFromParameter(parameterMap, nodeType, context) ;

        return context ;
    }

    public QueryResult makeResult() {
        try {
            Map<String, Object> resultData = nodeBindingInfo.retrieve(id);
            Node node = new Node(resultData, nodeType.getTypeId());

            QueryResult itemResult = makeResult(node);

            QueryResult queryResult = new QueryResult();
            queryResult.put("result", "200");
            queryResult.put("resultMessage", "SUCCESS");
            queryResult.put("item", itemResult);

            return queryResult ;
        }catch(NotFoundNodeException e){
            QueryResult queryResult = new QueryResult();
            queryResult.put("result", "404");
            queryResult.put("resultMessage", "Not Found!");
            return queryResult ;

        }

    }
}
