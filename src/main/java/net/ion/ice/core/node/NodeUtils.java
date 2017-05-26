package net.ion.ice.core.node;

import java.util.List;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 5. 17..
 */
public class NodeUtils {


    public static List<Node> makeNodeList(Collection<Map<String, Object>> nodeDataList) {
        List<Node> nodeList = new ArrayList<Node>();
        for(Map<String, Object> data : nodeDataList){
            nodeList.add(makeNode(data)) ;
        }
        return nodeList ;
    }

    public static Node makeNodeType(Map<String, Object> data) {
        Node node =  new Node(data.get("id"), NodeType.NODETYPE) ;
        nodeType.putAll(data);

        return nodeType ;
    }


    public static List<NodeType> makeNodeTypeList(Collection<Map<String, Object>> nodeTypeDataList) {
        List<NodeType> nodeTypeList = new ArrayList<NodeType>();
        for(Map<String, Object> data : nodeTypeDataList){
            nodeTypeList.add(makeNodeType(data)) ;
        }
        return nodeTypeList ;
    }

    public static NodeType makeNodeType(Map<String, Object> data) {
        String tid = data.get("tid") == null ? data.get("id").toString() : data.get("tid").toString() ;
        NodeType nodeType =  new NodeType(tid, NodeType.NODETYPE) ;
        nodeType.putAll(data);

        return nodeType ;
    }



    public static PropertyType makePropertyType(Map<String, Object> data) {
        Object id = data.get("id");
        if(id == null){
            id = data.get("tid") + "/" + data.get("pid") ;
        }

        PropertyType propertyType =  new PropertyType((String) id, PropertyType.PROPERTYTYPE) ;
        propertyType.putAll(data);

        return propertyType ;
    }

    public static List<PropertyType> makePropertyTypeList(Collection<Map<String, Object>> propertyTypeDataList) {
        List<PropertyType> propertyTypeList = new ArrayList<PropertyType>();
        for(Map<String, Object> data : propertyTypeDataList){
            propertyTypeList.add(makePropertyType(data)) ;
        }
        return propertyTypeList ;
    }
}
