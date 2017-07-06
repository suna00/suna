package net.ion.ice.core.infinispan.lucene;


import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.node.Node;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.hibernate.search.bridge.impl.JavaTimeBridgeProvider;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.infinispan.query.CacheQuery;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.apache.lucene.facet.DrillDownQuery.term;

/**
 * Created by jaeho on 2017. 4. 24..
 */
public class LuceneQueryUtils {

    public static CacheQuery makeQuery(QueryContext queryContext) throws IOException {
        Query query ;
        List<Query> innerQueries =  new ArrayList<>();
        if(queryContext.hasQueryTerms()) {
            for (QueryTerm term : queryContext.getQueryTerms()) {
                innerQueries.add(createLuceneQuery(term));
            }
        }
        if(innerQueries.size() ==0){
            query = new MatchAllDocsQuery() ;
        }else  if(innerQueries.size() == 1){
            query = innerQueries.get(0) ;
        }else{
            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            for(Query innerQuery : innerQueries){
                booleanQueryBuilder.add(innerQuery, BooleanClause.Occur.MUST) ;
            }
            query = booleanQueryBuilder.build();
        }

        CacheQuery cacheQuery = queryContext.getSearchManager().getQuery(query, Node.class) ;

        if(queryContext.hasSorting()) {
            makeSorting(queryContext, cacheQuery);
        }else{

        }

        cacheQuery.maxResults(queryContext.getMaxResultSize()) ;

        return cacheQuery ;
    }

    private static void makeSorting(QueryContext queryContext, CacheQuery cacheQuery) {
        String[] sortings = StringUtils.split(queryContext.getSorting(), ",");
        List<SortField> sorts = new ArrayList<SortField>();
        for (String sorting : sortings) {
            String sortField = StringUtils.substringBefore(sorting.trim(), " ").trim();
            if(StringUtils.isEmpty(sortField)){
                sortField = sorting ;
            }
            String sortTypeStr = null;
            if (StringUtils.contains(sortField, "(")) {
                sortTypeStr = StringUtils.substringBefore(sortField, "(").trim().toUpperCase();
                sortField = StringUtils.substringBetween(sortField, "(", ")");
            }else{
                sortTypeStr = queryContext.getNodetype().getPropertyType(sortField).getValueType().toString() ;
            }
            String order = StringUtils.substringAfter(sorting, " ") ;

            SortField.Type sortType = null ;

            switch (sortTypeStr){
                case "STRING":case "TEXT":case "DATE":{
                    sortType = SortField.Type.STRING ;
                    break ;
                }
                case "LONG": {
                    sortType = SortField.Type.LONG;
                    break ;
                }
                case "INT" : {
                    sortType = SortField.Type.INT ;
                    break ;
                }
                case "DOUBLE" : {
                    sortType = SortField.Type.DOUBLE ;
                    break ;
                }
                default:
                    sortType = SortField.Type.STRING ;
                    break ;
            }

            sorts.add(new SortField(sortField, sortType, order.equalsIgnoreCase("desc") ? true : false));
        }

        Sort sort = new Sort(sorts.toArray(new SortField[sorts.size()]));
        cacheQuery.sort(sort);
    }

    private static Query createLuceneQuery(QueryTerm term) throws IOException {
        Query query;
        List<String> terms = getAllTermsFromText(
                term.getQueryKey(),
                term.getQueryValue(),
                term.getAnalyzer()
        );

        if ( terms.size() == 0 ) {
            throw new IceRuntimeException("term size zero") ;
        }
        else if ( terms.size() == 1 ) {
            query = createTermQuery( term, terms.get( 0 ) );
        }
        else {
            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            for ( String localTerm : terms ) {
                Query termQuery = createTermQuery( term, localTerm );
                booleanQueryBuilder.add( termQuery, BooleanClause.Occur.SHOULD );
            }
            query = booleanQueryBuilder.build();
        }
        return query;
    }


