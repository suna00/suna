package net.ion.ice.core.query;

import net.ion.ice.core.infinispan.lucene.AnalyzerFactory;
import net.ion.ice.core.infinispan.lucene.CodeAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;

/**
 * Created by jaeho on 2017. 4. 24..
 */
public class QueryTerm {
    private String queryKey ;
    private Object queryValue ;

    private QueryMethod method ;
    private Analyzer analyzer;

    public QueryTerm(String queryKey, String analyzer, String method, Object queryValue) {
        this.queryKey = queryKey ;

        this.method = QueryMethod.valueOf(method.toUpperCase()) ;
        this.queryValue = queryValue ;
        this.analyzer = AnalyzerFactory.getAnalyzer(analyzer) ;
    }


    public QueryTerm(String queryKey, String method, Object queryValue) {
        this(queryKey, "simple", method, queryValue) ;
    }

    public QueryTerm(String queryKey, Object queryValue){
        this(queryKey, "matching", queryValue) ;
    }

    public QueryTerm(String fieldId, Analyzer luceneAnalyzer, String method, Object value) {
        this.queryKey = fieldId ;

        this.method = QueryMethod.valueOf(method.toUpperCase()) ;
        this.queryValue = value ;
        this.analyzer = luceneAnalyzer ;
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
        return (String) queryValue;
    }

    public void setQueryValue(Object queryValue) {
        this.queryValue = queryValue;
    }

    public Analyzer getAnalyzer() {
        if(this.analyzer == null){
            return new CodeAnalyzer() ;
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
