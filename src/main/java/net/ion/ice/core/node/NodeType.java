package net.ion.ice.core.node;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class NodeType extends Node{

    private transient Map<String, Node> propertyTypes ;

    public NodeType(String id, String tid) {
        super(id, tid) ;
    }


    public void setPropertyTypes(List<Object> propertyTypeList){
        if(propertyTypes == null){
            propertyTypes = new ConcurrentHashMap<>() ;
        }
        for(Object _obj : propertyTypeList){
            Node propertyType = (Node) _obj;
            propertyTypes.put(propertyType.get("pid").toString(), propertyType) ;
        }
    }

    public Node getPropertyType(String pid){
        return propertyTypes.get(pid) ;
    }

}
