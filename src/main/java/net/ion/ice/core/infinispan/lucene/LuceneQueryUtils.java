package net.ion.ice.core.infinispan.lucene;


import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.FacetTerm;
import net.ion.ice.core.query.QueryTerm;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.*;
import org.hibernate.search.query.dsl.impl.*;
import org.hibernate.search.query.engine.spi.FacetManager;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
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
        List<Query> shouldInnerQueries = new ArrayList<>();
        List<Query> notInnerQueries = new ArrayList<>();


        if(queryContext.getJoinQueryContexts() != null && queryContext.getJoinQueryContexts().size() >0){
            for(QueryContext joinQueryContext : queryContext.getJoinQueryContexts()){
                List<Object> joinQueryResult = NodeUtils.getNodeService().executeQuery(joinQueryContext) ;
                String targetJoinField = joinQueryContext.getTargetJoinField();
                String sourceJoinField = joinQueryContext.getSourceJoinField();

                List<String> joinValues = new ArrayList<>();

                for(Object joinObj : joinQueryResult){
                    String joinVal = ((Node) joinObj).getStringValue(targetJoinField) ;
                    if(!joinValues.contains(joinVal)){
                        joinValues.add(joinVal) ;
                    }
                }
                if(joinValues.size() > 1) {
                    BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
                    for (String joinVal : joinValues) {
                        Query termQuery = new TermQuery(new Term(sourceJoinField, joinVal));
                        booleanQueryBuilder.add(termQuery, BooleanClause.Occur.SHOULD);
                    }
                    innerQueries.add(booleanQueryBuilder.build());
                }else if(joinValues.size() == 1){
                    innerQueries.add(new TermQuery(new Term(sourceJoinField, joinValues.get(0)))) ;
                }
            }
        }


        if(queryContext.hasQueryTerms()) {
            for (QueryTerm term : queryContext.getQueryTerms()) {
                if(StringUtils.isEmpty(term.getQueryValue())){
                    continue;
                }
                if(term.isNot()){
                    notInnerQueries.add(createLuceneQuery(term)) ;
                }else if(term.isShould()){
                    shouldInnerQueries.add(createLuceneQuery(term)) ;
                }else {
                    innerQueries.add(createLuceneQuery(term));
                }
            }
        }

        if(innerQueries.size() == 0 && shouldInnerQueries.size() == 0 && notInnerQueries.size() == 0){
            query = new MatchAllDocsQuery() ;
        }else  if(innerQueries.size() == 1 && shouldInnerQueries.size() == 0 && notInnerQueries.size() == 0){
            query = innerQueries.get(0) ;
        }else  if(innerQueries.size() == 0 && shouldInnerQueries.size() == 1 && notInnerQueries.size() == 0){
            query = shouldInnerQueries.get(0) ;
        }else  if(innerQueries.size() == 0 && shouldInnerQueries.size() == 0 && notInnerQueries.size() == 1){
            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            booleanQueryBuilder.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST) ;
            booleanQueryBuilder.add(notInnerQueries.get(0), BooleanClause.Occur.MUST_NOT) ;
            query = booleanQueryBuilder.build() ;
        }else{
            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();

            for(Query innerQuery : innerQueries){
                booleanQueryBuilder.add(innerQuery, BooleanClause.Occur.MUST) ;
            }

            if (notInnerQueries.size() == 1) {
                booleanQueryBuilder.add(notInnerQueries.get(0), BooleanClause.Occur.MUST_NOT);
            } else if (notInnerQueries.size() > 1) {
                BooleanQuery.Builder notBooleanQueryBuilder = new BooleanQuery.Builder();
                for (Query notQuery : notInnerQueries) {
                    notBooleanQueryBuilder.add(notQuery, BooleanClause.Occur.SHOULD);
                }
                booleanQueryBuilder.add(notBooleanQueryBuilder.build(), BooleanClause.Occur.MUST_NOT) ;

            }

            if(shouldInnerQueries.size() == 1){
                booleanQueryBuilder.add(shouldInnerQueries.get(0), BooleanClause.Occur.MUST) ;
            }else if(shouldInnerQueries.size() > 1){
                BooleanQuery.Builder shouldBooleanQueryBuilder = new BooleanQuery.Builder();
                for (Query shouldQuery : shouldInnerQueries) {
                    shouldBooleanQueryBuilder.add(shouldQuery, BooleanClause.Occur.SHOULD);
                }
                booleanQueryBuilder.add(shouldBooleanQueryBuilder.build(), BooleanClause.Occur.MUST) ;
            }
            query = booleanQueryBuilder.build();
        }

        CacheQuery cacheQuery = queryContext.getSearchManager().getQuery(query, Node.class) ;

        if(queryContext.hasSorting()) {
            makeSorting(queryContext, cacheQuery);
        }

        if(queryContext.getFacetTerms()!= null && queryContext.getFacetTerms().size() > 0){
            makeFacet(queryContext, cacheQuery) ;
        }

        cacheQuery.maxResults(queryContext.getMaxResultSize()) ;

        return cacheQuery ;
    }

    private static void makeFacet(QueryContext queryContext, CacheQuery cacheQuery) {
        FacetManager facetManager = cacheQuery.getFacetManager() ;
        QueryBuilder queryBuilder = queryContext.getSearchManager().buildQueryBuilderForClass(Node.class).get();

        for(FacetTerm facetTerm : queryContext.getFacetTerms()){
            if(facetTerm.isDiscrete()) {
                FacetingRequest fieldFacetRequest = queryBuilder.facet()
                        .name(facetTerm.getName())
                        .onField(facetTerm.getFieldName())
                        .discrete()
                        .orderedBy(FacetSortOrder.COUNT_DESC)
                        .includeZeroCounts(false)
                        .maxFacetCount(-1)
                        .createFacetingRequest();
                facetManager.enableFaceting(fieldFacetRequest);
            }else{
                FacetRangeAboveBelowContext<Object> cond = queryBuilder.facet()
                        .name(facetTerm.getName())
                        .onField(facetTerm.getFieldName())
                        .range() ;
                FacetRangeBelowContinuationContext<Object> below = null ;
                FacetRangeEndContext<Object> fromto = null ;
                FacetingRequest rangeFacetRequest = null ;
                PropertyType pt = queryContext.getNodetype().getPropertyType(facetTerm.getFieldName()) ;
                PropertyType.ValueType valueType = pt.getValueType();

                for(int i = 0; i< facetTerm.getRangeList().size(); i++){
                    String fc = facetTerm.getRangeList().get(i) ;
                    if(i == 0){
                        below = cond.below(getValueTypeValue(valueType, fc.trim())) ;
                    }else if(i == (facetTerm.getRangeList().size() - 1)){
                        rangeFacetRequest = fromto.above(getValueTypeValue(valueType, fc.trim())).excludeLimit().createFacetingRequest() ;
                    }else{
                        Object from  = getValueTypeValue(valueType, StringUtils.substringBefore(fc,"~").trim()) ;
                        Object to  = getValueTypeValue(valueType, StringUtils.substringAfter(fc,"~").trim()) ;

                        if(fromto == null){
                            fromto = below.from(from).to(to) ;
                        }else{
                            fromto = fromto.from(from).to(to) ;
                        }
                    }
                }
                facetManager.enableFaceting(rangeFacetRequest) ;
            }
        }
    }

    private static Object getValueTypeValue(PropertyType.ValueType valueType, String value) {
        switch (valueType){
            case DATE: {
                return NodeUtils.getDateLongValue(value) ;
            }
            case LONG: {
                return Long.parseLong(value) ;
            }
            case INT: {
                return Integer.parseInt(value) ;
            }
            case DOUBLE: {
                return Double.parseDouble(value);
            }
            default: {
                return value;
            }

        }
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
                PropertyType pt = queryContext.getNodetype().getPropertyType(sortField) ;
                if(pt != null) {
                    sortTypeStr = pt.getValueType().toString();
                    if(!pt.isSorted() && pt.isSortable() && !pt.isNumeric()){
                        sortField = sortField + "_sort" ;
                    }
                }else if(sortField.equals("created") || sortField.equals("changed")){
                    sortTypeStr = "DATE";
                }
            }
            String order = StringUtils.substringAfter(sorting, " ") ;

            SortField.Type sortType = null ;

            switch (sortTypeStr){
                case "STRING":case "TEXT":{
                    sortType = SortField.Type.STRING ;
                    break ;
                }
                case "LONG": case "DATE": {
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

        switch (term.getMethod()) {
            case MATCHING: case WILDCARD:{
                return createKeywordTermQuery(term);
            }
            case EQUALS:{
                return createTermQuery(term, term.getQueryValue());
            }
            case ABOVE:{
                return createRangeQuery(term, term.getQueryValue(), null, true, true);
            }
            case BELOW:{
                return createRangeQuery(term, null, term.getQueryValue(), true, true);
            }
            case EXCESS:{
                return createRangeQuery(term, term.getQueryValue(), null, false, false);
            }
            case UNDER:{
                return createRangeQuery(term, null, term.getQueryValue(), false, false);
            }
            case FROMTO:{
                return createRangeQuery(term, StringUtils.substringBefore(term.getQueryValue(), "~"), StringUtils.substringAfter(term.getQueryValue(), "~"), true, true);
            }
        }
        return createKeywordTermQuery(term);
    }

    private static Query createRangeQuery(QueryTerm term, String min, String max, boolean minInclusive, boolean maxInclusive) {
        switch(term.getValueType()) {
            case DATE: {
                return NumericRangeQuery.newLongRange(term.getQueryKey(), min != null ? NodeUtils.getDateLongValue(min) : null, max != null ? NodeUtils.getDateLongValue(max) : null, minInclusive, maxInclusive);
            }
            case LONG: {
                return NumericRangeQuery.newLongRange(term.getQueryKey(), min != null ? new Long(min): null, max != null ? new Long(max) : null, minInclusive, maxInclusive);
            }
            case INT: {
                return NumericRangeQuery.newIntRange(term.getQueryKey(), min != null ? new Integer(min): null, max != null ? new Integer(max) : null, minInclusive, maxInclusive);
            }
            case DOUBLE: {
                return NumericRangeQuery.newDoubleRange(term.getQueryKey(), min != null ? new Double(min): null, max != null ? new Double(max) : null, minInclusive, maxInclusive);
            }
            default: {
                return TermRangeQuery.newStringRange(term.getQueryKey(), min, max, minInclusive, maxInclusive);
            }
        }
    }

    private static Query createKeywordTermQuery(QueryTerm term) throws IOException {
        Query query;
        List<String> terms;

        if(term.getAnalyzer() instanceof SimpleAnalyzer){
            terms = new ArrayList<>(1) ;
            terms.add(term.getQueryValue()) ;
        }else {
            terms = getAllTermsFromText(
                    term.getQueryKey(),
                    term.getQueryValue(),
                    term.getAnalyzer()
            );
        }

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

    private static Query createKeywordRangeQuery(String fieldName, RangeQueryContext rangeContext, QueryBuildingContext queryContext, ConversionContext conversionContext, FieldContext fieldContext) {
        final AnalyzerReference analyzerReference = queryContext.getQueryAnalyzerReference();

        final DocumentBuilderIndexedEntity documentBuilder = queryContext.getDocumentBuilder();

        final String fromString = rangeContext.hasFrom() ?
                fieldContext.objectToString( documentBuilder, rangeContext.getFrom(), conversionContext ) :
                null;
        final String toString = rangeContext.hasTo() ?
                fieldContext.objectToString( documentBuilder, rangeContext.getTo(), conversionContext ) :
                null;

        String lowerTerm = null ;
        String upperTerm;
//        if ( queryContext.getFactory().getIndexBinding( queryContext.getEntityType() ).getIndexManagers()[0] instanceof RemoteAnalyzerProvider) {
//            lowerTerm = fromString == null ? null : fromString;
//            upperTerm = toString == null ? null : toString;
//        }
//        else {
//            final Analyzer queryAnalyzer = analyzerReference.unwrap( LuceneAnalyzerReference.class ).getAnalyzer();
//
//            lowerTerm = fromString == null ?
//                    null :
//                    Helper.getAnalyzedTerm( fieldName, fromString, "from", queryAnalyzer, fieldContext );
//
//            upperTerm = toString == null ?
//                    null :
//                    Helper.getAnalyzedTerm( fieldName, toString, "to", queryAnalyzer, fieldContext );
//        }

        return TermRangeQuery.newStringRange( fieldName, lowerTerm, null, !rangeContext.isExcludeFrom(), !rangeContext.isExcludeTo() );
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
                query = new TermQuery( createTerm(termContext, fieldName,  term));
                break;
            case WILDCARD:
                if(StringUtils.contains(term, "*") || StringUtils.contains(term, "?")){
                    query = new WildcardQuery(new Term(fieldName, "*"+term+"*")) ;
                }else {
                    query = new WildcardQuery(new Term(fieldName, term));
                }
                break;
//            case FUZZY:
//                int maxEditDistance = getMaxEditDistance( term );
//                query = new FuzzyQuery(
//                        new Term( fieldName, term ),
//                        maxEditDistance,
//                        termContext.getPrefixLength()
//                );
//                break;
            case EQUALS:
                query = new TermQuery( createTerm(termContext, fieldName,  term));
                break ;
            default:
                throw new AssertionFailure( "Unknown approximation: " + termContext.getMethod() );
        }
        return query;
    }

    private static Term createTerm(QueryTerm termContext, String fieldName, String term) {
        switch (termContext.getValueType()){
            case INT: {
                BytesRefBuilder brb = new BytesRefBuilder();
                NumericUtils.intToPrefixCoded(new Integer(term), 0, brb);
                return new Term(fieldName, brb.get());
            }
            case LONG: {
                BytesRefBuilder brb = new BytesRefBuilder();
                NumericUtils.longToPrefixCoded(new Long(term), 0, brb);
                return new Term(fieldName, brb.get());
            }
            case DOUBLE: {
                BytesRefBuilder brb = new BytesRefBuilder();
                NumericUtils.longToPrefixCoded(NumericUtils.doubleToSortableLong(new Double(term)), 0, brb);
                return new Term(fieldName, brb.get());
            }
            case DATE :{

            }
            default:
                return new Term(fieldName, term);
        }
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
