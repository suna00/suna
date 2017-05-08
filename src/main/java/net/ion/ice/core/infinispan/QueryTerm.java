package net.ion.ice.core.infinispan;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;

/**
 * Created by jaeho on 2017. 4. 24..
 */
public class QueryTerm {
    private String queryKey ;
    private String queryValue ;

    private QueryMethod method ;
    private Analyzer analyzer;

    public QueryTerm(String queryKey, String method, String queryValue) {
        this.queryKey = queryKey ;

        this.method = QueryMethod.valueOf(method.toUpperCase()) ;
        this.queryValue = queryValue ;
    }

    public QueryTerm(String queryKey, String queryValue){
        this(queryKey, "matching", queryValue) ;
    }

    public QueryMethod getMethod() {
        return method;
    }


    public String getQueryKey() {
        return queryKey;
    }

    public void setQueryKey(String queryKey) {
        this.queryKey = queryKey;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public void setQueryValue(String queryValue) {
        this.queryValue = queryValue;
    }

    public Analyzer getAnalyzer() {
        if(this.analyzer == null){
            return new SimpleAnalyzer() ;
        }
        return analyzer;
    }

    public enum QueryMethod {
        MATCHING,
        PHRASE,
        ABOVE,
        BELOW,
        WILDCARD,
        FUZZY;
    }
    

}
