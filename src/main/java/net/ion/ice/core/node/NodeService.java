package net.ion.ice.core.node;

import com.sun.tools.javac.util.List;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.infinispan.QueryContext;
import net.ion.ice.core.infinispan.QueryTerm;
import net.ion.ice.core.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinispan.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Created by jaeho on 2017. 4. 3..
 */
@Service("nodeService")
public class NodeService {

    @Autowired
    private InfinispanRepositoryService infinispanRepositoryService ;

    private NodeType nodeType ;

    public NodeType getNodeType(String tid){
        if(tid.equals("nodeType")){
            return getNodeTypeNode() ;
        }
        Node nodeTypeNode = infinispanRepositoryService.getNode("nodeType", tid) ;

        if(nodeTypeNode != null) {
            NodeType _nodeType = (NodeType) nodeTypeNode;
            _nodeType.setPropertyTypes(getNodeList("propertyType", "tid_matching=" + tid).getResultList());
        }

        return null ;
    }

    public NodeType saveNodeType(String tid){
        if(tid.equals("nodeType")){
            return getNodeTypeNode() ;
        }
        Node nodeTypeNode = infinispanRepositoryService.getNode("nodeType", tid) ;

        if(nodeTypeNode != null) {
            NodeType _nodeType = (NodeType) nodeTypeNode;
            _nodeType.setPropertyTypes(getNodeList("propertyType", "tid_matching=" + tid).getResultList());
        }

        return null ;
    }


    public Node getPropertyType(String tid, String pid){
        Node propertyType = infinispanRepositoryService.getNode("propertyType", tid + "/" + pid) ;
        return propertyType ;
    }

    public NodeType getNodeTypeNode() {
        if(nodeType == null){
            Cache<String, Node> nodeTypeCache = infinispanRepositoryService.getNodeCache("nodeType") ;
            if(nodeTypeCache == null || nodeTypeCache.size() == 0){
                Resource configFilePath = ApplicationContextManager.getResource("nodeType.json") ;
                if(configFilePath.exists()){
                    try {
                        Map<String, Object> configSrc = JsonUtils.parsingJsonFileToMap(configFilePath.getFile()) ;
                    } catch (IOException e) {
                    }
                }
                if(nodeType == null) {
                    initNodeType(nodeTypeCache);
                }
            }
        }
        return nodeType;
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
        Collection<Map<String, Object>> nodeTypeList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("/schema/node/nodeType.json")) ;


    }


}
