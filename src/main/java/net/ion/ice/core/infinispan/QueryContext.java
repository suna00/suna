package net.ion.ice.core.infinispan;

import org.infinispan.query.SearchManager;

import java.util.List;

/**
 * Created by jaeho on 2017. 4. 26..
 */
public class QueryContext{
    private List<QueryTerm> queryTerms ;
    private SearchManager searchManager;

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
}
