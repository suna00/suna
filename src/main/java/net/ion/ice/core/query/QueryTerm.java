package net.ion.ice.core.query;

import net.ion.ice.core.context.TemplateParam;
import net.ion.ice.core.infinispan.lucene.AnalyzerFactory;
import net.ion.ice.core.infinispan.lucene.CodeAnalyzer;
import net.ion.ice.core.infinispan.lucene.LuceneQueryUtils;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;

import java.util.Date;

import static net.ion.ice.core.query.QueryTerm.QueryTermType.DATA;
import static net.ion.ice.core.query.QueryTerm.QueryTermType.NODE;

/**
 * Created by jaeho on 2017. 4. 24..
 */
public class QueryTerm {
    private QueryTermType queryTermType ;

    private String queryKey ;
    private Object queryValue ;

    private QueryMethod method ;
    private Analyzer analyzer;

    private PropertyType.ValueType valueType ;

    private Boolean not ;

    private Boolean should ;

    public QueryTerm(QueryTermType queryTermType, String queryKey, String analyzer, String method, Object queryValue, PropertyType.ValueType valueType) {
        this.queryTermType = queryTermType ;
        this.queryKey = queryKey ;

        if(method == null){
            if(queryTermType == DATA){
                method = "equals" ;
            }else{
                method = "matching" ;
            }
        }

        method = method.toUpperCase() ;

        if(method.startsWith("NOT")){
            this.not = true;
            method = StringUtils.substringAfter(method, "NOT");
        }

        if(method.endsWith("SHOULD")){
            this.should = true ;
            method = StringUtils.substringBefore(method, "SHOULD");
        }

        if(method.endsWith("OR")){
            this.should = true ;
            method = StringUtils.substringBefore(method, "OR");
        }

        if(method.endsWith("AND")){
            this.should = false ;
            method = StringUtils.substringBefore(method, "AND");
        }

        if(method.endsWith("MUST")){
            this.should = false ;
            method = StringUtils.substringBefore(method, "MUST");
        }


        this.method = QueryMethod.valueOf(method.toUpperCase()) ;
        this.queryValue = queryValue;

        if(queryTermType == NODE) {
            this.analyzer = AnalyzerFactory.getAnalyzer(analyzer);
        }
        if(valueType == null){
            if(queryValue instanceof String){
                valueType = PropertyType.ValueType.STRING ;
            }else if(queryValue instanceof Integer){
                valueType = PropertyType.ValueType.INT ;
            }else if(queryValue instanceof Long){
                valueType = PropertyType.ValueType.LONG ;
            }else if(queryValue instanceof Double){
                valueType = PropertyType.ValueType.DOUBLE ;
            }else if(queryValue instanceof Date){
                valueType = PropertyType.ValueType.DATE ;
            }else{
                valueType = PropertyType.ValueType.STRING ;
            }
        }
        this.valueType = valueType ;
    }

    public QueryTerm(QueryTermType queryTermType, String queryKey, String method, Object queryValue, PropertyType.ValueType valueType) {
        this(queryTermType, queryKey, null, method, queryValue, valueType) ;
    }

    public QueryTerm(QueryTermType queryTermType, String queryKey, String method, Object queryValue) {
        this(queryTermType, queryKey, method, queryValue, null) ;
    }

    public QueryTerm(QueryTermType queryTermType, String queryKey, Object queryValue){
        this(queryTermType, queryKey, null, queryValue) ;
    }

    public QueryTerm(String fieldId, Analyzer luceneAnalyzer, String method, Object value, PropertyType.ValueType valueType) {
        this.queryKey = fieldId ;
        if(method == null){
            method = "matching" ;
        }

        method = method.toUpperCase() ;

        if(method.startsWith("NOT")){
            this.not = true;
            method = StringUtils.substringAfter(method, "NOT");
        }

        if(method.endsWith("SHOULD")){
            this.should = true ;
            method = StringUtils.substringBefore(method, "SHOULD");
        }

        if(method.endsWith("OR")){
            this.should = true ;
            method = StringUtils.substringBefore(method, "OR");
        }

        if(method.endsWith("AND")){
            this.should = false ;
            method = StringUtils.substringBefore(method, "AND");
        }

        if(method.endsWith("MUST")){
            this.should = false ;
            method = StringUtils.substringBefore(method, "MUST");
        }


        this.method = QueryMethod.valueOf(method.toUpperCase()) ;
        this.queryValue = value ;
        this.analyzer = luceneAnalyzer ;
        this.queryTermType = NODE ;
        this.valueType = valueType ;
    }


    public boolean isNot(){
        return not != null && not ;
    }


    public boolean isShould(){
        return should !=null && should ;
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
        if(this.queryTermType == DATA && method == QueryMethod.MATCHING){
            return "%".concat((String) queryValue).concat("%") ;
        }
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

    public String getMethodQuery() {
        if(isNot()){
            if(QueryMethod.MATCHING.equals(method) || QueryMethod.IN.equals(method)){
                return "NOT ".concat(method.getQueryString());
            }else if(QueryMethod.EQUALS.equals(method)){
                return "!".concat(method.getQueryString());
            }
        }
        return method.getQueryString();
    }

    public PropertyType.ValueType getValueType() {
        return valueType;
    }

    public String getValue(){
        return queryValue.toString() ;
    }

    public String toString(){
        return this.queryKey + "=" + this.queryValue ;
    }
    public enum QueryMethod {
        PHRASE("LIKE"),
        WILDCARD("LIKE"),
        FUZZY(""),
        MATCHING("LIKE"),
        IN("IN"),
        EQUALS("="),
        ABOVE(">="),
        BELOW("<="),
        EXCESS(">"),
        UNDER("<"),
        FROMTO("BETWEEN"),
        EXISTS("IN");


        private String queryString;

        QueryMethod(String queryString) {
            this.queryString = queryString;
        }

        String getQueryString() {
            return queryString;
        }
    }
    public enum QueryTermType {
        DATA,
        NODE,
        IF
    }

}
