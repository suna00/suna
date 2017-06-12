package net.ion.ice.core.infinispan;

import net.ion.ice.core.infinispan.lucene.LuceneQueryUtils;
import net.ion.ice.core.infinispan.lucene.QueryType;
import net.ion.ice.core.node.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
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

@Service
public class InfinispanRepositoryService {
    private Logger logger = LoggerFactory.getLogger(InfinispanRepositoryService.class);

    public static final String NODEVALUE_SEPERATOR = "://";
    @Autowired
    private InfinispanCacheManager cacheManager ;

    @Autowired
    private NodeService nodeService ;

    public Cache<String, Node> getNodeCache(String tid){
        return cacheManager.getCache(tid, 100000) ;
    }


    public Cache<String, NodeValue> getNodeValueCache(){
        return cacheManager.getCache("nodeValue", 100000) ;
    }


    private Node initNode(String typeId, Node srcNode) {
        if(srcNode.getNodeValue() == null) {
            Cache<String, NodeValue> nodeValueCache = getNodeValueCache();
            srcNode.setNodeValue(nodeValueCache.get(typeId + NODEVALUE_SEPERATOR + srcNode.getId()));
        }
        Node node = srcNode.clone() ;

        return node ;
    }

    public Node read(String typeId, String id) {
        Cache<String, Node> cache = getNodeCache(typeId);
        Node srcNode = cache.get(id) ;
        if(srcNode == null){
            throw new NotFoundNodeException(typeId, id) ;
        }

        return srcNode ;
    }


    public Node getNode(String typeId, String id) {
        try {
            Node srcNode = read(typeId, id);
            Node node = initNode(typeId, srcNode);
            return node;
        }catch(NotFoundNodeException e){
            return null ;
        }
    }

    public Collection<Node> getNodes(String typeId) {
        return (Collection<Node>) getNodeCache(typeId).values();
    }

    public Node execute(ExecuteContext context) {
        Node node = context.getNode() ;
        if(!context.isExecute()) return  node ;
        try {
            Cache<String, Node> nodeCache = getNodeCache(node.getTypeId());
            nodeCache.put(node.getId().toString(), node);

            Cache<String, NodeValue> nodeValueCache = getNodeValueCache();
            node.getNodeValue().setContent(node.getSearchValue());
            nodeValueCache.put(node.getTypeId() + NODEVALUE_SEPERATOR + node.getId(), node.getNodeValue());
        }catch(Exception e){
            logger.error(node.toString(), e);
        }
        return node ;
    }


    public void deleteNode(Node node) {
        Cache<String, Node> nodeCache = getNodeCache(node.getTypeId());
        nodeCache.remove(node.getId().toString()) ;

        Cache<String, NodeValue> nodeValueCache = getNodeValueCache() ;
        nodeValueCache.remove(node.getTypeId() + NODEVALUE_SEPERATOR + node.getId()) ;
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

        List<Object> list = cacheQuery.list();
        queryContext.setResultSize(cacheQuery.getResultSize());
        return list;
    }


    public QueryResult getQueryTreeNodes(String typeId, QueryContext queryContext) {
        NodeType nodeType = queryContext.getNodetype() ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
            if(pt.isTreeable()){
                QueryContext subQueryContext = QueryContext.makeQueryContextForTree(nodeType, pt, "root") ;
                subQueryContext.setTreeable(true);
                return new QueryResult(getSubQueryNodes(pt.getReferenceType(), subQueryContext), subQueryContext) ;
            }
        }
        return null ;
    }

    public QueryResult getQueryNodes(String typeId, QueryContext queryContext){
        queryContext.setIncludeReference(true);
        return new QueryResult(getSubQueryNodes(typeId, queryContext), queryContext) ;
    }



    public List<Node> getSubQueryNodes(String typeId, QueryContext queryContext){
        List<Object> list = executeQuery(typeId, queryContext);

        NodeType nodeType = queryContext.getNodetype() ;

        boolean hasReferenced = nodeType.hasReferenced() ;
        List<Node> resultList = new ArrayList<Node>() ;
        for(Object item : list){
            Node node = (Node) item;
            node = initNode(typeId, node);

            if(queryContext.isIncludeReferenced()){
                for(PropertyType pt : nodeType.getPropertyTypes(PropertyType.ValueType.REFERENCED)){
                    QueryContext subQueryContext = QueryContext.makeQueryContextForReferenced(nodeType, pt, node) ;
                    node.put(pt.getPid(), getSubQueryNodes(pt.getReferenceType(), subQueryContext)) ;
                }
            }
            if(queryContext.isTreeable()) {
                for (PropertyType pt : nodeType.getPropertyTypes()) {
                    if (pt.isTreeable()) {
                        QueryContext subQueryContext = QueryContext.makeQueryContextForTree(nodeType, pt, node.getId().toString());
                        subQueryContext.setTreeable(true);
                        node.put("children", getSubQueryNodes(pt.getReferenceType(), subQueryContext));
                    }
                }
            }
            resultList.add(node) ;
        }

        return resultList;
    }


    public NodeValue getLastCacheNodeValue() {
        Cache<String, NodeValue> nodeValueCache = getNodeValueCache() ;
        SearchManager qf = Search.getSearchManager(nodeValueCache) ;
        QueryBuilder queryBuilder = qf.buildQueryBuilderForClass(NodeValue.class).get();

        CacheQuery cacheQuery = qf.getQuery(queryBuilder.all().createQuery());
        cacheQuery.sort(new Sort(new SortField("changed", SortField.Type.LONG, true))) ;
        cacheQuery.maxResults(1) ;

        List result = cacheQuery.list() ;
        return (NodeValue) result.get(0);
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
