package net.ion.ice.core.node;

import sun.security.krb5.internal.crypto.EType;

import java.util.*;
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


    public void setPropertyTypes(List<Node> propertyTypeList){
        if(propertyTypes == null){
            propertyTypes = new LinkedHashMap<>() ;
        }
        for(Node _node : propertyTypeList){
            PropertyType propertyType = new PropertyType(_node);
            propertyTypes.put(propertyType.getPid(), propertyType) ;
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

        propertyTypes.put(propertyType.getPid(), propertyType) ;
    }

    public String getTypeId() {
        return nodeTypeNode.getId().toString();
    }

    public Collection<PropertyType> getPropertyTypes() {
        return propertyTypes.values();
    }

    public boolean hasReferenced() {
        for(PropertyType pt : propertyTypes.values()){
            if(pt.isReferenced()) return true ;
        }
        return false ;
    }

    public Collection<PropertyType> getPropertyTypes(PropertyType.ValueType valueType) {
        List<PropertyType> results = new ArrayList<PropertyType>() ;

        for(PropertyType pt : propertyTypes.values()){
            if(pt.getValueType() == valueType){
                results.add(pt) ;
            }
        }
        return results ;
    }
}
