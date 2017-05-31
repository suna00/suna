package net.ion.ice.core.infinispan;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.query.SearchManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 4. 26..
 */
public class QueryContext{
    private List<QueryTerm> queryTerms ;
    private SearchManager searchManager;
    private String sorting;

    public QueryContext() {
    }

    public static QueryContext makeQueryContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        QueryContext queryContext = new QueryContext() ;
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        if(parameterMap == null || parameterMap.size() == 0){
            return queryContext ;
        }

        for (String paramName : parameterMap.keySet()) {

            String[] values = parameterMap.get(paramName);
            if(values== null || StringUtils.isEmpty(values[0])){
                continue;
            }

            String value = StringUtils.join(values, ' ') ;

            makeQueryTerm(nodeType, queryContext, queryTerms, paramName, value);

        }
        queryContext.setQueryTerms(queryTerms);
        return queryContext ;
    }


    public static QueryContext makeQueryContextFromText(String searchText, NodeType nodeType) {
        QueryContext queryContext = new QueryContext() ;
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

        if(nodeType == null) {
            if (paramName.equals("sorting")) {
                queryContext.setSorting(value);
            } else if (paramName.contains("_")) {
                String fieldId = StringUtils.substringBeforeLast(paramName, "_");
                queryTerms.add(new QueryTerm(StringUtils.substringBeforeLast(paramName, "_"), StringUtils.substringAfterLast(paramName, "_"), value));
            } else {
                queryTerms.add(new QueryTerm(paramName, value));
            }
        }else{
            if (paramName.equals("sorting")) {
                queryContext.setSorting(value, nodeType);
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
}
