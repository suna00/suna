package net.ion.ice.core.context;

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
    protected String responseType;
    protected String mergeField;


    public ApiQueryContext(NodeType nodeType) {
        super(nodeType) ;
    }

    public ApiQueryContext() {
        super();
    }



    public static ApiQueryContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ApiQueryContext queryContext = null;
        if (config.containsKey("typeId")) {
            queryContext = new ApiQueryContext(NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data)));
        } else {
            queryContext = new ApiQueryContext();
        }

        queryContext.config = config ;
        queryContext.data = data ;

        for(String key : config.keySet()) {
            if(key.equals("typeId")) continue ;

            if (key.equals("q")) {
                List<QueryTerm> queryTerms = QueryUtils.makeNodeQueryTerms(queryContext, config.get("q"), queryContext.nodeType);
                queryContext.setQueryTerms(queryTerms);
                queryContext.makeSearchFields((Map<String, Object>) config.get("q"));
            }else if(key.equals("query")){
                List<QueryTerm> queryTerms = QueryUtils.makeNodeQueryTerms(queryContext, config.get("query"), queryContext.nodeType);
                queryContext.setQueryTerms(queryTerms);
            }else if(key.equals("response")){
                Map<String, Object> response = (Map<String, Object>) config.get(key);

//                if(response.containsKey("merge")){
//                    queryContext.responseType = "merge" ;
//                    queryContext.mergeField = (String) response.get("merge");
//                }

                for(String fieldName : response.keySet()) {
                    Object fieldValue = response.get(fieldName) ;
                    if (fieldValue == null) {
                        queryContext.addResultField(new ResultField(fieldName, fieldName));
                    } else if (fieldValue instanceof String) {
                        if(StringUtils.isEmpty((String) fieldValue)){
                            queryContext.addResultField(new ResultField(fieldName, fieldName));
                        }else {
                            queryContext.addResultField(new ResultField(fieldName, (String) fieldValue));
                        }
                    } else if (fieldValue instanceof Map) {
                        if(((Map) fieldValue).containsKey("query")) {
                            queryContext.addResultField(new ResultField(fieldName, makeContextFromConfig((Map<String, Object>) fieldValue, data)));
                        }else if(((Map) fieldValue).containsKey("select")){
                            queryContext.addResultField(new ResultField(fieldName, ApiSelectContext.makeContextFromConfig((Map<String, Object>) fieldValue, data)));
                        }else{
                            queryContext.addResultField(new ResultField(fieldName, (Map<String, Object>) fieldValue));
                        }
                    }
                }
            }
        }
        return queryContext;
    }


}
