package net.ion.ice.core.node;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.infinispan.QueryContext;
import net.ion.ice.core.infinispan.QueryTerm;
import net.ion.ice.core.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jaeho on 2017. 4. 3..
 */
@Service("nodeService")
public class NodeService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private InfinispanRepositoryService infinispanRepositoryService ;

    private NodeType nodeType ;
    private NodeType propertyType ;

    public NodeType getNodeType(String tid) {
        if(tid.equals("nodeType")){
            try {
                return getDefaultNodeType() ;
            } catch (IOException e) {
                logger.error("NODE TYPE INIT ERROR : ", e) ;
                throw new RuntimeException("INIT ERROR") ;
            }
        }else if(tid.equals("propertyType")){
            try {
                return getDefaultPropertyType() ;
            } catch (IOException e) {
                throw new RuntimeException("INIT ERROR") ;
            }
        }

        Cache<String, Node> nodeCache = infinispanRepositoryService.getNodeCache("nodeType") ;
        Node nodeTypeNode = nodeCache.get(tid) ;

        Cache<String, NodeValue> nodeValueCache = infinispanRepositoryService.getNodeValueCache() ;
        nodeTypeNode.setNodeValue(nodeValueCache.get(nodeTypeNode.getId())) ;

        NodeType nodeType = new NodeType(nodeTypeNode) ;
        nodeType.setPropertyTypes(getNodeList("propertyType", "tid_matching=" + tid).getResultList());


        return nodeType ;
    }



    public NodeType getDefaultNodeType() throws IOException {
        if(nodeType == null){
            initNodeType();
        }
        return nodeType;
    }

    public NodeType getDefaultPropertyType() throws IOException {
        if(propertyType == null){
            initNodeType();
        }
        return propertyType;
    }

    public QueryResult getNodeList(String nodeType, String searchText) {
        QueryContext queryContext = makeQueryContextFromText(searchText, getNodeType(nodeType)) ;
        return infinispanRepositoryService.getQueryNodes(nodeType, queryContext) ;
    }


    public QueryResult getNodeList(String nodeType, Map<String, String[]> parameterMap) {
        QueryContext queryContext = makeQueryContextFromParameter(parameterMap, getNodeType(nodeType)) ;

        return infinispanRepositoryService.getQueryNodes(nodeType, queryContext) ;
    }

    private QueryContext makeQueryContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        QueryContext queryContext = new QueryContext() ;
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        if(parameterMap == null || parameterMap.size() == 0){
            return queryContext ;
        }

        for (String paramName : parameterMap.keySet()) {

            String[] values = parameterMap.get(paramName);
            if(values== null || StringUtils.isEmpty(values[0])){
                continue;
            }

            String value = StringUtils.join(values, ' ') ;

            makeQueryTerm(nodeType, queryContext, queryTerms, paramName, value);

        }
        queryContext.setQueryTerms(queryTerms);
        return queryContext ;
    }


    private QueryContext makeQueryContextFromText(String searchText, NodeType nodeType) {
        QueryContext queryContext = new QueryContext() ;
        java.util.List<QueryTerm> queryTerms = new ArrayList<>();

        if(StringUtils.isEmpty(searchText)){
            return queryContext ;
        }

        for (String param : StringUtils.split(searchText, '&')) {
            if (StringUtils.isNotEmpty(param) && StringUtils.contains(param, "=")) {
                String value = StringUtils.substringAfter(param, "=");
                if(StringUtils.isEmpty(value)){
                    continue;
                }
                String paramName = StringUtils.substringBefore(param, "=") ;

                if(StringUtils.isEmpty(paramName)){
                    continue ;
                }

                makeQueryTerm(nodeType, queryContext, queryTerms, paramName, value);
            }
        }
        queryContext.setQueryTerms(queryTerms);
        return queryContext ;
    }

    private void makeQueryTerm(NodeType nodeType, QueryContext queryContext, java.util.List<QueryTerm> queryTerms, String paramName, String value) {
        value = value.equals("@sysdate") ? new SimpleDateFormat("yyyyMMdd HHmmss").format(new Date()) : value.equals("@sysday") ? new SimpleDateFormat("yyyyMMdd").format(new Date()) : value;

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
                QueryTerm queryTerm = makePropertyQueryTerm(nodeType, queryTerms, fieldId, method, value) ;
                if(queryTerm == null){
                    queryTerm = makePropertyQueryTerm(nodeType, queryTerms, paramName, "matching", value) ;
                }

                if(queryTerm != null ){
                    queryTerms.add(queryTerm) ;
                }

            } else {

                queryTerms.add(new QueryTerm(paramName, value));
            }

        }
    }

    private QueryTerm makePropertyQueryTerm(NodeType nodeType, java.util.List<QueryTerm> queryTerms, String fieldId, String method, String value) {
        PropertyType propertyType = (PropertyType) nodeType.getPropertyType(fieldId);
        if(propertyType != null && propertyType.indexing()) {
            return new QueryTerm(fieldId, propertyType.getAnalyzer(), method, value);
        }
        return null ;
    }

    private void initNodeType() throws IOException {
        Collection<Map<String, Object>> nodeTypeDataList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("classpath:schema/node/nodeType.json")) ;

        List<NodeType> nodeTypeList = NodeUtils.makeNodeTypeList(nodeTypeDataList) ;
        for(NodeType nodeType : nodeTypeList){
            saveNodeType(nodeType);
        }

        Collection<Map<String, Object>> propertyTypeDataList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("classpath:schema/node/propertyType.json")) ;

        List<PropertyType> propertyTypeList = NodeUtils.makePropertyTypeList(propertyTypeDataList) ;
        for(PropertyType propertyType : propertyTypeList){
            savePropertyType(propertyType);
        }
    }



}