    public static Query equalsQuery(String key, String value, boolean isNumeric) {
        TermQuery termQuery = new TermQuery(isNumeric ? new Term(key, getBytesRefNumericValue(value)) : new Term(key, value));
        return (Query) termQuery;
    }

    public static Query containsQuery(String key, String value, boolean isNumeric) {
        WildcardQuery wildcardQuery = new WildcardQuery(isNumeric ? new Term(key, getBytesRefNumericValue(value)) : term(key, "*" + value + "*"));
        return (Query) wildcardQuery;
    }

    public static Query matchingQuery(String key, String value, boolean isNumeric) {
        WildcardQuery wildcardQuery = new WildcardQuery(isNumeric ? new Term(key, getBytesRefNumericValue(value)) : term(key, value));
        return (Query) wildcardQuery;
    }

    public static Query startWithQuery(String key, String value) {
        PrefixQuery prefixQuery = new PrefixQuery(term(key, value));
        return (Query) prefixQuery;
    }

    public static Query endWithQuery(String key, String value) {
        WildcardQuery wildcardQuery = new WildcardQuery(term(key, "*" + value));
        return (Query) wildcardQuery;
    }

    public static Query afterQuery(String key, Long value, boolean minInclusive, boolean maxInclusive) {
        NumericRangeQuery<Long> numericRangeQuery = NumericRangeQuery.newLongRange(key, value, null, minInclusive, maxInclusive);
        return (Query) numericRangeQuery;
    }

    public static Query beforeQuery(String key, Long value, boolean minInclusive, boolean maxInclusive) {
        NumericRangeQuery<Long> numericRangeQuery = NumericRangeQuery.newLongRange(key, null, value, minInclusive, maxInclusive);
        return (Query) numericRangeQuery;
    }

    public static Query isNotNullQuery(String key) {
        WildcardQuery wildcardQuery = new WildcardQuery(term(key, "*"));
        return (Query) wildcardQuery;
    }

