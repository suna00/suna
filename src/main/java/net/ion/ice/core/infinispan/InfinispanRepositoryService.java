package net.ion.ice.core.infinispan;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.infinispan.lucene.LuceneQueryUtils;
import net.ion.ice.core.infinispan.lucene.QueryType;
import net.ion.ice.core.node.*;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.query.FacetTerm;
import net.ion.ice.core.query.SimpleQueryResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.facet.Facet;
import org.infinispan.Cache;
import org.infinispan.query.CacheQuery;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Created by jaeho on 2017. 3. 31..
 */

@Service("infinispanService")
public class InfinispanRepositoryService {
    private Logger logger = LoggerFactory.getLogger(InfinispanRepositoryService.class);

    @Autowired
    private InfinispanCacheManager cacheManager;


    public Cache<String, Node> getNodeCache(String tid) {
        return cacheManager.getCache(tid, 100000);
    }

    public Node read(String typeId, String id) {
        Cache<String, Node> cache = getNodeCache(typeId);
        Node srcNode = cache.get(id);
        if (srcNode == null) {
            logger.error("Not found Node : {},{}", typeId, id) ;
            throw new NotFoundNodeException(typeId, id);
        }

        return srcNode;
    }


    public Node getNode(String typeId, String id) {
        try {
            Node srcNode = read(typeId, id);
            Node node = srcNode.clone();
            return node;
        } catch (Exception e) {
            return null;
        }
    }

    public Collection<Node> getNodes(String typeId) {
        return (Collection<Node>) getNodeCache(typeId).values();
    }


    public void startBatch(String typeId){
        Cache<String, Node> nodeCache = getNodeCache(typeId);
        nodeCache.startBatch() ;
    }

    public void endBatch(String typeId, boolean commit){
        Cache<String, Node> nodeCache = getNodeCache(typeId);
        nodeCache.endBatch(commit);
    }


    public Node execute(ExecuteContext context) {
        Node node = context.getNode();
        if (!context.isExecute()) return node;
        if(context.getEvent().equals("delete")){
            deleteNode(node);
            return node ;
        }
        cacheNode(node);
        return node.clone();
    }

    public void cacheNode(Node node) {
        Cache<String, Node> nodeCache = null ;

        try {
            nodeCache = getNodeCache(node.getTypeId());
            node.toStore();
            nodeCache.put(node.getId(), node);
        } catch (Exception e) {
            if(nodeCache != null){
                nodeCache.remove(node.getId()) ;
            }

            e.printStackTrace();
            logger.error(node.toString(), e);
        }
    }

    public void remove(ExecuteContext context) {
        deleteNode(context.getNode()) ;
    }

    public void deleteNode(Node node) {
        Cache<String, Node> nodeCache = getNodeCache(node.getTypeId());
        nodeCache.remove(node.getId().toString());
    }

    public void deleteNode(String typeId, String id) {
        Cache<String, Node> nodeCache = getNodeCache(typeId);
        nodeCache.remove(id);
    }

