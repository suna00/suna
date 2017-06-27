package net.ion.ice.core.node;

import org.stagemonitor.util.StringUtils;

import java.util.*;

/**
 * Created by jaeho on 2017. 5. 15..
 */
public class NodeType {
    public static final String NODETYPE = "nodeType";
    public static final String TABLE_NAME = "tableName";

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

    public List<PropertyType> getIdablePropertyTypes() {
        List<PropertyType> idPts = new ArrayList<>() ;
        for(PropertyType pt : propertyTypes.values()){
            if(pt.isIdable()){
                idPts.add(pt) ;
            }
        }
        return idPts ;
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

    public boolean isInit(){
        return this.propertyTypes != null && this.propertyTypes.size() > 0 ;
    }

    public boolean hasTableName() {
        return nodeTypeNode.get(TABLE_NAME) != null && StringUtils.isNotEmpty((String) nodeTypeNode.get(TABLE_NAME));
    }
}
