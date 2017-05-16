package net.ion.ice.core.infinispan;

import net.ion.ice.core.node.Node;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.query.SearchManager;

import java.util.List;

/**
 * Created by jaeho on 2017. 4. 26..
 */
public class QueryContext{
    private List<QueryTerm> queryTerms ;
    private SearchManager searchManager;
    private String sorting;

    public QueryContext() {
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

    public void setSorting(String sortingStr, Node nodeType) {
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
}
