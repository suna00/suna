package net.ion.ice.core.infinispan;

import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
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
    private Integer resultSize ;

    private boolean paging ;

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
            queryContext.setResultSize(value) ;
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

                queryTerms.add(new QueryTerm(paramName, value));
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

    public void setResultSize(String resultSize) {
        this.resultSize = Integer.valueOf(resultSize) ;
    }

    public int getMaxResultSize() {
        if(resultSize == null && currentPage == null && pageSize == null){
            resultSize = 1000 ;
            currentPage = 1 ;
            pageSize = resultSize ;
            return  resultSize ;
        }else if(paging){
            if(currentPage == null){
                currentPage = 1 ;
            }
            if(pageSize == null){
                pageSize = 10 ;
            }
            if(resultSize == null){
                resultSize = pageSize ;
            }
            this.paging = true ;
            return pageSize * currentPage ;
        }else{
            currentPage = 1 ;
            pageSize = resultSize ;
            return resultSize ;
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
}
