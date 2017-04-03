package net.ion.ice.node;

import net.ion.ice.infinispan.InfinispanRepositoryService;
import org.infinispan.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by jaeho on 2017. 4. 3..
 */
@Service
public class NodeService {

    @Autowired
    private InfinispanRepositoryService infinispanRepositoryService ;

    private Node nodeType ;

    public Node getNodeType(String tid){
        if(tid.equals("nodeType")){
            return getNodeTypeNode() ;
        }
        Node nodeType = infinispanRepositoryService.getNode("nodeType", tid) ;
        nodeType.put("properties", infinispanRepositoryService.getQueryNodes("propertyType", "typeId_matching=") + tid) ;

        return nodeType ;
    }


    public Node getNodeTypeNode() {
        if(nodeType == null){

            Cache<String, Node> nodeTypeCache = infinispanRepositoryService.getNodeCache("nodeType") ;

            nodeType = new Node("nodeType", "nodeType") ;
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
        return nodeType;
    }
}
