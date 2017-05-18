package net.ion.ice.core.node;

import com.sun.tools.javac.util.List;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.json.JsonUtils;
import org.infinispan.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
            _nodeType.setPropertyTypes(infinispanRepositoryService.getQueryNodes("propertyType", "tid_matching=" + tid));
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

    private void initNodeType(Cache<String, Node> nodeTypeCache) {
        nodeType = new NodeType("nodeType", "nodeType") ;
        nodeType.put("tid", "nodeType") ;
        nodeType.put("repositoryType", "node") ;
        nodeType.put("typeName", "Node Type") ;

        nodeTypeCache.put("nodeType", nodeType) ;

        Node propertyType = new Node("propertyType", "nodeType") ;
        propertyType.put("tid", "propertyType") ;
        propertyType.put("repositoryType", "node") ;
        propertyType.put("typeName", "Property Type") ;

        nodeTypeCache.put("propertyType", propertyType) ;

        Cache<String, Node> propertyTypeCache = infinispanRepositoryService.getNodeCache("propertyType") ;

        Node tid = new Node("nodeType/tid", "propertyType") ;
        tid.put("tid", "nodeType") ;
        tid.put("pid", "tid") ;
        tid.put("propertyTypeName", "Type Id") ;
        tid.put("valueType", "ID") ;
        propertyTypeCache.put("nodeType/tid", tid) ;

        Node repositoryType = new Node("nodeType/repositoryType", "propertyType") ;
        repositoryType.put("tid", "nodeType") ;
        repositoryType.put("pid", "repositoryType") ;
        repositoryType.put("propertyTypeName", "Repository Type") ;
        repositoryType.put("valueType", "CODE") ;
        propertyTypeCache.put("nodeType/repositoryType", repositoryType) ;

        Node typeName = new Node("nodeType/typeName", "propertyType") ;
        typeName.put("tid", "nodeType") ;
        typeName.put("pid", "typeName") ;
        typeName.put("propertyTypeName", "Type Name") ;
        typeName.put("valueType", "NAME") ;
        propertyTypeCache.put("nodeType/typeName", typeName) ;

        Node propertyTid = new Node("propertyType/tid", "propertyType") ;
        tid.put("tid", "nodeType") ;
        tid.put("pid", "tid") ;
        tid.put("propertyTypeName", "Type Id") ;
        tid.put("valueType", "ID") ;
        propertyTypeCache.put("tid", tid) ;
    }

    public List<Node> getNodeList(String nodeType, Map<String, String[]> parameterMap) {

    }
}
