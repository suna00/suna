package net.ion.ice.core.node;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.annotations.*;

import javax.persistence.Id;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 3. 31..
 */

@ProvidedId
@Indexed
public class Node implements Map<String, Object>, Serializable{
    public static final String ID = "id";
    public static final String TYPEID = "typeId";
    public static final String USERID = "userId";
    public static final String ANONYMOUS = "anonymous";
    public static final String SYSTEM = "system";

    @Id
    private Object id ;

    @Field
    @FieldBridge(impl = PropertiesFieldBridge.class)
    private Properties properties ;

    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.SECOND)
    private transient Date changed ;

    private transient NodeValue nodeValue ;

    public Node(){
        properties = new Properties() ;
    }

    public Node(Object id, String typeId){
        this(id, typeId, ANONYMOUS) ;
    }

    public Node(Object id, String typeId, String userId){
        this.id = id ;
        properties = new Properties() ;
        this.properties.setId(id) ;
        this.properties.setTypeId(typeId) ;
        this.nodeValue = new NodeValue(id, typeId, StringUtils.isEmpty(userId) ? ANONYMOUS : userId) ;
    }


    public Node(Map<String, Object> data, String typeId){
        construct(data, typeId);
    }


    public Node(Map<String, Object> data){
        String typeId = (String) data.get(TYPEID);
        if(typeId == null){
            throw new RuntimeException("TYPE ID is NULL");

        }
        construct(data, typeId) ;
    }

    private void construct(Map<String, Object> data, String typeId) {
        properties = new Properties() ;

        this.id = data.get(ID);
        if(this.id == null || StringUtils.isEmpty(this.id.toString())){
            List<String> idablePids = NodeUtils.getNodeType(typeId).getIdablePIds() ;
            for(int i = 0 ; i < idablePids.size(); i++){
                this.id = data.get(idablePids.get(i)) + (i < (idablePids.size() - 1) ? "/" : "") ;
            }
            if(this.id == null || StringUtils.isEmpty(this.id.toString())) {
                throw new RuntimeException("ID is NULL");
            }
        }

        this.putAll(data, typeId);

        this.properties.setId(id) ;
        this.properties.setTypeId(typeId) ;
        this.nodeValue = new NodeValue(id, typeId, data.get("userId") == null ? ANONYMOUS : data.get("userId").toString()) ;
    }

    @Override
    public boolean containsKey(Object key) {
        return properties.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return properties.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return properties.put(key, value);
    }



    @Override
    public int size() {
        return properties.size();
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public Object remove(Object key) {
        return properties.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        if(this.nodeValue != null && getTypeId() != null){
            putAll(m, getTypeId()) ;
        }else {
            properties.putAll(m);
        }
    }

    public void putAll(Map<? extends String, ?> m, String typeId) {
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;
        if(nodeType != null){
            properties.putAll(m, nodeType);
        }else{
            properties.putAll(m);
        }
    }

    @Override
    public void clear() {
        properties.clear();
    }

    @Override
    public Set<String> keySet() {
        return properties.keySet();
    }

    @Override
    public Collection<Object> values() {
        return properties.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return properties.entrySet();
    }

    public Object getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.nodeValue.setId(id) ;
    }

    public String getTypeId() {
        return nodeValue.getTypeId();
    }

    public void setTypeId(String typeId) {
        nodeValue.setTypeId(typeId) ;
    }

    @Override
    public String toString(){
        return this.properties.toString() ;
    }

    public NodeValue getNodeValue() {
        return nodeValue;
    }

    public void setNodeValue(NodeValue nodeValue) {
        this.changed = nodeValue.getChanged() ;
        this.nodeValue = nodeValue;
    }

    public boolean getBooleanValue(String pid){
        Object booleanValue = get(pid) ;
        if(booleanValue == null) return false ;
        if(booleanValue instanceof Boolean){
            return (boolean) booleanValue;
        }
        return Boolean.valueOf(booleanValue.toString()) ;
    }

    public String getStringValue(String pid){
        Object stringValue = get(pid) ;
        if(stringValue == null) return "" ;
        if(stringValue instanceof String){
            return (String) stringValue;
        }
        return stringValue.toString() ;
    }


    public String getSearchValue(){
        NodeType nodeType = NodeUtils.getNodeType(getTypeId()) ;
        StringBuffer searchValue = new StringBuffer() ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
            if(pt.isSeacheable()){
                searchValue.append(getStringValue(pt.getPid())) ;
            }
        }
        return searchValue.toString() ;
    }

    public boolean isNullValue(String pid) {
        Object stringValue = get(pid) ;
        if(stringValue == null) return true ;
        return false ;
    }

    public Object getValue(String pid) {
        return get(pid) ;
    }

    public String getLabel(NodeType nodeType) {
        for(PropertyType pt : nodeType.getPropertyTypes()){
            if(pt.isLabelable()){
                return getStringValue(pt.getPid()) ;
            }
        }
        return getId().toString() ;
    }

//    public Object getValue(String pid, PropertyType.ValueType valueType) {
//        Object value = get(pid) ;
//
//        switch(valueType){
//            case INT :
//
//        }
//    }
}
