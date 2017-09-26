package net.ion.ice.core.context;

import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.*;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import net.ion.ice.core.query.ResultField;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.Cache;
import org.infinispan.query.SearchManager;

import javax.management.Query;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jaeho on 2017. 4. 26..
 */
public class QueryContext extends ReadContext {
    private static final Integer DEFAULT_PAGESIZE = 10;

    protected List<QueryTerm> queryTerms;
    protected List<QueryContext> joinQueryContexts ;
    protected String targetJoinField ;
    protected String sourceJoinField ;
    
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

        queryContext.makeQueryTerm(nodeType) ;

        queryContext.makeSearchFields() ;

        return queryContext;
    }

    public void makeQueryTerm(NodeType nodeType) {
        if(data == null) return  ;

        List<QueryTerm> queryTerms = new ArrayList<>();

        for (String key : data.keySet()) {
            QueryUtils.makeQueryTerm(nodeType, this, queryTerms, key, data.get(key));
        }

        setQueryTerms(queryTerms);
    }


    public static QueryContext createQueryContextFromText(String searchText, NodeType nodeType) {
        QueryContext queryContext = new QueryContext(nodeType);
//        queryContext.setIncludeReferenced(false);

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

    public static QueryContext createQueryContextFromTerms(List<QueryTerm> queryTerms, NodeType nodeType) {
        QueryContext queryContext = new QueryContext(nodeType);
        queryContext.setQueryTerms(queryTerms);
        return queryContext;
    }

    public void makeSearchFields(Map<String, Object> _config) {
        if(_config == null) return ;
        String searchFieldsStr = (String)  ContextUtils.getValue(_config.get("searchFields"), data);
        if(org.apache.commons.lang3.StringUtils.isEmpty(searchFieldsStr)) {
            return ;
        }

        String searchValue = (String) ContextUtils.getValue(_config.get("searchValue"), data);
        if(org.apache.commons.lang3.StringUtils.isEmpty(searchValue)) {
            return ;
        }
        makeSearchFields(searchFieldsStr, searchValue);
    }

    public void makeSearchFields() {
        if(data == null) return ;
        String searchFieldsStr = (String) data.get("searchFields");
        if(StringUtils.isEmpty(searchFieldsStr)) {
            return ;
        }

        String searchValue = (String) data.get("searchValue");
        if(StringUtils.isEmpty(searchValue)) {
            return ;
        }
        makeSearchFields(searchFieldsStr, searchValue);
    }

    protected void makeSearchFields(String searchFieldsStr, String searchValue) {
        if(StringUtils.isEmpty(searchFieldsStr)) {
            return ;
        }

        if(StringUtils.isEmpty(searchValue)) {
            return ;
        }

        this.searchFields = new ArrayList<>() ;

        for(String searchField : StringUtils.split(searchFieldsStr, ",")){
            searchField = searchField.trim() ;
            if(StringUtils.isNotEmpty(searchField)) {
                this.searchFields.add(searchField) ;
                QueryTerm queryTerm = QueryUtils.makePropertyQueryTerm(this.getQueryTermType(), this.nodeType, searchField, "matchingShould", searchValue);
                this.addQueryTerm(queryTerm);
            }
        }

        this.searchValue = searchValue ;
    }

    public void setQueryTerms(List<QueryTerm> queryTerms) {
        if(this.queryTerms == null) {
            this.queryTerms = queryTerms;
        }else{
            this.queryTerms.addAll(queryTerms) ;
        }
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
        if(StringUtils.isNotEmpty(sortingStr)) {
            this.sorting = sortingStr;
        }
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
        try {
            this.pageSize = Integer.valueOf(pageSize);
        }catch (NumberFormatException e){
            this.pageSize = DEFAULT_PAGESIZE ;
        }
        this.paging = true;
    }

    public void setCurrentPage(String page) {
        try {
            this.currentPage = Integer.valueOf(page);
        }catch (NumberFormatException e){
            this.currentPage = 1 ;
        }
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

        if (StringUtils.contains(value, Node.ID_SEPERATOR) && pt.isIgnoreHierarchyValue()) {
            value = StringUtils.substringAfterLast(value, Node.ID_SEPERATOR);
        }

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


    public List<Node> getQueryList() {
        if(this.queryTermType == QueryTerm.QueryTermType.DATA) {
            if(nodeBindingInfo == null){
                nodeBindingInfo = NodeUtils.getNodeBindingInfo(nodeType.getTypeId()) ;
            }
            List<Map<String, Object>> resultList = nodeBindingInfo.list(this);
            List<Node> resultNodeList = NodeUtils.initDataNodeList(nodeType.getTypeId(), resultList);

            return resultNodeList ;
        }else{
            List<Object> resultList = NodeUtils.getNodeService().executeQuery(this) ;
            List<Node> resultNodeList = NodeUtils.initNodeList(nodeType.getTypeId(), resultList) ;

            return resultNodeList ;
        }
    }

    public QueryResult makeQueryResult() {
        return makeQueryResult(result, null, ResultField.ResultType.LIST);
    }


    public QueryResult makeQueryResult(Object result, String fieldName, ResultField.ResultType resultType) {
        if(this.ifTest != null && !(this.ifTest.equalsIgnoreCase("true"))) {
            return null ;
        }
        if(this.remote != null && this.remote){
            Map<String, Object> queryResult = ClusterUtils.callQuery((ApiQueryContext) this) ;
            this.result = (List<Map<String, Object>>) queryResult.get("items");
            return new QueryResult(queryResult) ;
        }else {
            List<Node> resultNodeList = getQueryList();
            this.result = resultNodeList;

            return makeQueryResult(result, fieldName, resultType, resultNodeList);
        }
    }


    protected QueryResult makeQueryResult(Object result, String fieldName, ResultField.ResultType resultType, List<Node> resultNodeList) {
        NodeType nodeType = getNodetype() ;

        if(resultType != null && resultType == ResultField.ResultType.NONE){
            return null;
        }
        if(fieldName == null){
            fieldName = "items" ;
        }
        List<QueryResult> subList ;
        if(this.resultFields == null){
            subList = makeDefaultResult(nodeType, resultNodeList);
        }else{
            subList = makeResultList(nodeType, resultNodeList);
        }

        if(result == null){
            return makePaging(fieldName, subList);
        }
        if(resultType != null){
            if(resultType == ResultField.ResultType.NONE){
                return null;
            }else if(resultType == ResultField.ResultType.MERGE) {
                if(subList != null && subList.size() > 0) {
                    ((Map) result).putAll(subList.get(0));
                }
            }else if(resultType == ResultField.ResultType.READ){
                ((Map) result).put(fieldName, (subList != null && subList.size() > 0 ) ? subList.get(0) : null) ;
            }else{
                ((Map) result).put(fieldName, subList) ;
            }
            return (QueryResult) result;
        }else if(result instanceof Map){
            ((QueryResult) result).put(fieldName, subList) ;
            return (QueryResult) result;
        }
        return makePaging(fieldName, subList);
    }

    protected List<QueryResult> makeResultList(NodeType nodeType, List<Node> resultNodeList) {
        List<QueryResult> subList =  new ArrayList<>(resultNodeList.size()) ;

        Map<String, Object> contextData = new HashMap<>() ;
        contextData.putAll(data);

        for(Node resultNode : resultNodeList) {
            contextData.putAll(resultNode);
            QueryResult subQueryResult = new QueryResult() ;
            makeItemQueryResult(resultNode, subQueryResult, contextData);
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
                        subQueryContext.makeQueryResult(itemResult, "children", null);
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
            queryResult.put("pageCount", getResultSize() / getPageSize() + (getResultSize() % getPageSize() > 0 ? 1 : 0));
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

    public void addJoinQuery(QueryContext joinQueryContext) {
        if(joinQueryContext.getQueryTerms()!= null && joinQueryContext.getQueryTerms().size() > 0){
            if(this.joinQueryContexts == null){
                this.joinQueryContexts = new ArrayList<>();
            }
            this.joinQueryContexts.add(joinQueryContext) ;
        }
    }

    public List<QueryContext> getJoinQueryContexts(){
        return joinQueryContexts ;
    }
    

    public Integer getLimit() {
        return getMaxResultSize();
    }

    public Integer getOffset() {
        return getStart();
    }

    public void setTargetJoinField(String targetJoinField) {
        this.targetJoinField = targetJoinField;
    }

    public void setSourceJoinField(String sourceJoinField) {
        this.sourceJoinField = sourceJoinField;
    }

    public String getTargetJoinField() {
        return targetJoinField;
    }

    public String getSourceJoinField() {
        return sourceJoinField;
    }

    protected void addQueryTerm(QueryTerm queryTerm) {
        if(queryTerm == null) return ;
        if (this.queryTerms == null) {
            this.queryTerms = new ArrayList<>();
        }
        this.queryTerms.add(queryTerm);
    }

}
