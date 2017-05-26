package net.ion.ice.core.node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class NodeType {
    public static final String NODETYPE = "nodeType";

    private Node nodeTypeNode ;
    private Map<String, PropertyType> propertyTypes ;


    public NodeType(Node nodeTypeNode) {
        this.nodeTypeNode = nodeTypeNode ;
    }


    public void setPropertyTypes(List<Object> propertyTypeList){
        if(propertyTypes == null){
            propertyTypes = new LinkedHashMap<>() ;
        }
        for(Object _obj : propertyTypeList){
            PropertyType propertyType = (PropertyType) _obj;
            propertyTypes.put(propertyType.get("pid").toString(), propertyType) ;
        }
    }

    public PropertyType getPropertyType(String pid){
        return propertyTypes.get(pid) ;
    }

    public List<String> getIdablePIds() {
        List<String> ids = new ArrayList<>() ;
        for(PropertyType pt : propertyTypes.values()){
            if(pt.isIdable()){
                ids.add(pt.getPid()) ;
            }
        }
        return ids ;
    }

    public void addPropertyType(PropertyType propertyType) {
        if(propertyTypes == null){
            propertyTypes = new LinkedHashMap<>() ;
        }

        propertyTypes.put(propertyType.get("pid").toString(), propertyType) ;
    }
}