    private List<Object> executeQuery(String typeId, QueryContext queryContext) {
        Cache<String, Node> cache = getNodeCache(typeId);

        queryContext.setSearchManager(Search.getSearchManager(cache));

        CacheQuery cacheQuery = null;
        try {
            cacheQuery = LuceneQueryUtils.makeQuery(queryContext);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(cacheQuery == null){
            queryContext.setResultSize(0);
            queryContext.setQueryListSize(0) ;
            return new ArrayList<>() ;
        }


        List<Object> list = cacheQuery.list();
        queryContext.setResultSize(cacheQuery.getResultSize());
        queryContext.setQueryListSize(list.size()) ;
        if(queryContext.getFacetTerms() != null) {
            for (FacetTerm facet : queryContext.getFacetTerms()) {
                facet.setFacets(cacheQuery.getFacetManager().getFacets(facet.getName()));
            }
        }
        if(queryContext.getStart() > 0) {
            return list.subList(queryContext.getStart(), list.size()) ;
        }
        return list;
    }


    public SimpleQueryResult getQueryTreeNodes(String typeId, QueryContext queryContext) {
        NodeType nodeType = queryContext.getNodetype();
        for (PropertyType pt : nodeType.getPropertyTypes()) {
            if (pt.isTreeable()) {
                QueryContext subQueryContext = QueryContext.makeQueryContextForTree(nodeType, pt, queryContext.getQueryTerms() == null || queryContext.getQueryTerms().isEmpty() ? "root" : "");
                if (queryContext.getQueryTerms() != null && !queryContext.getQueryTerms().isEmpty()) {
                    subQueryContext.getQueryTerms().addAll(queryContext.getQueryTerms());
                }
                subQueryContext.setTreeable(true);
                subQueryContext.setLimit(queryContext.getLimit().toString());
                subQueryContext.setSorting(queryContext.getSorting());
                List<Node> result = getSubQueryNodes(pt.getReferenceType(), subQueryContext);
                for (Node node : result) {
                    node.toDisplay(subQueryContext);
                }
                return new SimpleQueryResult(result, subQueryContext);
            }
        }
        return null;
    }

    public SimpleQueryResult getQueryNodes(String typeId, QueryContext queryContext) {
//        queryContext.setIncludeReferenced(true);
        List<Node> result = getSubQueryNodes(typeId, queryContext);
        for (Node node : result) {
            node.toDisplay(queryContext);
        }
        return new SimpleQueryResult(result, queryContext);
    }


    public List<Node> getSubQueryNodes(String typeId, QueryContext queryContext) {
        List<Object> list = executeQuery(typeId, queryContext);

        NodeType nodeType = NodeUtils.getNodeType(typeId);
        List<Node> resultList = new ArrayList<>();
        for (Object item : list) {
            Node node = (Node) item;
            node = node.clone();

//            if (queryContext.isIncludeReferenced()) {
//                for (PropertyType pt : nodeType.getPropertyTypes(PropertyType.ValueType.REFERENCED)) {
//                    QueryContext subQueryContext = QueryContext.makeQueryContextForReferenced(nodeType, pt, node);
//                    node.put(pt.getPid(), getSubQueryNodes(pt.getReferenceType(), subQueryContext));
//                }
//            }
            if (queryContext.isTreeable()) {
                for (PropertyType pt : nodeType.getPropertyTypes()) {
                    if (pt.isTreeable()) {
                        QueryContext subQueryContext = QueryContext.makeQueryContextForTree(nodeType, pt, node.getId().toString());
                        subQueryContext.setTreeable(true);
//                        if (queryContext.getQueryTerms() != null) subQueryContext.getQueryTerms().addAll(queryContext.getQueryTerms());
                        if (queryContext.getSorting() != null ) subQueryContext.setSorting(queryContext.getSorting());
                        node.put("children", getSubQueryNodes(pt.getReferenceType(), subQueryContext));
                    }
                }
            }
            resultList.add(node);
        }

        return resultList;
    }

    public List<Map<String, Object>> getSyncQueryList(String typeId, QueryContext queryContext) {
        List<Object> list = executeQuery(typeId, queryContext);

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Object item : list) {
            Node node = (Node) item;
            resultList.add(node.toMap()) ;
        }
        return resultList;
    }

    public SimpleQueryResult getQueryCodeNodes(String typeId, QueryContext queryContext) {
        queryContext.setIncludeReferenced(false);
        List<Node> result = getSubQueryNodes(typeId, queryContext);
        for (Node node : result) {
            node.toCode();
        }
        return new SimpleQueryResult(result, queryContext);
    }

