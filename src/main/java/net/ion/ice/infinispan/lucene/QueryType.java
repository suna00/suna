package net.ion.ice.infinispan.lucene;

import org.apache.lucene.search.Query;

/**
 * Created by jaeho on 2017. 4. 3..
 */
public class QueryType {
    private Query query ;
    private String booleanType ;

    public QueryType(String bop, Query query) {
        this.booleanType = bop;
        this.query = query;
    }
    public Query getQuery() {
        return query;
    }
    public void setQuery(Query query) {
        this.query = query;
    }
    public String getBooleanType() {
        return booleanType;
    }
    public void setBooleanType(String booleanType) {
        this.booleanType = booleanType;
    }

}
