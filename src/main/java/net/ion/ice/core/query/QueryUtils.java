package net.ion.ice.core.query;

import net.ion.ice.core.context.DBQueryTerm;
import net.ion.ice.core.context.DataQueryContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class QueryUtils {


    public static void makeQueryTerm(NodeType nodeType, QueryContext queryContext, List<QueryTerm> queryTerms, String paramName, String value) {
        value = value.equals("@sysdate") ? new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()) : value.equals("@sysday") ? new SimpleDateFormat("yyyyMMdd").format(new Date()) : value;

        if (paramName.equals("fields") || paramName.equals("pids") || paramName.equals("response")) {
            return;
        }

        if (paramName.equals("page")) {
            queryContext.setCurrentPage(value);
            return;
        } else if (paramName.equals("pageSize")) {
            queryContext.setPageSize(value);
            return;
        } else if (paramName.equals("count") || paramName.equals("limit")) {
            queryContext.setLimit(value);
            return;
        } else if (paramName.equals("query")) {
            try {
                Map<String, Object> query = JsonUtils.parsingJsonToMap(value);

            } catch (IOException e) {
            }
        }

        if (nodeType == null) {
            if (paramName.equals("sorting")) {
                queryContext.setSorting(value);
                return;
            } else if (paramName.contains("_")) {
                String fieldId = StringUtils.substringBeforeLast(paramName, "_");
                queryTerms.add(new QueryTerm(queryContext.getQueryTermType(), fieldId, StringUtils.substringAfterLast(paramName, "_"), value));
            } else {
                queryTerms.add(new QueryTerm(queryContext.getQueryTermType(), paramName,  value));
            }
        } else {
            if (paramName.equals("sorting")) {
                queryContext.setSorting(value, nodeType);
                return;
            } else if (paramName.contains("_")) {
                String fieldId = StringUtils.substringBeforeLast(paramName, "_");
                String method = StringUtils.substringAfterLast(paramName, "_");
                QueryTerm queryTerm = QueryUtils.makePropertyQueryTerm(queryContext.getQueryTermType(), nodeType, fieldId, method, value);
                if (queryTerm == null) {
                    queryTerm = QueryUtils.makePropertyQueryTerm(queryContext.getQueryTermType(), nodeType, paramName, null, value);
                }

                if (queryTerm != null) {
                    queryTerms.add(queryTerm);
                }

            } else {
                QueryTerm queryTerm = QueryUtils.makePropertyQueryTerm(queryContext.getQueryTermType(), nodeType, paramName, null, value);
                if (queryTerm != null) {
                    queryTerms.add(queryTerm);
                }
            }

        }
    }

    public static QueryTerm makePropertyQueryTerm(QueryTerm.QueryTermType queryTermType, NodeType nodeType, String fieldId, String method, String value) {
        if(queryTermType == QueryTerm.QueryTermType.DATA){
            return makeDataQueryTerm(nodeType, fieldId, method, value) ;
        }else{
            return makeNodeQueryTerm(nodeType, fieldId, method, value) ;
        }
    }

    public static List<QueryTerm> makeNodeQueryTerms(QueryTerm.QueryTermType queryTermType, Object q, NodeType nodeType) {
        List<QueryTerm> queryTerms = new ArrayList<>();
        if (q instanceof List) {
            for (Map<String, Object> _q : (List<Map<String, Object>>) q) {
                makeNodeQueryTerm(queryTermType, _q, nodeType, queryTerms);
            }
        } else if (q instanceof Map) {
            makeNodeQueryTerm(queryTermType, (Map<String, Object>) q, nodeType, queryTerms);
        }

        return queryTerms;
    }

    public static void makeNodeQueryTerm(QueryTerm.QueryTermType queryTermType, Map<String, Object> q, NodeType nodeType, List<QueryTerm> queryTerms) {
        if (q.containsKey("field") && q.containsKey("method")) {
            QueryTerm queryTerm = makePropertyQueryTerm(queryTermType, nodeType, q.get("field").toString(), q.get("method").toString(), q.get("value").toString());
            if (queryTerm != null) {
                queryTerms.add(queryTerm);
            }
        } else {
            for (String key : q.keySet()) {
                QueryTerm queryTerm = makePropertyQueryTerm(queryTermType, nodeType, key, null, q.get(key).toString());
                if (queryTerm != null) {
                    queryTerms.add(queryTerm);
                }
            }
        }
    }

    public static QueryTerm makeNodeQueryTerm(NodeType nodeType, String fieldId, String method, String value) {
        PropertyType propertyType = (PropertyType) nodeType.getPropertyType(fieldId);
        if (propertyType != null && propertyType.isIndexable()) {
            try {
                return new QueryTerm(fieldId, propertyType.getLuceneAnalyzer(), method, value);
            }catch (Exception e){
                return  null ;
            }
        }
        return null;
    }

    public static QueryTerm makeDataQueryTerm(NodeType nodeType, String fieldId, String method, String value) {
        PropertyType propertyType = (PropertyType) nodeType.getPropertyType(fieldId);
        if (propertyType != null) {
            return new QueryTerm(QueryTerm.QueryTermType.DATA, fieldId, method, value);
        }
        return null;
    }
}
