package net.ion.ice.core.infinispan;

import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.query.SearchManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 4. 26..
 */
public class QueryContext{
    private NodeType nodeType ;
    private List<QueryTerm> queryTerms ;
    private SearchManager searchManager;
    private String sorting;
    private Integer pageSize ;
    private Integer currentPage ;
    private Integer maxSize;
    private Integer resultSize ;

    private boolean paging ;

    private boolean includeReference ;

    private boolean treeable ;


    public QueryContext(NodeType nodeType) {
        this.nodeType = nodeType ;
    }

    public static QueryContext makeQueryContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        QueryContext queryContext = new QueryContext(nodeType) ;
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        if(parameterMap == null || parameterMap.size() == 0){
            return queryContext ;
        }

        for (String paramName : parameterMap.keySet()) {

            String[] values = parameterMap.get(paramName);
            if(values== null || StringUtils.isEmpty(values[0])){
                continue;
            }

            String value = StringUtils.join(values, ' ');
            makeQueryTerm(nodeType, queryContext, queryTerms, paramName, value);

        }
        queryContext.setQueryTerms(queryTerms);
        return queryContext ;
    }


    public static QueryContext makeQueryContextFromText(String searchText, NodeType nodeType) {
        QueryContext queryContext = new QueryContext(nodeType) ;
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        if(StringUtils.isEmpty(searchText)){
            return queryContext ;
        }

        for (String param : StringUtils.split(searchText, '&')) {
            if (StringUtils.isNotEmpty(param) && StringUtils.contains(param, "=")) {
                String value = StringUtils.substringAfter(param, "=");
                if(StringUtils.isEmpty(value)){
                    continue;
                }
                String paramName = StringUtils.substringBefore(param, "=") ;

                if(StringUtils.isEmpty(paramName)){
                    continue ;
                }

                makeQueryTerm(nodeType, queryContext, queryTerms, paramName, value);
            }
        }
        queryContext.setQueryTerms(queryTerms);
        return queryContext ;
    }

    public static void makeQueryTerm(NodeType nodeType, QueryContext queryContext, java.util.List<QueryTerm> queryTerms, String paramName, String value) {
        value = value.equals("@sysdate") ? new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()) : value.equals("@sysday") ? new SimpleDateFormat("yyyyMMdd").format(new Date()) : value;

        if(paramName.equals("page")){
            queryContext.setCurrentPage(value) ;
            return  ;
        }else if(paramName.equals("pageSize")){
            queryContext.setPageSize(value) ;
            return ;
        }else if(paramName.equals("count")){
            queryContext.setMaxSize(value) ;
            return ;
        }else if(paramName.equals("query")){
            try {
                Map<String, Object> query = JsonUtils.parsingJsonToMap(value) ;

            } catch (IOException e) {
            }
        }

        if(nodeType == null) {
            if (paramName.equals("sorting")) {
                queryContext.setSorting(value);
                return ;
            } else if (paramName.contains("_")) {
                String fieldId = StringUtils.substringBeforeLast(paramName, "_");
                queryTerms.add(new QueryTerm(StringUtils.substringBeforeLast(paramName, "_"), StringUtils.substringAfterLast(paramName, "_"), value));
            } else {
                queryTerms.add(new QueryTerm(paramName, value));
            }
        }else{
            if (paramName.equals("sorting")) {
                queryContext.setSorting(value, nodeType);
                return ;
            } else if (paramName.contains("_")) {
                String fieldId = StringUtils.substringBeforeLast(paramName, "_");
                String method = StringUtils.substringAfterLast(paramName, "_") ;
                QueryTerm queryTerm = makePropertyQueryTerm(nodeType, queryTerms, fieldId, method, value) ;
                if(queryTerm == null){
                    queryTerm = makePropertyQueryTerm(nodeType, queryTerms, paramName, "matching", value) ;
                }

                if(queryTerm != null ){
                    queryTerms.add(queryTerm) ;
                }

            } else {
                queryTerms.add( makePropertyQueryTerm(nodeType, queryTerms, paramName, "matching", value)) ;
            }

        }
    }

    public static QueryTerm makePropertyQueryTerm(NodeType nodeType, java.util.List<QueryTerm> queryTerms, String fieldId, String method, String value) {
        PropertyType propertyType = (PropertyType) nodeType.getPropertyType(fieldId);
        if(propertyType != null && propertyType.isIndexable()) {
            return new QueryTerm(fieldId, propertyType.getLuceneAnalyzer(), method, value);
        }
        return null ;
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
        this.sorting = sorting;
    }

    public void setSorting(String sortingStr) {
        this.sorting = sorting;
    }

    public String getSorting() {
        return sorting;
    }

    public boolean hasSorting(){
        return StringUtils.isNotBlank(sorting) ;
    }

    public boolean hasQueryTerms() {
        return queryTerms != null && queryTerms.size() > 0 ;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = Integer.valueOf(pageSize) ;
        this.paging = true ;
    }

    public void setCurrentPage(String page) {
        this.currentPage = Integer.valueOf(page) ;
        this.paging = true ;
    }

    public void setMaxSize(String maxSize) {
        this.maxSize = Integer.valueOf(maxSize) ;
    }

    public int getMaxResultSize() {
        if(maxSize == null && currentPage == null && pageSize == null){
            maxSize = 1000 ;
            currentPage = 1 ;
            pageSize = maxSize;
            return maxSize;
        }else if(paging){
            if(currentPage == null){
                currentPage = 1 ;
            }
            if(pageSize == null){
                pageSize = 10 ;
            }
            if(maxSize == null){
                maxSize = pageSize ;
            }
            this.paging = true ;
            return pageSize * currentPage ;
        }else{
            currentPage = 1 ;
            pageSize = maxSize;
            return maxSize;
        }
    }


    public Integer getPageSize() {
        return pageSize;
    }


    public Integer getCurrentPage() {
        return currentPage;
    }

    public boolean isPaging() {
        return paging;
    }

    public NodeType getNodetype(){
        return nodeType ;
    }

    public static QueryContext makeQueryContextForReferenced(NodeType nodeType, PropertyType pt, Node node) {
        QueryContext queryContext = new QueryContext(nodeType) ;
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        String refTypeId = pt.getReferenceType() ;
        NodeType refNodeType = NodeUtils.getNodeType(refTypeId) ;

        if(refNodeType == null ){
            throw new RuntimeException("REFERENCED NODE TYPE is Null : " + nodeType.getTypeId() + "." + pt.getPid() + " = " + refTypeId) ;
        }

        List<String> idPids = refNodeType.getIdablePIds() ;

        if(idPids == null || idPids.size() == 0){
            throw new RuntimeException("REFERENCED NODE TYPE has No ID : " + nodeType.getTypeId() + "." + pt.getPid() + " = " + refTypeId) ;
        }

        makeQueryTerm(refNodeType, queryContext, queryTerms, idPids.get(0), node.getId().toString());

        queryContext.setQueryTerms(queryTerms);
        return queryContext ;

    }

    public static QueryContext makeQueryContextForTree(NodeType nodeType, PropertyType pt, String value) {
        QueryContext queryContext = new QueryContext(nodeType) ;
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        makeQueryTerm(nodeType, queryContext, queryTerms, pt.getPid(), value);

        queryContext.setQueryTerms(queryTerms);
        return queryContext ;
    }

    public static QueryContext makeQueryContextForReferenceValue(NodeType nodeType, PropertyType pt, String value) {
        QueryContext queryContext = new QueryContext(nodeType) ;
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        String refTypeId = pt.getReferenceType() ;
        NodeType refNodeType = NodeUtils.getNodeType(refTypeId) ;

        if(refNodeType == null ){
            throw new RuntimeException("REFERENCED NODE TYPE is Null : " + nodeType.getTypeId() + "." + pt.getPid() + " = " + refTypeId) ;
        }

        List<String> idPids = refNodeType.getIdablePIds() ;

        if(idPids == null || idPids.size() == 0){
            throw new RuntimeException("REFERENCED NODE TYPE has No ID : " + nodeType.getTypeId() + "." + pt.getPid() + " = " + refTypeId) ;
        }

        makeQueryTerm(refNodeType, queryContext, queryTerms, idPids.get(0), value);

        queryContext.setQueryTerms(queryTerms);
        return queryContext ;

    }

    public Integer getResultSize() {
        return resultSize;
    }

    public void setResultSize(Integer resultSize) {
        this.resultSize = resultSize;
    }

    public boolean isIncludeReferenced() {
        return includeReference ;
    }

    public void setIncludeReference(boolean includeReference){
        this.includeReference = includeReference ;
    }

    public boolean isTreeable(){
        return treeable ;
    }

    public void setTreeable(boolean treeable){
        this.treeable = treeable ;
    }
}
