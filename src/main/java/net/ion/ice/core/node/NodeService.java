package net.ion.ice.core.node;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.infinispan.QueryContext;
import net.ion.ice.core.infinispan.QueryTerm;
import net.ion.ice.core.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private Map<String, NodeType> nodeTypeCache = new ConcurrentHashMap<>() ;

    @PostConstruct
    public void init(){
        try {
            initNodeType(true) ;
        } catch (IOException e) {
            e.printStackTrace();
        }

        NodeUtils.setNodeService(this) ;

        try {
            initNodeType(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public NodeType getNodeType(String typeId) {
        if(nodeTypeCache.containsKey(typeId)) {
            return nodeTypeCache.get(typeId) ;
        }

        if(typeId.equals("nodeType")){
            try {
                return getDefaultNodeType() ;
            } catch (IOException e) {
                logger.error("NODE TYPE INIT ERROR : ", e) ;
                throw new RuntimeException("INIT ERROR") ;
            }
        }else if(typeId.equals("propertyType")){
            try {
                return getDefaultPropertyType() ;
            } catch (IOException e) {
                throw new RuntimeException("INIT ERROR") ;
            }
        }


        Node nodeTypeNode = infinispanRepositoryService.getNode("nodeType", typeId) ;

        NodeType nodeType = new NodeType(nodeTypeNode) ;
        nodeType.setPropertyTypes(getNodeList("propertyType", "tid_matching=" + typeId).getResultList());

        nodeTypeCache.put(typeId, nodeType) ;
        return nodeType ;
    }



    public NodeType getDefaultNodeType() throws IOException {
        if(nodeType == null){
            initNodeType(true);
        }
        return nodeType;
    }

    public NodeType getDefaultPropertyType() throws IOException {
        if(propertyType == null){
            initNodeType(true);
        }
        return propertyType;
    }

    public QueryResult getNodeList(String typeId, String searchText) {
        QueryContext queryContext = QueryContext.makeQueryContextFromText(searchText, getNodeType(typeId)) ;
        return infinispanRepositoryService.getQueryNodes(typeId, queryContext) ;
    }


    public QueryResult getNodeList(String typeId, Map<String, String[]> parameterMap) {
        QueryContext queryContext = QueryContext.makeQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        return infinispanRepositoryService.getQueryNodes(typeId, queryContext) ;
    }

    public QueryResult getNodeTree(String typeId, Map<String, String[]> parameterMap) {
        QueryContext queryContext = QueryContext.makeQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        return infinispanRepositoryService.getQueryTreeNodes(typeId, queryContext) ;
    }


    private void initNodeType(boolean preConstruct) throws IOException {
        Collection<Map<String, Object>> nodeTypeDataList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("classpath:schema/node/nodeType.json")) ;

        List<Node> nodeTypeList = NodeUtils.makeNodeList(nodeTypeDataList, "nodeType") ;
        for(Node nodeType : nodeTypeList){
            if(preConstruct) {
                if (nodeType.getId().equals("nodeType")) {
                    this.nodeType = new NodeType(nodeType);
                } else if (nodeType.getId().equals("propertyType")) {
                    this.propertyType = new NodeType(nodeType);
                }
            }else {
                saveNode(nodeType);
            }
        }

        Collection<Map<String, Object>> propertyTypeDataList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("classpath:schema/node/propertyType.json")) ;

        List<Node> propertyTypeList = NodeUtils.makeNodeList(propertyTypeDataList, "propertyType") ;
        for(Node propertyType : propertyTypeList){
            if(preConstruct) {
                if (propertyType.get("tid").equals("nodeType")) {
                    this.nodeType.addPropertyType(new PropertyType(propertyType));
                } else if (propertyType.get("tid").equals("propertyType")) {
                    this.propertyType.addPropertyType(new PropertyType(propertyType));
                }
            }else {
                saveNode(propertyType);
            }
        }

        if(!preConstruct) {

            Resource resource = ApplicationContextManager.getResource("classpath:schema/node") ;
            File initNodeDir =  resource.getFile() ;

            NodeValue nodeValue = infinispanRepositoryService.getLastCacheNodeValue() ;
            String lastChanged = DateTools.dateToString(nodeValue.getChanged(), DateTools.Resolution.SECOND);

            logger.info("LAST CHANGED : " + lastChanged);
            for(File dir : initNodeDir.listFiles((File f) -> { return f.isDirectory() ; })){
                for(File f : dir.listFiles((File f) -> {return f.getName().endsWith(".json");})){
                    String fileName = StringUtils.substringBefore(f.getName(), ".json");
                    Collection<Map<String, Object>> nodeDataList = JsonUtils.parsingJsonFileToList(f) ;

                    if(fileName.startsWith("20") && fileName.length() == 14 && lastChanged.compareTo(fileName) < 0){
                        List<Node> nodeList = NodeUtils.makeNodeList(nodeDataList) ;
                        nodeList.forEach(node -> saveNode(node));
                    }else{
                        List<Node> nodeList = NodeUtils.makeNodeListFilterBy(nodeDataList, lastChanged) ;
                        nodeList.forEach(node -> saveNode(node));
                    }
                }
            }
        }

    }


    public void saveNode(Node node) {
        infinispanRepositoryService.saveNode(node);
    }

    public Node saveNode(Map<String, String[]> parameterMap, String typeId) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeContextFormParameter(parameterMap, nodeType) ;

        Node node = context.getNode() ;
        infinispanRepositoryService.saveNode(node);
        return node ;
    }

    public Node deleteNode(Map<String, String[]> parameterMap, String typeId) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeContextFormParameter(parameterMap, nodeType) ;
        Node node = context.getNode() ;
        infinispanRepositoryService.deleteNode(node) ;
        return node ;
    }

    public Node readNode(Map<String, String[]> parameterMap, String typeId, String id) {
        return infinispanRepositoryService.getNode(typeId, id) ;
    }

    public Node readNode(Map<String, String[]> parameterMap, String typeId) {
        String id = null ;
        for(String paramName : parameterMap.keySet()){
            if(paramName.equals("id")){
                id = parameterMap.get(paramName)[0] ;
            }
        }

        if(id == null){
            List<String> idablePids = NodeUtils.getNodeType(typeId).getIdablePIds() ;
            for(int i = 0 ; i < idablePids.size(); i++){
                id = parameterMap.get(idablePids.get(i))[0] + (i < (idablePids.size() - 1) ? "/" : "") ;
            }
        }

        return infinispanRepositoryService.getNode(typeId, id) ;

    }

    public Node getNode(String typeId, String id) {
        return infinispanRepositoryService.getNode(typeId, id) ;
    }
}