    public static BytesRef getBytesRefNumericValue(String value) {
        if(StringUtils.isNumeric(value)){
            BytesRefBuilder brb = new BytesRefBuilder();
            NumericUtils.longToPrefixCoded(new Long(value), 0, brb);
            return brb.get();
        }else{
            return new BytesRef(value);
        }
    }

//    private List<String> getAllTermsFromText(String fieldName, String localText, Analyzer analyzer) {
//        //it's better not to apply the analyzer with wildcard as * and ? can be mistakenly removed
//        List<String> terms = new ArrayList<String>();
//        if ( termContext.getApproximation() == TermQueryContext.Approximation.WILDCARD ) {
//            terms.add( localText );
//        }
//        else {
//            try {
//                terms = Helper.getAllTermsFromText( fieldName, localText, analyzer );
//            }
//            catch (IOException e) {
//                throw new AssertionFailure( "IO exception while reading String stream??", e );
//            }
//        }
//        return terms;
//    }

//    public Query createQuery() {
//        final int size = fieldsContext.size();
//        final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
//        if ( size == 1 ) {
//            return queryCustomizer.setWrappedQuery( createQuery( fieldsContext.getFirst(), conversionContext ) ).createQuery();
//        }
//        else {
//            BooleanQuery.Builder aggregatedFieldsQueryBuilder = new BooleanQuery.Builder();
//            for ( FieldContext fieldContext : fieldsContext ) {
//                aggregatedFieldsQueryBuilder.add(
//                        createQuery( fieldContext, conversionContext ),
//                        BooleanClause.Occur.SHOULD
//                );
//            }
//            BooleanQuery aggregatedFieldsQuery = aggregatedFieldsQueryBuilder.build();
//            return queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
//        }
//    }
//
//    private Query createQuery(FieldContext fieldContext, ConversionContext conversionContext) {
//        final Query perFieldQuery;
//        final DocumentBuilderIndexedEntity documentBuilder = queryContext.getDocumentBuilder();
//        final boolean applyTokenization;
//
//        FieldBridge fieldBridge = fieldContext.getFieldBridge() != null ? fieldContext.getFieldBridge() : documentBuilder.getBridge( fieldContext.getField() );
//        // Handle non-null numeric values
//        if ( value != null ) {
//            applyTokenization = fieldContext.applyAnalyzer();
//            if ( NumericFieldUtils.isNumericFieldBridge( fieldBridge ) ) {
//                return NumericFieldUtils.createExactMatchQuery( fieldContext.getField(), value );
//            }
//        }
//        else {
//            applyTokenization = false;
//            if ( fieldBridge instanceof NullEncodingTwoWayFieldBridge) {
//                NullEncodingTwoWayFieldBridge nullEncodingBridge = (NullEncodingTwoWayFieldBridge) fieldBridge;
//                validateNullValueIsSearchable( fieldContext );
//                return nullEncodingBridge.buildNullQuery( fieldContext.getField() );
//            }
//        }
//
//        validateNullValueIsSearchable( fieldContext );
//        final String searchTerm = buildSearchTerm( fieldContext, documentBuilder, conversionContext );
//
//        if ( !applyTokenization || fieldBridge instanceof IgnoreAnalyzerBridge ||
//                (fieldBridge instanceof NullEncodingTwoWayFieldBridge
//                        && ((NullEncodingTwoWayFieldBridge) fieldBridge).unwrap() instanceof IgnoreAnalyzerBridge ) ) {
//            perFieldQuery = createTermQuery( fieldContext, searchTerm );
//        }
//        else {
//            // we need to build differentiated queries depending of if the search terms should be analyzed
//            // locally or not
//            if ( queryContext.getQueryAnalyzerReference().is( RemoteAnalyzerReference.class ) ) {
//                perFieldQuery = createRemoteQuery( fieldContext, searchTerm );
//            }
//            else {
//                perFieldQuery = createLuceneQuery( fieldContext, searchTerm );
//            }
//        }
//        return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
//    }
//
//    private void validateNullValueIsSearchable(FieldContext fieldContext) {
//        if ( fieldContext.isIgnoreFieldBridge() ) {
//            if ( value == null ) {
//                throw log.unableToSearchOnNullValueWithoutFieldBridge( fieldContext.getField() );
//            }
//        }
//    }
//
//    private String buildSearchTerm(FieldContext fieldContext, DocumentBuilderIndexedEntity documentBuilder, ConversionContext conversionContext) {
//        if ( fieldContext.isIgnoreFieldBridge() ) {
//            String stringform = value.toString();
//            if ( stringform == null ) {
//                throw new SearchException(
//                        "When ignoreFieldBridge() is enabled, toString() on the value is used: the returned string must not be null: " +
//                                "on field " + fieldContext.getField() );
//            }
//            return stringform;
//        }
//        else {
//            // need to go via the appropriate bridge, because value is an object, eg boolean, and must be converted to a string first
//            return fieldContext.objectToString( documentBuilder, value, conversionContext );
//        }
//    }

    private static Query createTermQuery(QueryTerm termContext, String term) {
        Query query;
        final String fieldName = termContext.getQueryKey();
        switch ( termContext.getMethod() ) {
            case MATCHING:
                query = new TermQuery( new Term( fieldName, term ) );
                break;
            case WILDCARD:
                query = new WildcardQuery(new Term(fieldName, term)) ;
                break;
//            case FUZZY:
//                int maxEditDistance = getMaxEditDistance( term );
//                query = new FuzzyQuery(
//                        new Term( fieldName, term ),
//                        maxEditDistance,
//                        termContext.getPrefixLength()
//                );
//                break;
            default:
                throw new AssertionFailure( "Unknown approximation: " + termContext.getMethod() );
        }
        return query;
    }


