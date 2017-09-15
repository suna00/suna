package net.ion.ice.core.query;

import net.ion.ice.core.context.*;
import net.ion.ice.core.infinispan.lucene.AnalyzerFactory;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class QueryUtils {


    public static void makeQueryTerm(NodeType nodeType, QueryContext queryContext, List<QueryTerm> queryTerms, String paramName, Object val) {
        if(val == null) return ;
        String value = val.toString() ;
        value = value.equals("@sysdate") ? new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) : value.equals("@sysday") ? new SimpleDateFormat("yyyyMMdd").format(new Date()) : value;

        value = ContextUtils.makeContextConfig(queryContext, paramName, value);
        if (value == null) return;

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

    public static List<QueryTerm> makeNodeQueryTerms(QueryContext context, Object q, NodeType nodeType) {
        List<QueryTerm> queryTerms = new ArrayList<>();
        if (q instanceof List) {
            for (Map<String, Object> _q : (List<Map<String, Object>>) q) {
                makeNodeQueryTerm(context, _q, nodeType, queryTerms);
                context.makeSearchFields(_q);

            }
        } else if (q instanceof Map) {
            makeNodeQueryTerm(context, (Map<String, Object>) q, nodeType, queryTerms);
            context.makeSearchFields((Map<String, Object>) q);
        }

        return queryTerms;
    }

    public static QueryTerm makePropertyQueryTerm(QueryTerm.QueryTermType queryTermType, NodeType nodeType, String fieldId, String method, String value) {
        if(StringUtils.isEmpty(value)) return null ;
        if(queryTermType == QueryTerm.QueryTermType.DATA){
            return makeDataQueryTerm(nodeType, fieldId, method, value) ;
        }else{
            return makeNodeQueryTerm(nodeType, fieldId, method, value) ;
        }
    }

    public static void makeNodeQueryTerm(QueryContext context, Map<String, Object> q, NodeType nodeType, List<QueryTerm> queryTerms) {
        if (q.containsKey("field") && q.containsKey("method")) {
            String field = q.get("field").toString();
            String method = q.get("method").toString();
            String queryValue = (String) ContextUtils.getValue(q.get("value"), context.getData());

            if (method.equals("hasReferenced")) {
                NodeType refNodeType = NodeUtils.getNodeType(nodeType.getPropertyType(field).getReferenceType());
                QueryContext joinQueryContext = QueryContext.createQueryContextFromText(queryValue, refNodeType);
                if (joinQueryContext != null) {
                    joinQueryContext.setTargetJoinField(nodeType.getPropertyType(field).getReferenceValue());
                    joinQueryContext.setSourceJoinField("id");
                    context.addJoinQuery(joinQueryContext);
                }
            } else if (method.equals("referenceJoin")) {
                NodeType refNodeType = NodeUtils.getNodeType(nodeType.getPropertyType(field).getReferenceType());
                QueryContext joinQueryContext = QueryContext.createQueryContextFromText(queryValue, refNodeType);
                if (joinQueryContext != null) {
                    joinQueryContext.setTargetJoinField("id");
                    joinQueryContext.setSourceJoinField(field);
                    context.addJoinQuery(joinQueryContext);
                }
            } else {
                QueryTerm queryTerm = makePropertyQueryTerm(context.getQueryTermType(), nodeType, q.get("field").toString(), method, queryValue);
                if (queryTerm != null) {
                    queryTerms.add(queryTerm);
                }
            }
        } else if(q.containsKey("parameters")){
            context.makeQueryTerm(nodeType);
        } else {
            for (String key : q.keySet()) {
                makeQueryTerm(nodeType, context, queryTerms, key, (String) ContextUtils.getValue(q.get(key).toString(), context.getData()));
            }
        }
    }

    public static QueryTerm makeNodeQueryTerm(NodeType nodeType, String fieldId, String method, String value) {
        if(fieldId.equals("id")){
            return new QueryTerm(fieldId, AnalyzerFactory.getAnalyzer("code"), method, value, PropertyType.ValueType.STRING);
        }
        PropertyType propertyType = nodeType.getPropertyType(fieldId);
        if(propertyType == null && fieldId.contains("_")){
            propertyType = nodeType.getPropertyType(StringUtils.substringBeforeLast(fieldId, "_"));
        }
        if (propertyType != null && propertyType.isIndexable()) {
            try {
                if(StringUtils.isNotEmpty(propertyType.getCodeFilter())){
                    if(StringUtils.contains(value, ">")) {
                        return new QueryTerm(fieldId, propertyType.getLuceneAnalyzer(), method, value, propertyType.getValueType());
                    }else{
                        return new QueryTerm(fieldId, propertyType.getLuceneAnalyzer(), method, propertyType.getCodeFilter() + ">" + value, propertyType.getValueType());
                    }
                }else {
                    return new QueryTerm(fieldId, propertyType.getLuceneAnalyzer(), method, value, propertyType.getValueType());
                }
            }catch (Exception e){
                return  null ;
            }
        }
        return null;
    }


    public static QueryTerm makeDataQueryTerm(NodeType nodeType, String fieldId, String method, String value) {
        PropertyType propertyType = (PropertyType) nodeType.getPropertyType(fieldId);
        if (propertyType != null) {
            return new QueryTerm(QueryTerm.QueryTermType.DATA, fieldId, method, value, propertyType.getValueType());
        }
        return null;
    }
}
