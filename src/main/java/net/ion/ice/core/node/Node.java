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
public class Node implements Map<String, Object>, Serializable, Cloneable{
    public static final String ID = "id";
    public static final String TYPEID = "typeId";
    public static final String USERID = "userId";
    public static final String ANONYMOUS = "anonymous";
    public static final String SYSTEM = "system";

    public static final String ID_SEPERATOR = ">";

    @Id
    private String id ;

    @Field
    @FieldBridge(impl = PropertiesFieldBridge.class)
    private Properties properties ;

    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.SECOND)
    @SortableField()
    private Date changed ;

    private transient NodeValue nodeValue ;

    public Node(){
        properties = new Properties() ;
    }

    public Node(String id, String typeId){
        this(id, typeId, ANONYMOUS) ;
    }

    public Node(String id, String typeId, String userId){
        this.id = id ;
        properties = new Properties() ;
        this.properties.setId(id) ;
        this.properties.setTypeId(typeId) ;
        this.nodeValue = new NodeValue(id, typeId, StringUtils.isEmpty(userId) ? ANONYMOUS : userId, new Date()) ;
    }


    public Node(Map<String, Object> data, String typeId){
        construct(data, typeId, data.get(USERID) == null ? ANONYMOUS : data.get(USERID).toString());
    }

    public Node(Map<String, Object> data, String typeId, String userId){
        construct(data, typeId, userId);
    }


    public Node(Map<String, Object> data){
        properties = new Properties() ;
        this.id = data.get(ID).toString();
        this.putAll(data);
        this.properties.setId(id) ;
    }


    private void construct(Map<String, Object> data, String typeId, String userId) {
        properties = new Properties() ;
        if(data.containsKey(ID)) {
            this.id = data.get(ID).toString();
        }

        if(isNullId()){
            List<PropertyType> idablePts = NodeUtils.getNodeType(typeId).getIdablePropertyTypes() ;
            if(idablePts.size() > 1) {
                id = "";
                for (int i = 0; i < idablePts.size(); i++) {
                    Object _id = data.get(idablePts.get(i).getPid());
                    if (_id == null || StringUtils.isEmpty(_id.toString())) {
                        throw new RuntimeException("ID is NULL");
                    }
                    if (StringUtils.contains(_id.toString(), Node.ID_SEPERATOR)) {
                        this.id = _id.toString() + ID_SEPERATOR + StringUtils.removeEnd(id, Node.ID_SEPERATOR) ;
                    } else {
                        this.id = id + _id + (i < (idablePts.size() - 1) ? ID_SEPERATOR : "");
                    }
                }
            }else{
                PropertyType idPropertyType = idablePts.get(0) ;
                this.id = data.containsKey(idPropertyType.getPid())  ? data.get(idPropertyType.getPid()).toString() : null;
                if(isNullId()){
                    switch (idPropertyType.getIdType()){
                        case UUID:{
                            this.id = UUID.randomUUID().toString() ;
                            data.put(idPropertyType.getPid(), this.id) ;
                            break ;
                        }
                        case autoIncrement:{
                            Long seq = NodeUtils.getSequenceValue(typeId) ;
                            this.id = seq.toString() ;
                            data.put(idPropertyType.getPid(), seq) ;
                            break ;
                        }
                    }

                }
            }
        }

        this.putAll(data, typeId);

        this.properties.setId(id) ;
        this.properties.setTypeId(typeId) ;
        this.changed = new Date() ;
        this.nodeValue = new NodeValue(id, typeId, userId, changed) ;
    }

    private boolean isNullId() {
        return this.id == null || StringUtils.isEmpty(this.id.toString());
    }

    @Override
    public boolean containsKey(Object key) {
        return properties.containsKey(key) || NodeValue.containsKey(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        Object value = null ;
        if(nodeValue != null && nodeValue.containsKey(key.toString())){
            value = nodeValue.getValue(key.toString()) ;
        }else{
            value = properties.get(key)  ;
        }
        return value ;
    }

    @Override
    public Object put(String key, Object value) {
        if(nodeValue != null && NodeValue.containsKey(key)){
            if(properties.containsKey(key)){
                properties.remove(key) ;
            }
            return nodeValue.putValue(key, value) ;
        }
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
        if(nodeType != null && nodeType.isInit()){
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

    public String getId() {
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
        if(nodeValue != null) {
            this.changed = nodeValue.getChanged();
            this.nodeValue = nodeValue;
        }
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
        Object value = get(pid)  ;
        if(value == null && nodeValue.containsKey(pid)){
            value = nodeValue.getValue(pid) ;
        }
        return value ;
    }

    public String getLabel(NodeType nodeType) {
        for(PropertyType pt : nodeType.getPropertyTypes()){
            if(pt.isLabelable()){
                return getStringValue(pt.getPid()) ;
            }
        }
        return getId().toString() ;
    }

    public Date getChanged(){
        return changed ;
    }

    public Node clone(){
        Node cloneNode = new Node() ;
        cloneNode.properties = properties.clone() ;
        if(nodeValue != null) {
            cloneNode.nodeValue = nodeValue.clone();
            cloneNode.properties.setTypeId(getTypeId()) ;
        }

        cloneNode.id = getId();
        cloneNode.changed = changed ;
        cloneNode.properties.setId(id) ;
        return cloneNode ;
    }

    public Node clone(String typeId){
        Node cloneNode = new Node() ;
        cloneNode.properties = properties.clone() ;
        if(nodeValue != null) {
            cloneNode.nodeValue = nodeValue.clone();
        }

        cloneNode.id = getId();
        cloneNode.changed = changed ;
        cloneNode.properties.setId(id) ;
        cloneNode.properties.setTypeId(typeId) ;
        return cloneNode ;
    }
    public void setUpdate(String userId, Date changed) {
        this.changed = changed;
        this.nodeValue.setModifier(userId) ;
        this.nodeValue.setChanged(changed) ;
    }

    public Node toDisplay() {
        this.properties.toDisplay(this.nodeValue);
        return this ;
    }


    public Node toCode() {
        this.properties.toCode();
        return this ;
    }

    public Node toStore() {
        NodeType nodeType = NodeUtils.getNodeType(getTypeId()) ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
            Object value = NodeUtils.getStoreValue(this, pt, this.id) ;

            if(value != null){
                put(pt.getPid(), value);
            }else{
                remove(pt.getPid()) ;
            }
        }
//        properties.toStore();
        return this ;
    }

    public Object getStoreValue(String pid) {
        return NodeUtils.getStoreValue(getValue(pid), NodeUtils.getNodeType(getTypeId()).getPropertyType(pid), getId()) ;
    }

    public Object getBindingValue(String pid) {
        return NodeUtils.getBindingValue(getValue(pid), NodeUtils.getNodeType(getTypeId()).getPropertyType(pid), getId()) ;
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
