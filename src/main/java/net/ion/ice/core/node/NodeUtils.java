package net.ion.ice.core.node;

import java.util.List;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 5. 17..
 */
public class NodeUtils {

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

    public static List<PropertyType> makePropertyTypeList(Collection<Map<String, Object>> propertyTypeDataList) {

    }


    public static PropertyType makePropertyType(Map<String, Object> data) {
        Object id = data.get("id");
        if(id == null){
            id = data.get("tid") + "/" + data.get("pid") ;
        }

        PropertyType propertyType =  new PropertyType(id, PropertyType.PROPERTYTYPE) ;
        propertyType.putAll(data);

        return propertyType ;
    }
}
