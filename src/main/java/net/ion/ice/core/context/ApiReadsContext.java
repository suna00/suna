package net.ion.ice.core.context;

import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiReadsContext extends ApiReadContext{
    protected List<String> ids ;



    public static ApiReadsContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ApiReadsContext readsContext = new ApiReadsContext();
        readsContext.nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data)) ;
        readsContext.config = config ;
        readsContext.data = data ;

        for(String key : config.keySet()) {
            if(key.equals("typeId")) continue ;

            if(key.equals("ids")){
                String idsStr = (String) ContextUtils.getValue(config.get(key), data) ;
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


    public QueryResult makeQueryResult() {
        return makeQueryResult(result, null, resultType);
    }

}
