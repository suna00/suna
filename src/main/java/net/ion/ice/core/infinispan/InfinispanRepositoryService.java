package net.ion.ice.core.infinispan;

import net.ion.ice.core.infinispan.lucene.LuceneQueryUtils;
import net.ion.ice.core.infinispan.lucene.QueryType;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.infinispan.Cache;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jaeho on 2017. 3. 31..
 */

@Service
public class InfinispanRepositoryService {

    @Autowired
    private InfinispanCacheManager cacheManager ;

    @Autowired
    private NodeService nodeService ;

    public Cache<String, Node> getNodeCache(String tid){
        return cacheManager.getCache(tid, 100000) ;
    }

    public Node getNode(String tid, String id) {
        Cache<String, Node> cache = getNodeCache(tid) ;
        return cache.get(id) ;
    }

    public Collection<Node> getNodes(String tid) {
        return getNodeCache(tid).values() ;
    }

    public List<Object> getQueryNodes(String tid, String search){
        Cache<String, Node> cache = getNodeCache(tid) ;

        NodeType nodeType = nodeService.getNodeType(tid) ;

        QueryContext queryContext = makeQueryContext(search, nodeType) ;
        queryContext.setSearchManager(Search.getSearchManager(cache));

        CacheQuery cacheQuery = null;
        try {
            cacheQuery = LuceneQueryUtils.makeQuery(queryContext);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Object> list = cacheQuery.list();

        return list ;
    }

    private QueryContext makeQueryContext(String searchText, NodeType nodeType) {
        QueryContext queryContext = new QueryContext() ;
        List<QueryTerm> queryTerms = new ArrayList<>();

        if(StringUtils.isEmpty(searchText)){
            return queryContext ;
        }

        for (String param : StringUtils.split(searchText, '&')) {
            if (StringUtils.isNotEmpty(param) && StringUtils.contains(param, "=")) {
                String value = StringUtils.substringAfter(param, "=");
                if(StringUtils.isEmpty(value)){
                    continue;
                }
                value = value.equals("@sysdate") ? new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()) : value.equals("@sysday") ? new SimpleDateFormat("yyyyMMdd").format(new Date()) : value;
                String paramName = StringUtils.substringBefore(param, "=") ;
                if(StringUtils.isEmpty(paramName)){
                    continue ;
                }

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
                        QueryTerm queryTerm = makeQueryTerm(nodeType, queryTerms, fieldId, method, value) ;
                        if(queryTerm == null){
                            queryTerm = makeQueryTerm(nodeType, queryTerms, paramName, "matching", value) ;
                        }

                        if(queryTerm != null ){
                            queryTerms.add(queryTerm) ;
                        }

                    } else {

                        queryTerms.add(new QueryTerm(paramName, value));
                    }

                }
            }
        }
        queryContext.setQueryTerms(queryTerms);
        return queryContext ;
    }

    private QueryTerm makeQueryTerm(NodeType nodeType, List<QueryTerm> queryTerms, String fieldId, String method, String value) {
        PropertyType propertyType = (PropertyType) nodeType.getPropertyType(fieldId);
        if(propertyType != null && propertyType.indexing()) {
            return new QueryTerm(fieldId, propertyType.getAnalyzer(), method, value);
        }
        return null ;
    }


    public static CacheQuery makeQuery(SearchManager qf, QueryBuilder queryBuilder, Node nodeType, Cache<String, Node> cache, Map<String, String[]> params) {
        if(qf == null) {
            qf = Search.getSearchManager(cache);
            queryBuilder = qf.buildQueryBuilderForClass(Node.class).get();
        }
        List<QueryType> innerQueries = new ArrayList<QueryType>();
        List<QueryType> notInnerQueries = new ArrayList<QueryType>();
        List<QueryType> shouldInnerQueries = new ArrayList<QueryType>();

        for (String paramName : params.keySet()) {
            String[] values = params.get(paramName);
            if (values == null || values.length == 0) continue;
            if (!StringUtils.contains(paramName, "_")) continue;


            //program_code_matching_join=program.program_code/media_code_matching=01
            String fieldId = StringUtils.substringBeforeLast(paramName, "_");
            String bop = StringUtils.substringAfterLast(paramName, "_");
            String qop = StringUtils.substringAfterLast(paramName, "_");
            if (bop.equalsIgnoreCase("should") || bop.equalsIgnoreCase("not") || bop.equalsIgnoreCase("must") || bop.equalsIgnoreCase("join")) {
                qop = StringUtils.substringAfterLast(fieldId, "_");
                fieldId = StringUtils.substringBeforeLast(fieldId, "_");
            } else {
                bop = "must";
            }
            if (StringUtils.isEmpty(fieldId)) continue;
            String value = values[0];
            if (StringUtils.isBlank(value)) continue;

            String indexKey = fieldId;

            QueryType innerQueryType = null;

            if (qop.equalsIgnoreCase("matching")) {
                if (value.contains("*") || value.contains("?")) {
                    innerQueryType = new QueryType(bop, queryBuilder.keyword().wildcard().onField(indexKey).matching(value.toLowerCase()).createQuery());
                } else {
                    innerQueryType = new QueryType(bop, queryBuilder.keyword().onField(indexKey).matching(value.toString().toLowerCase()).createQuery());
                }
            } else if (qop.equalsIgnoreCase("sentence")) {
                innerQueryType = new QueryType(bop, queryBuilder.phrase().onField(indexKey).sentence(value).createQuery());
            } else if (qop.equalsIgnoreCase("below")) {
                innerQueryType = new QueryType(bop, queryBuilder.range().onField(indexKey).below(value).createQuery());
            } else if (qop.equalsIgnoreCase("above")) {
                innerQueryType = new QueryType(bop, queryBuilder.range().onField(indexKey).above(value).createQuery());
            } else if (qop.equalsIgnoreCase("fromto")) {
                if(value.contains(",")){
                    for(String fromto : StringUtils.split(value, ",")){
                        Object from = StringUtils.substringBefore(fromto, "~").trim();
                        Object to = StringUtils.substringAfter(fromto, "~").trim();
                        if (indexKey.startsWith("string")) {
                            shouldInnerQueries.add(new QueryType(bop, queryBuilder.range().onField(indexKey).ignoreAnalyzer().ignoreFieldBridge().from(from).to(to).createQuery()));
                        } else {
                            shouldInnerQueries.add(new QueryType(bop, queryBuilder.range().onField(indexKey).from(from).to(to).createQuery()));
                        }
                    }
                }else{
                    Object from = StringUtils.substringBefore(value, "~").trim();
                    Object to = StringUtils.substringAfter(value, "~").trim();
                    if (indexKey.startsWith("string")) {
                        innerQueryType = new QueryType(bop, queryBuilder.range().onField(indexKey).ignoreAnalyzer().ignoreFieldBridge().from(from).to(to).createQuery());
                    } else {
                        innerQueryType = new QueryType(bop, queryBuilder.range().onField(indexKey).from(from).to(to).createQuery());

                    }
                }
            }
            if (innerQueryType != null) {
                if (bop.equals("not")) {
                    notInnerQueries.add(innerQueryType);
                } else if (bop.equals("should")){
                    shouldInnerQueries.add(innerQueryType) ;
                } else {
                    innerQueries.add(innerQueryType);
                }
            }
        }

        CacheQuery cacheQuery = null;

        if (innerQueries.size() == 0 && notInnerQueries.size() == 0) {
            cacheQuery = qf.getQuery(queryBuilder.all().createQuery());
        } else {

            BooleanJunction<BooleanJunction> baseQuery = queryBuilder.bool();
            for (QueryType queryType : innerQueries) {
                if (queryType.getBooleanType().equalsIgnoreCase("must")) {
                    baseQuery.must(queryType.getQuery());
                } else if (queryType.getBooleanType().equalsIgnoreCase("should")) {
                    baseQuery.should(queryType.getQuery());
                }
            }

            if (notInnerQueries.size() > 1) {
                BooleanJunction<BooleanJunction> notSubQuery = queryBuilder.bool();
                for (QueryType queryType : notInnerQueries) {
                    notSubQuery.should(queryType.getQuery());
                }
                baseQuery.must(notSubQuery.createQuery()).not();
            } else if (notInnerQueries.size() == 1) {
                baseQuery.must(notInnerQueries.get(0).getQuery()).not();
            }

            if (shouldInnerQueries.size() > 1) {
                BooleanJunction<BooleanJunction> shouldSubQuery = queryBuilder.bool();
                for (QueryType queryType : shouldInnerQueries) {
                    shouldSubQuery.should(queryType.getQuery());
                }
                baseQuery.must(shouldSubQuery.createQuery()) ;
            } else if (shouldInnerQueries.size() == 1) {
                baseQuery.should(shouldInnerQueries.get(0).getQuery());
            }
            cacheQuery = qf.getQuery(baseQuery.createQuery());
        }

        if (params.containsKey("sorting") && params.get("sorting") != null && StringUtils.isNotBlank(params.get("sorting")[0])) {
            String[] sortings = StringUtils.split(params.get("sorting")[0], ",");
            List<SortField> sorts = new ArrayList<SortField>();
            for (String sorting : sortings) {
                String sortField = StringUtils.substringBefore(sorting.trim(), " ").trim() ;
                String sortTypeStr = null ;
                if(StringUtils.contains(sortField, "(")){
                    sortTypeStr = StringUtils.substringBefore(sortField, "(").trim() ;
                    sortField = StringUtils.substringBetween(sortField, "(", ")") ;
                }
                String order = StringUtils.substringAfter(sorting.trim(), " ").trim();


                sorts.add(new SortField(order, sortTypeStr.equalsIgnoreCase("text") ? SortField.Type.STRING :
                        sortTypeStr.equalsIgnoreCase("number") ? SortField.Type.LONG : (sortTypeStr.equalsIgnoreCase("double") ? SortField.Type.DOUBLE : SortField.Type.STRING), order.equalsIgnoreCase("desc") ? true : false));
            }

            Sort sort = new Sort(sorts.toArray(new SortField[sorts.size()]));
            cacheQuery.sort(sort);
        } else {
//			Sort sort = new Sort( new SortField("nid", SortField.LONG, true));
//		    cacheQuery.sort(sort) ;
        }

        return cacheQuery;
    }
}
