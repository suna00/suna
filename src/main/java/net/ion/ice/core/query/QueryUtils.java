package net.ion.ice.core.query;

import net.ion.ice.core.context.*;
import net.ion.ice.core.infinispan.lucene.AnalyzerFactory;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;

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
                if(method.equals("facet")){
                    queryContext.addFacetTerm(new FacetTerm(fieldId, value));
                } else if (method.startsWith("hasReferenced")) {
                    NodeType refNodeType = NodeUtils.getNodeType(nodeType.getPropertyType(fieldId).getReferenceType());
                    QueryContext joinQueryContext = QueryContext.createQueryContextFromText(value, refNodeType, StringUtils.substringAfter(method, "hasReferenced"));
                    makeHasReferencedContext(queryContext, nodeType, fieldId, joinQueryContext);
                } else if (method.startsWith("referenceJoin")) {
                    NodeType refNodeType = NodeUtils.getNodeType(nodeType.getPropertyType(fieldId).getReferenceType());
                    QueryContext joinQueryContext = QueryContext.createQueryContextFromText(value, refNodeType, StringUtils.substringAfter(method, "referenceJoin"));
                    makeJoinContext(queryContext, fieldId, joinQueryContext);
                }
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

    private static void makeJoinContext(QueryContext queryContext, String fieldId, QueryContext joinQueryContext) {
        if (joinQueryContext != null) {
            joinQueryContext.setMaxSize("100000");
            joinQueryContext.setTargetJoinField("id");
            joinQueryContext.setSourceJoinField(fieldId);
            queryContext.addJoinQuery(joinQueryContext);
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

    public static QueryTerm makePropertyQueryTerm(NodeType nodeType, String fieldId, String method, String value) {
        if(StringUtils.isEmpty(value)) return null ;
        if(nodeType.isDataType()){
            return makeDataQueryTerm(nodeType, fieldId, method, value) ;
        }else{
            return makeNodeQueryTerm(nodeType, fieldId, method, value) ;
        }
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
            String field = (String) ContextUtils.getValue(q.get("field"), context.getData());
            String method = q.get("method").toString();

            Object value = ContextUtils.getValue(q.get("value"), context.getData());

            if (method.equals("facet")) {
                context.addFacetTerm(new FacetTerm(field, value));
                return;
            }

            if (value == null) return;
            String queryValue = value.toString();
            if (method.startsWith("hasReferenced")) {
                NodeType refNodeType = NodeUtils.getNodeType(nodeType.getPropertyType(field).getReferenceType());
                QueryContext joinQueryContext = QueryContext.createQueryContextFromText(queryValue, refNodeType, StringUtils.substringAfter(method, "hasReferenced"));
                makeHasReferencedContext(context, nodeType, field, joinQueryContext);
            } else if (method.startsWith("referenceJoin")) {
                NodeType refNodeType = NodeUtils.getNodeType(nodeType.getPropertyType(field).getReferenceType());
                QueryContext joinQueryContext = QueryContext.createQueryContextFromText(queryValue, refNodeType, StringUtils.substringAfter(method, "referenceJoin"));
                makeJoinContext(context, field, joinQueryContext);
            } else {
                QueryTerm queryTerm = makePropertyQueryTerm(context.getQueryTermType(), nodeType, field, method, queryValue);
                if (queryTerm != null) {
                    queryTerms.add(queryTerm);
                }
            }
        } else if(q.containsKey("field") && q.containsKey("value")){
            String field = (String) ContextUtils.getValue(q.get("field"), context.getData());
            Object value = ContextUtils.getValue(q.get("value"), context.getData());
            QueryTerm queryTerm = makePropertyQueryTerm(context.getQueryTermType(), nodeType, field, null, value.toString());
            if (queryTerm != null) {
                queryTerms.add(queryTerm);
            }
        } else if(q.containsKey("parameters")){
            context.makeQueryTerm(nodeType);
        } else {
            for (String key : q.keySet()) {
                makeQueryTerm(nodeType, context, queryTerms, key, ContextUtils.getValue(q.get(key).toString(), context.getData()));
            }
        }
    }

    private static void makeHasReferencedContext(QueryContext context, NodeType nodeType, String field, QueryContext joinQueryContext) {
        if (joinQueryContext != null) {
            joinQueryContext.setMaxSize("100000");
            if(nodeType.getTypeId().equals(nodeType.getPropertyType(field).getReferenceType())){
                joinQueryContext.setTargetJoinField(field);
            }else {
                joinQueryContext.setTargetJoinField(nodeType.getPropertyType(field).getReferenceValue());
            }
            joinQueryContext.setSourceJoinField("id");
            context.addJoinQuery(joinQueryContext);
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
        if(propertyType == null && fieldId.contains("_")){
            propertyType = nodeType.getPropertyType(StringUtils.substringBefore(fieldId, "_"));
        }
        if(propertyType == null && Node.NODE_VALUE_KEYS.contains(fieldId)){
            switch (fieldId){
                case "created": case "changed" :{
                    return new QueryTerm(fieldId, null, method, value, PropertyType.ValueType.DATE);
                }
                case "id": case "owner" : case "modifier" :{
                    return new QueryTerm(fieldId, AnalyzerFactory.getAnalyzer("code"), method, value, PropertyType.ValueType.STRING);
                }
            }
        }
        if (propertyType != null && propertyType.isIndexable()) {
            try {
                if(StringUtils.isNotEmpty(propertyType.getCodeFilter())){
                    if(StringUtils.contains(value, ">")) {
                        return new QueryTerm(fieldId, propertyType.getLuceneAnalyzer(), method, StringUtils.substringAfterLast(value, ">"), propertyType.getValueType());
                    }else{
//                        return new QueryTerm(fieldId, propertyType.getLuceneAnalyzer(), method, propertyType.getCodeFilter() + ">" + value, propertyType.getValueType());
                        return new QueryTerm(fieldId, propertyType.getLuceneAnalyzer(), method, value, propertyType.getValueType());
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
