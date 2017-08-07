package net.ion.ice.core.context;

import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.*;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.Cache;
import org.infinispan.query.SearchManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jaeho on 2017. 4. 26..
 */
public class QueryContext implements Context {
    protected NodeType nodeType;
    protected List<QueryTerm> queryTerms;
    protected SearchManager searchManager;
    protected String sorting;
    protected Integer pageSize;
    protected Integer currentPage;
    protected Integer maxSize;
    protected Integer resultSize;

    protected boolean paging;

    protected boolean includeReference;

    protected boolean treeable;

    protected List<ResultField> resultFields;

    public QueryContext(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public QueryContext() {
    }

    public static QueryContext makeQueryContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        QueryContext queryContext = new QueryContext(nodeType);
        List<QueryTerm> queryTerms = new ArrayList<>();

        if (parameterMap == null || parameterMap.size() == 0) {
            queryContext.setIncludeReference(true);
            return queryContext;
        }

        Map<String, Object> data = ContextUtils.makeContextData(parameterMap);

        for (String key : data.keySet()) {
            makeQueryTerm(nodeType, queryContext, queryTerms, key, (String) data.get(key));
        }

        queryContext.setQueryTerms(queryTerms);

        if(data.containsKey("fields")){
            makeResultField(queryContext, data, "fields");
        }else if(data.containsKey("pids")){
            makeResultField(queryContext, data, "pids");
        }

        if(queryContext.resultFields == null || queryContext.resultFields.size() == 0 ){
            queryContext.setIncludeReference(true);
        }

        return queryContext;
    }

    private static void makeResultField(QueryContext queryContext, Map<String, Object> data, String parameterName) {
        String fields = (String) data.get(parameterName);
        if(StringUtils.contains(fields,",")) {
            addResultFields(queryContext, fields, ",");
        }else if(StringUtils.contains(fields," ")){
            addResultFields(queryContext, fields, " ");
        }else{
            if(StringUtils.isNotEmpty(fields)) {
                queryContext.addResultField(new ResultField(fields, fields));
            }
        }
    }

    private static void addResultFields(QueryContext queryContext, String fields, String s) {
        for (String field : StringUtils.split(fields, s)) {
            field = StringUtils.trim(field) ;
            if(StringUtils.isNotEmpty(field)) {
                queryContext.addResultField(new ResultField(field, field));
            }
        }
    }


    public static QueryContext makeQueryContextFromText(String searchText, NodeType nodeType) {
        QueryContext queryContext = new QueryContext(nodeType);
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        if (StringUtils.isEmpty(searchText)) {
            return queryContext;
        }

        for (String param : StringUtils.split(searchText, '&')) {
            if (StringUtils.isNotEmpty(param) && StringUtils.contains(param, "=")) {
                String value = StringUtils.substringAfter(param, "=");
                if (StringUtils.isEmpty(value)) {
                    continue;
                }
                String paramName = StringUtils.substringBefore(param, "=");

                if (StringUtils.isEmpty(paramName)) {
                    continue;
                }

                makeQueryTerm(nodeType, queryContext, queryTerms, paramName, value);
            }
        }
        queryContext.setQueryTerms(queryTerms);
        return queryContext;
    }

    public static void makeQueryTerm(NodeType nodeType, QueryContext queryContext, List<QueryTerm> queryTerms, String paramName, String value) {
        value = value.equals("@sysdate") ? new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()) : value.equals("@sysday") ? new SimpleDateFormat("yyyyMMdd").format(new Date()) : value;

        if(paramName.equals("fields") || paramName.equals("pids") ||paramName.equals("response")){
            return ;
        }

