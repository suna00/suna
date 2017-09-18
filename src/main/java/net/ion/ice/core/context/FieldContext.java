package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.Reference;
import net.ion.ice.core.query.QueryResult;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Created by jaehocho on 2017. 8. 22..
 */
public class FieldContext extends ApiReadContext{

    public static FieldContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        FieldContext context = new FieldContext() ;

        context.nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data)) ;
        context.config = config ;
        context.data = data ;

        for(String key : config.keySet()) {
            if(key.equals("typeId")) continue ;
             makeApiContext(config, context, key);
        }

        return context;
    }

    public QueryResult makeQueryResult(Node refNode) {
        QueryResult queryResult = new QueryResult() ;
        queryResult.put("refId", refNode.getId()) ;
        queryResult.put("value", StringUtils.contains(refNode.getId(), ">") ? StringUtils.substringAfterLast(refNode.getId(), ">") : refNode.getId() ) ;
        queryResult.put("label", node.getLabel(this)) ;

        queryResult.put("item", makeResult(refNode)) ;
        return queryResult ;
    }
}
