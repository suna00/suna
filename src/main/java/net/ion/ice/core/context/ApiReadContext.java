package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class ApiReadContext extends ReadContext{

    protected Map<String, Object> config  ;

    protected ResultField.ResultType resultType ;

    public ApiReadContext() {
        super();
    }

    public QueryResult makeQueryResult() {
        return makeQueryResult(result, null, resultType);
    }


    public static ApiReadContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ApiReadContext readContext = new ApiReadContext();
        readContext.nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data)) ;
        readContext.config = config ;
        readContext.data = data ;

        for(String key : config.keySet()) {
            if(key.equals("typeId")) continue ;

            if(key.equals("id")){
                readContext.id = (String) ContextUtils.getValue(config.get(key), data) ;
            }else if(key.equals("response")){
                ContextUtils.makeApiResponse((Map<String, Object>) config.get(key), readContext);
            }else if(config.get(key) != null){
                ContextUtils.makeContextConfig(readContext, key, config.get(key).toString());
            }else if(key.equals("resultType")){
                readContext.resultType = ResultField.ResultType.valueOf(config.get("resultType").toString().toUpperCase());
            }
        }

        return readContext;
    }


    public Node getNode(){
        return  NodeUtils.getNode(nodeType.getTypeId(), id) ;
    }


}
