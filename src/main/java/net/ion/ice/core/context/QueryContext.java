package net.ion.ice.core.context;

import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.*;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
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
public class QueryContext extends ReadContext {
    protected List<QueryTerm> queryTerms;
    protected List<DBQueryTerm> dbQueryTermList;

    protected SearchManager searchManager;
    protected String sorting;
    protected Integer pageSize;
    protected Integer currentPage;
    protected Integer maxSize;
    protected Integer resultSize;

    protected boolean paging;
    protected boolean limit;

    protected boolean treeable;
    protected int queryListSize;

    protected QueryTerm.QueryTermType queryTermType ;

    public QueryContext(NodeType nodeType) {
        this.nodeType = nodeType;
        if(this.nodeType.getRepositoryType().equals("node")){
            this.queryTermType = QueryTerm.QueryTermType.NODE ;
        }else if(this.nodeType.getRepositoryType().equals("data")){
            this.queryTermType = QueryTerm.QueryTermType.DATA ;
        }
    }

    public QueryContext() {
    }

    public static QueryContext createQueryContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        QueryContext queryContext = new QueryContext(nodeType);
        ReadContext.makeContextFromParameter(parameterMap, nodeType, queryContext);

        queryContext.queryTermType = QueryTerm.QueryTermType.NODE ;
        makeQueryTerm(nodeType, queryContext) ;