    public static Query createNumericRangeQuery(String fieldName, Object from, Object to,
                                                boolean includeLower, boolean includeUpper) {

        Class<?> numericClass;

        if ( from != null ) {
            numericClass = from.getClass();
        }
        else if ( to != null ) {
            numericClass = to.getClass();
        }
        else {
            throw new IceRuntimeException("Invalid Query");

        }

        if ( Double.class.isAssignableFrom( numericClass ) ) {
            return NumericRangeQuery.newDoubleRange( fieldName, (Double) from, (Double) to, includeLower, includeUpper );
        }
        if ( Byte.class.isAssignableFrom( numericClass ) ) {
            return NumericRangeQuery.newIntRange( fieldName, ( (Byte) from ).intValue(), ( (Byte) to ).intValue(), includeLower, includeUpper );
        }
        if ( Short.class.isAssignableFrom( numericClass ) ) {
            return NumericRangeQuery.newIntRange( fieldName, ( (Short) from ).intValue(), ( (Short) to ).intValue(), includeLower, includeUpper );
        }
        if ( Long.class.isAssignableFrom( numericClass ) ) {
            return NumericRangeQuery.newLongRange( fieldName, (Long) from, (Long) to, includeLower, includeUpper );
        }
        if ( Integer.class.isAssignableFrom( numericClass ) ) {
            return NumericRangeQuery.newIntRange( fieldName, (Integer) from, (Integer) to, includeLower, includeUpper );
        }
        if ( Float.class.isAssignableFrom( numericClass ) ) {
            return NumericRangeQuery.newFloatRange( fieldName, (Float) from, (Float) to, includeLower, includeUpper );
        }
        if ( Date.class.isAssignableFrom( numericClass ) ) {
            Long fromValue = from != null ? ((Date) from).getTime() : null;
            Long toValue = to != null ? ((Date) to).getTime() : null;
            return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
        }
        if ( Calendar.class.isAssignableFrom( numericClass ) ) {
            Long fromValue = from != null ? ((Calendar) from).getTime().getTime() : null;
            Long toValue = to != null ? ((Calendar) to).getTime().getTime() : null;
            return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
        }
        if ( JavaTimeBridgeProvider.isActive() ) {
            if ( java.time.Duration.class.isAssignableFrom( numericClass ) ) {
                Long fromValue = from != null ? ( (java.time.Duration) from ).toNanos() : null;
                Long toValue = to != null ? ( (java.time.Duration) to ).toNanos() : null;
                return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
            }
            if ( java.time.Year.class.isAssignableFrom( numericClass ) ) {
                Integer fromValue = from != null ? ( (java.time.Year) from ).getValue() : null;
                Integer toValue = to != null ? ( (java.time.Year) to ).getValue() : null;
                return NumericRangeQuery.newIntRange( fieldName, fromValue, toValue, includeLower, includeUpper );
            }
            if ( java.time.Instant.class.isAssignableFrom( numericClass ) ) {
                Long fromValue = from != null ? ( (java.time.Instant) from ).toEpochMilli() : null;
                Long toValue = to != null ? ( (java.time.Instant) to ).toEpochMilli() : null;
                return NumericRangeQuery.newLongRange( fieldName, fromValue, toValue, includeLower, includeUpper );
            }
        }

//        throw log.numericRangeQueryWithNonNumericToAndFromValues( fieldName );
        return null;
    }

    static List<String> getAllTermsFromText(String fieldName, String localText, Analyzer analyzer) throws IOException {
        List<String> terms = new ArrayList<String>();

        // Can't deal with null at this point. Likely returned by some FieldBridge not recognizing the type.
        if ( localText == null ) {
            throw new SearchException( "Search parameter on field '" + fieldName + "' could not be converted. " +
                    "Are the parameter and the field of the same type?" +
                    "Alternatively, apply the ignoreFieldBridge() option to " +
                    "pass String parameters" );
        }
        final Reader reader = new StringReader( localText );
        final TokenStream stream = analyzer.tokenStream( fieldName, reader);
        try {
            CharTermAttribute attribute = stream.addAttribute( CharTermAttribute.class );
            stream.reset();
            while ( stream.incrementToken() ) {
                if ( attribute.length() > 0 ) {
                    String term = new String( attribute.buffer(), 0, attribute.length() );
                    terms.add( term );
                }
            }
            stream.end();
        }
        finally {
            stream.close();
        }
        return terms;
    }
}