    public List<Object> executeQuery(QueryContext queryContext) {
        Cache<String, Node> cache = getNodeCache(queryContext.getNodetype().getTypeId());

        queryContext.setSearchManager(Search.getSearchManager(cache));

        CacheQuery cacheQuery = null;
        try {
            cacheQuery = LuceneQueryUtils.makeQuery(queryContext);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(cacheQuery == null){
            queryContext.setResultSize(0);
            queryContext.setQueryListSize(0) ;
            return new ArrayList<>() ;
        }

        List<Object> list = cacheQuery.list();
        queryContext.setResultSize(cacheQuery.getResultSize());
        queryContext.setQueryListSize(list.size()) ;

        if(queryContext.getFacetTerms() != null && queryContext.getFacetTerms().size() > 0){
            for(FacetTerm facetTerm : queryContext.getFacetTerms()){
                List<Facet> facets = cacheQuery.getFacetManager().getFacets(facetTerm.getName()) ;
                facetTerm.setFacets(facets) ;
            }
        }

        if(queryContext.getStart() > 0) {
            return list.subList(queryContext.getStart(), list.size()) ;
        }

        return list;
    }

    public Date getLastCacheNode(String typeId) {
        return (Date) getSortedValue(typeId,"changed", SortField.Type.LONG, true);
    }

    public Object getSortedValue(String typeId, String field, SortField.Type sortType, boolean reverse) {
        Cache<String, Node> nodeCache = getNodeCache(typeId);
        SearchManager qf = Search.getSearchManager(nodeCache);
        QueryBuilder queryBuilder = qf.buildQueryBuilderForClass(Node.class).get();

        CacheQuery cacheQuery = qf.getQuery(queryBuilder.all().createQuery());
        cacheQuery.sort(new Sort(new SortField(field, sortType, reverse)));
        cacheQuery.maxResults(1);

        List result = cacheQuery.list();
        if (result == null || result.size() == 0) return null;
        Node node = (Node) result.get(0);
        switch (field) {
            case "changed":
                return node.getChanged();
            default:
                return node.get(field);
        }
    }


    public static CacheQuery makeQuery(SearchManager qf, QueryBuilder queryBuilder, Node nodeType, Cache<String, Node> cache, Map<String, String[]> params) {
        if (qf == null) {
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
                if (value.contains(",")) {
                    for (String fromto : StringUtils.split(value, ",")) {
                        Object from = StringUtils.substringBefore(fromto, "~").trim();
                        Object to = StringUtils.substringAfter(fromto, "~").trim();
                        if (indexKey.startsWith("string")) {
                            shouldInnerQueries.add(new QueryType(bop, queryBuilder.range().onField(indexKey).ignoreAnalyzer().ignoreFieldBridge().from(from).to(to).createQuery()));
                        } else {
                            shouldInnerQueries.add(new QueryType(bop, queryBuilder.range().onField(indexKey).from(from).to(to).createQuery()));
                        }
                    }
                } else {
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
                } else if (bop.equals("should")) {
                    shouldInnerQueries.add(innerQueryType);
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
                baseQuery.must(shouldSubQuery.createQuery());
            } else if (shouldInnerQueries.size() == 1) {
                baseQuery.should(shouldInnerQueries.get(0).getQuery());
            }
            cacheQuery = qf.getQuery(baseQuery.createQuery());
        }

        if (params.containsKey("sorting") && params.get("sorting") != null && StringUtils.isNotBlank(params.get("sorting")[0])) {
            String[] sortings = StringUtils.split(params.get("sorting")[0], ",");
            List<SortField> sorts = new ArrayList<SortField>();
            for (String sorting : sortings) {
                String sortField = StringUtils.substringBefore(sorting.trim(), " ").trim();
                String sortTypeStr = null;
                if (StringUtils.contains(sortField, "(")) {
                    sortTypeStr = StringUtils.substringBefore(sortField, "(").trim();
                    sortField = StringUtils.substringBetween(sortField, "(", ")");
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


    public void rebuild(String typeId) {
        Cache<String, Node> cache = getNodeCache(typeId) ;

//        SearchManager searchManager = Search.getSearchManager(cache);
//        searchManager.getMassIndexer().start();

        for(Node node : cache.values()){
            cache.put(node.getId(), node.toIndexing()) ;
        }
    }
}