        return queryContext;
    }

    protected static void makeQueryTerm(NodeType nodeType, QueryContext context) {
        if(context.data == null) return  ;

        List<QueryTerm> queryTerms = new ArrayList<>();

        for (String key : context.data.keySet()) {
            QueryUtils.makeQueryTerm(nodeType, context, queryTerms, key, (String) context.data.get(key));
        }

        context.setQueryTerms(queryTerms);
    }


    public static QueryContext createQueryContextFromText(String searchText, NodeType nodeType) {
        QueryContext queryContext = new QueryContext(nodeType);
        queryContext.setIncludeReferenced(false );

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

                QueryUtils.makeQueryTerm(nodeType, queryContext, queryTerms, paramName, value);
            }
        }
        queryContext.setQueryTerms(queryTerms);
        return queryContext;
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
    public void setLimit(String limit) {
        this.limit = true ;
        setMaxSize(limit) ;
    }

    public void setQueryListSize(int queryListSize) {
        this.queryListSize = queryListSize;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = Integer.valueOf(maxSize);
    }

    public int getMaxResultSize() {
        if (maxSize == null && currentPage == null && pageSize == null) {
            maxSize = 100;
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


    public static QueryContext makeQueryContextForReferenced(NodeType nodeType, PropertyType pt, Node node) {
        String refTypeId = pt.getReferenceType();
        NodeType refNodeType = NodeUtils.getNodeType(refTypeId);


        QueryContext queryContext = new QueryContext(refNodeType);
        List<QueryTerm> queryTerms = new ArrayList<>();

        if (refNodeType == null) {
            throw new RuntimeException("REFERENCED NODE TYPE is Null : " + nodeType.getTypeId() + "." + pt.getPid() + " = " + refTypeId);
        }

        if (StringUtils.isNotEmpty(pt.getReferenceValue())) {
            QueryUtils.makeQueryTerm(refNodeType, queryContext, queryTerms, pt.getReferenceValue(), node.getId().toString());
        }else {
            List<String> idPids = refNodeType.getIdablePIds();
            if (idPids != null && idPids.size() > 1) {
                QueryUtils.makeQueryTerm(refNodeType, queryContext, queryTerms, idPids.get(0), node.getId().toString());
            } else {
                for (PropertyType refPt : refNodeType.getReferencePropertyTypes()) {
                    if (nodeType.getTypeId().equals(refPt.getReferenceType())) {
                        QueryUtils.makeQueryTerm(refNodeType, queryContext, queryTerms, refPt.getPid(), node.getId().toString());
                    }
                }
            }
        }

        queryContext.setQueryTerms(queryTerms);
        queryContext.setIncludeReferenced(true);
        return queryContext;

    }

    public static QueryContext makeQueryContextForTree(NodeType nodeType, PropertyType pt, String value) {
        QueryContext queryContext = new QueryContext(nodeType);
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        QueryUtils.makeQueryTerm(nodeType, queryContext, queryTerms, pt.getPid()+"_matching", value);

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

        QueryUtils.makeQueryTerm(refNodeType, queryContext, queryTerms, idPids.get(0), value);

        queryContext.setQueryTerms(queryTerms);
        return queryContext;

    }

    public Integer getResultSize() {
        return resultSize;
    }

    public void setResultSize(Integer resultSize) {
        this.resultSize = resultSize;
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
                List<QueryTerm> queryTerms = QueryUtils.makeNodeQueryTerms(queryContext.queryTermType, queryData.get("q"), queryContext.getNodetype());
                queryContext.setQueryTerms(queryTerms);
            } else if (key.equals("query")) {
                List<QueryTerm> queryTerms = QueryUtils.makeNodeQueryTerms(queryContext.queryTermType, queryData.get("query"), queryContext.getNodetype());
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





    public QueryResult makeQueryResult(Object result, String fieldName) {
        List<Object> resultList = NodeUtils.getNodeService().executeQuery(this) ;
        List<Node> resultNodeList = NodeUtils.initNodeList(nodeType.getTypeId(), resultList) ;

        return makeQueryResult(result, fieldName, resultNodeList);
    }

    protected QueryResult makeQueryResult(Object result, String fieldName, List<Node> resultNodeList) {
        NodeType nodeType = getNodetype() ;
        Node node = null ;

        if(result instanceof Node){
            node = (Node) result;
        }

        if(node == null){
            fieldName = "items" ;
        }

        if(this.resultFields == null){
            List<QueryResult> subList = makeDefaultResult(nodeType, resultNodeList);
            if(node != null) {
                node.put(fieldName, subList);
                return null;
            }if(result != null && result instanceof QueryResult){
                ((QueryResult) result).put(fieldName, subList) ;
                return null ;
            }else {
                return makePaging(fieldName, subList);
            }
        }

        List<QueryResult> subList = makeResultList(nodeType, resultNodeList);
        if(result != null && result instanceof QueryResult){
            ((QueryResult) result).put(fieldName, subList) ;
            return (QueryResult) result;
        }
        return makePaging(fieldName, subList);
    }

    protected List<QueryResult> makeResultList(NodeType nodeType, List<Node> resultNodeList) {
        List<QueryResult> subList =  new ArrayList<>(resultNodeList.size()) ;

        for(Node resultNode : resultNodeList) {
            QueryResult subQueryResult = new QueryResult() ;
            for (ResultField resultField : getResultFields()) {
                if (resultField.getContext() != null) {
                    QueryContext subQueryContext = (QueryContext) resultField.getContext();
                    subQueryContext.makeQueryResult(subQueryResult, resultField.getFieldName());
                } else {
                    String fieldValue = resultField.getFieldValue();
                    fieldValue = fieldValue == null || StringUtils.isEmpty(fieldValue) ? resultField.getFieldName() : fieldValue;
                    subQueryResult.put(resultField.getFieldName(), NodeUtils.getResultValue(this, nodeType.getPropertyType(fieldValue), resultNode));
                }
            }
            subList.add(subQueryResult) ;
        }
        return subList;
    }

    protected List<QueryResult> makeDefaultResult(NodeType nodeType, List<Node> resultNodeList) {
        List<QueryResult> subList =  new ArrayList<>(resultNodeList.size()) ;

        for(Node resultNode : resultNodeList) {
            QueryResult itemResult = new QueryResult() ;
            for(PropertyType pt : nodeType.getPropertyTypes()){
                itemResult.put(pt.getPid(), NodeUtils.getResultValue(this, pt, resultNode));
            }
            if (isTreeable()) {
                for (PropertyType pt : nodeType.getPropertyTypes()) {
                    if (pt.isTreeable()) {
                        QueryContext subQueryContext = QueryContext.makeQueryContextForTree(nodeType, pt, resultNode.getId().toString());
                        subQueryContext.setTreeable(true);
                        subQueryContext.makeQueryResult(itemResult, "children");
                    }
                }
            }
            subList.add(itemResult) ;
        }
        return subList ;
    }

    protected QueryResult makePaging(String fieldName, List<?> list) {
        QueryResult queryResult = new QueryResult() ;
        queryResult.put("result", "200") ;
        queryResult.put("resultMessage", "SUCCESS") ;
        makePaging(queryResult, fieldName, list);
        return queryResult ;
    }

    protected QueryResult makePaging(QueryResult queryResult, String fieldName, List<?> list) {
        queryResult.put("totalCount", getResultSize()) ;
        queryResult.put("resultCount", list.size()) ;
        if(isPaging()) {
            queryResult.put("pageSize", getPageSize());
            queryResult.put("pageCount", getResultSize() / getPageSize() + 1);
            queryResult.put("currentPage", getCurrentPage());
        }else if(limit){
            queryResult.put("more", resultSize > queryListSize);
            queryResult.put("moreCount", resultSize - queryListSize);
        }
        queryResult.put(fieldName, list) ;
        return queryResult ;
    }


    public QueryTerm.QueryTermType getQueryTermType() {
        return queryTermType;
    }
}