        if (paramName.equals("page")) {
            queryContext.setCurrentPage(value);
            return;
        } else if (paramName.equals("pageSize")) {
            queryContext.setPageSize(value);
            return;
        } else if (paramName.equals("count")) {
            queryContext.setMaxSize(value);
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
                queryTerms.add(new QueryTerm(StringUtils.substringBeforeLast(paramName, "_"), StringUtils.substringAfterLast(paramName, "_"), value));
            } else {
                queryTerms.add(new QueryTerm(paramName, value));
            }
        } else {
            if (paramName.equals("sorting")) {
                queryContext.setSorting(value, nodeType);
                return;
            } else if (paramName.contains("_")) {
                String fieldId = StringUtils.substringBeforeLast(paramName, "_");
                String method = StringUtils.substringAfterLast(paramName, "_");
                QueryTerm queryTerm = makePropertyQueryTerm(nodeType, fieldId, method, value);
                if (queryTerm == null) {
                    queryTerm = makePropertyQueryTerm(nodeType, paramName, "matching", value);
                }

                if (queryTerm != null) {
                    queryTerms.add(queryTerm);
                }

            } else {
                queryTerms.add(makePropertyQueryTerm(nodeType, paramName, "matching", value));
            }

        }
    }


    public void setQueryTerms(List<QueryTerm> queryTerms) {
        this.queryTerms = queryTerms;
    }

    public List<QueryTerm> getQueryTerms() {
        return queryTerms;
    }

    public void setSearchManager(SearchManager searchManager) {
        this.searchManager = searchManager;
    }

    public SearchManager getSearchManager() {
        return searchManager;
    }

    public void setSorting(String sortingStr, NodeType nodeType) {
        this.sorting = sortingStr;
    }

    public void setSorting(String sortingStr) {
        this.sorting = sortingStr;
    }

    public String getSorting() {
        return sorting;
    }

    public boolean hasSorting() {
        return StringUtils.isNotBlank(sorting);
    }

    public boolean hasQueryTerms() {
        return queryTerms != null && queryTerms.size() > 0;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = Integer.valueOf(pageSize);
        this.paging = true;
    }

    public void setCurrentPage(String page) {
        this.currentPage = Integer.valueOf(page);
        this.paging = true;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = Integer.valueOf(maxSize);
    }

    public int getMaxResultSize() {
        if (maxSize == null && currentPage == null && pageSize == null) {
            maxSize = 1000;
            currentPage = 1;
            pageSize = maxSize;
            return maxSize;
        } else if (paging) {
            if (currentPage == null) {
                currentPage = 1;
            }
            if (pageSize == null) {
                pageSize = 10;
            }
            if (maxSize == null) {
                maxSize = pageSize;
            }
            this.paging = true;
            return pageSize * currentPage;
        } else {
            currentPage = 1;
            pageSize = maxSize;
            return maxSize;
        }
    }


    public Integer getPageSize() {
        return pageSize;
    }


    public Integer getStart() {
        if (paging) {
            return (currentPage - 1) * pageSize;
        }
        return 0;
    }

    public Integer getLast(Integer resultSize) {
        if (paging) {
            return resultSize;
        }
        return resultSize;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public boolean isPaging() {
        return paging;
    }

    public NodeType getNodetype() {
        return nodeType;
    }

    public static QueryContext makeQueryContextForReferenced(NodeType nodeType, PropertyType pt, Node node) {
        String refTypeId = pt.getReferenceType();
        NodeType refNodeType = NodeUtils.getNodeType(refTypeId);


        QueryContext queryContext = new QueryContext(refNodeType);
        List<QueryTerm> queryTerms = new ArrayList<>();

        if (refNodeType == null) {
            throw new RuntimeException("REFERENCED NODE TYPE is Null : " + nodeType.getTypeId() + "." + pt.getPid() + " = " + refTypeId);
        }

        if (StringUtils.isNotEmpty(pt.getReferenceValue())) {
            makeQueryTerm(refNodeType, queryContext, queryTerms, pt.getReferenceValue(), node.getId().toString());
        }else {
            List<String> idPids = refNodeType.getIdablePIds();
            if (idPids != null && idPids.size() > 1) {
                makeQueryTerm(refNodeType, queryContext, queryTerms, idPids.get(0), node.getId().toString());
            } else {
                for (PropertyType refPt : refNodeType.getReferencePropertyTypes()) {
                    if (nodeType.getTypeId().equals(refPt.getReferenceType())) {
                        makeQueryTerm(refNodeType, queryContext, queryTerms, refPt.getPid(), node.getId().toString());
                    }
                }
            }
        }

        queryContext.setQueryTerms(queryTerms);
        queryContext.setIncludeReference(true);
        return queryContext;

    }

    public static QueryContext makeQueryContextForTree(NodeType nodeType, PropertyType pt, String value) {
        QueryContext queryContext = new QueryContext(nodeType);
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        makeQueryTerm(nodeType, queryContext, queryTerms, pt.getPid(), value);

        queryContext.setQueryTerms(queryTerms);
        return queryContext;
    }

    public static QueryContext makeQueryContextForReferenceValue(NodeType nodeType, PropertyType pt, String value) {
        QueryContext queryContext = new QueryContext(nodeType);
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        String refTypeId = pt.getReferenceType();
        NodeType refNodeType = NodeUtils.getNodeType(refTypeId);

        if (refNodeType == null) {
            throw new RuntimeException("REFERENCE NODE TYPE is Null : " + nodeType.getTypeId() + "." + pt.getPid() + " = " + refTypeId);
        }

        List<String> idPids = refNodeType.getIdablePIds();

        if (idPids == null || idPids.size() == 0) {
            throw new RuntimeException("REFERENCE NODE TYPE has No ID : " + nodeType.getTypeId() + "." + pt.getPid() + " = " + refTypeId);
        }

        makeQueryTerm(refNodeType, queryContext, queryTerms, idPids.get(0), value);

        queryContext.setQueryTerms(queryTerms);
        return queryContext;

    }

    public Integer getResultSize() {
        return resultSize;
    }

    public void setResultSize(Integer resultSize) {
        this.resultSize = resultSize;
    }

    public boolean isIncludeReferenced() {
        return includeReference;
    }

    public void setIncludeReference(boolean includeReference) {
        this.includeReference = includeReference;
    }

    public boolean isTreeable() {
        return treeable;
    }

    public void setTreeable(boolean treeable) {
        this.treeable = treeable;
    }


    public static QueryContext makeQueryContextFromQuery(String query) {
        try {
            Map<String, Object> queryData = JsonUtils.parsingJsonToMap(query);
            QueryContext queryContext = new QueryContext();
            for (String key : queryData.keySet()) {
                queryContext.addResultField(new ResultField(key, makeQueryContextFromQueryData((Map<String, Object>) queryData.get(key))));
            }
            return queryContext;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static QueryContext makeQueryContextFromQueryData(Map<String, Object> queryData) {
        QueryContext queryContext = null;
        if (queryData.containsKey("typeId")) {
            queryContext = new QueryContext(NodeUtils.getNodeType((String) queryData.get("typeId")));
        } else {
            queryContext = new QueryContext();
        }
        for (String key : queryData.keySet()) {
            if (key.equals("typeId")) continue;

            if (key.equals("q")) {
                List<QueryTerm> queryTerms = makeNodeQueryTerms(queryData.get("q"), queryContext.getNodetype());
                queryContext.setQueryTerms(queryTerms);
            } else if (key.equals("query")) {
                List<QueryTerm> queryTerms = makeNodeQueryTerms(queryData.get("query"), queryContext.getNodetype());
                queryContext.setQueryTerms(queryTerms);
            } else {
                Object val = queryData.get(key);
                if (val == null) {
                    queryContext.addResultField(new ResultField(key, key));
                } else if (val instanceof String) {
                    queryContext.addResultField(new ResultField(key, (String) val));
                } else if (val instanceof Map) {
                    queryContext.addResultField(new ResultField(key, makeQueryContextFromQueryData((Map<String, Object>) val)));
                }
            }
        }
        return queryContext;
    }

    private void addResultField(ResultField resultField) {
        if (this.resultFields == null) {
            this.resultFields = new ArrayList<>();
        }
        this.resultFields.add(resultField);
    }

    private static List<QueryTerm> makeNodeQueryTerms(Object q, NodeType nodeType) {

        List<QueryTerm> queryTerms = new ArrayList<>();
        if (q instanceof List) {
            for (Map<String, Object> _q : (List<Map<String, Object>>) q) {
                makeNodeQueryTerm(_q, nodeType, queryTerms);
            }
        } else if (q instanceof Map) {
            makeNodeQueryTerm((Map<String, Object>) q, nodeType, queryTerms);
        }

        return queryTerms;
    }

    private static void makeNodeQueryTerm(Map<String, Object> q, NodeType nodeType, List<QueryTerm> queryTerms) {
        if (q.containsKey("field") && q.containsKey("method")) {
            QueryTerm queryTerm = makePropertyQueryTerm(nodeType, q.get("field").toString(), q.get("method").toString(), q.get("value").toString());
            if (queryTerm != null) {
                queryTerms.add(queryTerm);
            }
        } else {
            for (String key : q.keySet()) {
                QueryTerm queryTerm = makePropertyQueryTerm(nodeType, key, "matching", q.get(key).toString());
                if (queryTerm != null) {
                    queryTerms.add(queryTerm);
                }
            }
        }
    }

    public static QueryTerm makePropertyQueryTerm(NodeType nodeType, String fieldId, String method, String value) {
        PropertyType propertyType = (PropertyType) nodeType.getPropertyType(fieldId);
        if (propertyType != null && propertyType.isIndexable()) {
            return new QueryTerm(fieldId, propertyType.getLuceneAnalyzer(), method, value);
        }
        return null;
    }

    public List<ResultField> getResultFields() {
        return resultFields;
    }

    public static QueryContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        QueryContext queryContext = null;
        if (config.containsKey("typeId")) {
            queryContext = new QueryContext(NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data)));
        } else {
            queryContext = new QueryContext();
        }


//        for(String key : queryData.keySet()) {
//            if(key.equals("typeId")) continue ;
//
//            if (key.equals("q")) {
//                List<QueryTerm> queryTerms = makeNodeQueryTerms(queryData.get("q"), queryContext.getNodetype());
//                queryContext.setQueryTerms(queryTerms);
//            }else if(key.equals("query")){
//                List<QueryTerm> queryTerms = makeNodeQueryTerms(queryData.get("query"), queryContext.getNodetype());
//                queryContext.setQueryTerms(queryTerms);
//            }else {
//                Object val = queryData.get(key) ;
//                if(val == null){
//                    queryContext.addResultField(new ResultField(key, key)) ;
//                }else if(val instanceof String){
//                    queryContext.addResultField(new ResultField(key, (String) val)) ;
//                }else if(val instanceof Map){
//                    queryContext.addResultField(new ResultField(key, makeQueryContextFromQueryData((Map<String, Object>) val))) ;
//                }
//            }
//        }
        return queryContext;
    }

    public QueryResult makeQueryResult(Object result, String fieldName) {
        NodeType nodeType = getNodetype() ;
        Node node = null ;

        if(result instanceof Node){
            node = (Node) result;
        }

        if(node == null){
            fieldName = "items" ;
        }

        List<Object> resultList = NodeUtils.getNodeService().executeQuery(this) ;
        List<Node> resultNodeList = NodeUtils.initNodeList(nodeType.getTypeId(), resultList) ;


        if(this.resultFields == null){
            for(Node resultNode : resultNodeList) {
                if (isIncludeReferenced()) {
                    for (PropertyType pt : nodeType.getPropertyTypes(PropertyType.ValueType.REFERENCED)) {
                        QueryContext subQueryContext = QueryContext.makeQueryContextForReferenced(nodeType, pt, resultNode);
                        subQueryContext.makeQueryResult(resultNode, pt.getPid());
                    }
                }
                if (isTreeable()) {
                    for (PropertyType pt : nodeType.getPropertyTypes()) {
                        if (pt.isTreeable()) {
                            QueryContext subQueryContext = QueryContext.makeQueryContextForTree(nodeType, pt, resultNode.getId().toString());
                            subQueryContext.setTreeable(true);
                            subQueryContext.makeQueryResult(resultNode, "children");
                        }
                    }
                }
            }
            if(node != null){
                node.put(fieldName, resultNodeList) ;
                return null ;
            }else {
                return makePaging(fieldName, resultNodeList);
            }
        }

        List<QueryResult> subList =  new ArrayList<>(resultNodeList.size()) ;

        for(Node resultNode : resultNodeList) {
            QueryResult subQueryResult = new QueryResult() ;
            for (ResultField resultField : getResultFields()) {
                if (resultField.getQueryContext() != null) {
                    QueryContext subQueryContext = resultField.getQueryContext();
                    subQueryContext.makeQueryResult(subQueryResult, resultField.getFieldName());
                } else {
                    String fieldValue = resultField.getFieldValue();
                    fieldValue = fieldValue == null || StringUtils.isEmpty(fieldValue) ? resultField.getFieldName() : fieldValue;
                    subQueryResult.put(resultField.getFieldName(), NodeUtils.getResultValue(resultNode.get(fieldValue), nodeType.getPropertyType(fieldValue), resultNode));
                }
            }
            subList.add(subQueryResult) ;
        }
        if(result != null && result instanceof QueryResult){
            ((QueryResult) result).put(fieldName, subList) ;
            return (QueryResult) result;
        }
        return makePaging(fieldName, subList);
    }

    private QueryResult makePaging(String fieldName, List<?> list) {
        QueryResult queryResult = new QueryResult() ;
        queryResult.put("result", "200") ;
        queryResult.put("resultMessage", "SUCCESS") ;
        queryResult.put("totalCount", getResultSize()) ;
        queryResult.put("resultCount", list.size()) ;
        if(isPaging()) {
            queryResult.put("pageSize", getPageSize());
            queryResult.put("pageCount", getResultSize() / getPageSize() + 1);
            queryResult.put("currentPage", getCurrentPage());
        }
        queryResult.put(fieldName, list) ;
        return queryResult ;
    }

}
