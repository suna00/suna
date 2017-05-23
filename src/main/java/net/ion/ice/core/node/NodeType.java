package net.ion.ice.core.node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class NodeType extends Node{
    public static final String NODETYPE = "nodeType";

    private transient Map<String, PropertyType> propertyTypes ;

    public NodeType(String id, String tid) {
        super(id, tid) ;
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

    public Node getPropertyType(String pid){
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
}
