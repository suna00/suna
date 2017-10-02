package net.ion.ice.core.node;

import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.context.ReadContext;
import net.ion.ice.core.infinispan.lucene.CodeAnalyzer;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.annotations.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 3. 31..
 */
@Indexed
public class Node implements Map<String, Object>, Serializable, Cloneable{
    public static final String ID = "id";
    public static final String TYPEID = "typeId";
    public static final String USERID = "userId";
    public static final String ANONYMOUS = "anonymous";
    public static final String SYSTEM = "system";
    public static final String TYPE_SEPERATOR = "::";
    public static final String ID_SEPERATOR = ">";

    public static List<String> NODE_VALUE_KEYS = Arrays.asList(new String[] {"id", "typeId", "owner", "modifier", "created", "changed", "status"}) ;

    @DocumentId
    @Field
    @Analyzer(impl = CodeAnalyzer.class)
    private String id ;

    @Field(analyze = Analyze.NO)
    private String facet ;

    @Field
    @Analyzer(impl = CodeAnalyzer.class)
    private String typeId;

    @Field
    @Analyzer(impl = CodeAnalyzer.class)
    private String owner ;

    @Field
    @Analyzer(impl = CodeAnalyzer.class)
    private String modifier ;


    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.SECOND)
    private Date created ;

    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.SECOND)
    private Date changed ;


    @Field(analyze = Analyze.NO)
    @FieldBridge(impl = PropertiesFieldBridge.class)
    private Properties<String, Object> properties ;


    public Node(){
        properties = new Properties() ;
    }

    public Node(String id, String typeId){
        this(id, typeId, ANONYMOUS) ;
    }

    public Node(String id, String typeId, String userId){
        userId = StringUtils.isEmpty(userId) ? ANONYMOUS : userId ;

        this.id = id ;
        this.typeId = typeId ;
        this.created = new Date() ;
        this.changed = created ;
        this.owner = userId ;
        this.modifier = userId ;

        properties = new Properties() ;
        this.properties.setId(id) ;
        this.properties.setTypeId(typeId) ;
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
        this.typeId = typeId ;
        this.owner = userId ;
        this.modifier = userId ;
        this.created = new Date() ;
        this.changed = created ;

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
                    this.id = (String) id + _id + (i < (idablePts.size() - 1) ? ID_SEPERATOR : "");
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
    }

    private boolean isNullId() {
        return this.id == null || StringUtils.isEmpty(this.id.toString());
    }

    @Override
    public boolean containsKey(Object key) {
        return properties.containsKey(key) || NODE_VALUE_KEYS.contains(key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        return properties.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        Object value = properties.get(key)  ;
        if(value == null && NODE_VALUE_KEYS.contains(key.toString())){
            switch (key.toString()){
                case "id":
                    return id ;
                case "typeId":
                    return getTypeId() ;
                case "owner" :
                    return owner ;
                case "modifier" :
                    return modifier ;
                case "created":
                    return created ;
                case "changed":
                    return changed ;
                default:
                    return null ;
            }
        }
        return value ;
    }

    @Override
    public Object put(String key, Object value) {
        if(NODE_VALUE_KEYS.contains(key)){
            switch (key){
                case "id":
                    this.id = value.toString();
                    return id ;
                case "typeId":
                    setTypeId(value.toString());
                    return getTypeId() ;
                case "owner" :
                    if (value instanceof Code) {
                        owner = ((Code) value).getValue().toString();
                    } else {
                        owner = (String) value;
                    }
                    return owner ;
                case "modifier" :
                    if (value instanceof Code) {
                        modifier = ((Code) value).getValue().toString();
                    } else {
                        modifier = (String) value;
                    }
                    return modifier ;
                case "created":
                    created = NodeUtils.getDateValue(value) ;
                    return created ;
                case "changed":
                    changed = NodeUtils.getDateValue(value) ;
                    return changed ;
                default:
                    return null ;
            }
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
        if(getTypeId() != null){
            putAll((Map<String, Object>) m, getTypeId()) ;
        }else {
            properties.putAll(m);
        }
    }

    public void putAll(Map<String, Object> m, String typeId) {
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
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId ;
    }

    @Override
    public String toString(){
        return this.properties.toString() ;
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

    public Integer getIntValue(String pid) {
        Object value = get(pid) ;
        if(value == null) return 0 ;
        return NodeUtils.getIntValue(value) ;
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
        return  get(pid)  ;
    }

    public String getLabel(NodeType nodeType) {
        for(PropertyType pt : nodeType.getPropertyTypes()){
            if(pt.isLabelable()){
                return getStringValue(pt.getPid()) ;
            }
        }
        return getId().toString() ;
    }

    public String getLabel(ReadContext context) {
        NodeType nodeType = NodeUtils.getNodeType(getTypeId()) ;
        if(context == null) return getLabel(nodeType) ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
            if(pt.isLabelable()){
                if(pt.isI18n() && context.hasLocale()){
                    return getStringValue(pt.getPid(), context.getLocale()) ;
                }
                return getStringValue(pt.getPid()) ;
            }
        }
        return getId().toString() ;
    }

    private String getStringValue(String pid, String locale) {
        Object value = get(pid) ;
        if(value == null) return "" ;
        if(value instanceof String){
            return (String) value;
        }else if(value instanceof Map){
            if(StringUtils.isNotEmpty(locale) && ((Map) value).containsKey(locale)){
                return ((Map) value).get(locale).toString();
            }else{
                return (String) ((Map) value).get(NodeUtils.getNodeService().getDefaultLocale());
            }
        }
        return value.toString() ;
    }

    public Date getChanged(){
        return changed ;
    }

    public Node clone(){
        Node cloneNode = new Node() ;
        cloneNode.properties = properties.clone() ;

        cloneNode.id = id;
        cloneNode.typeId = typeId ;
        cloneNode.owner = owner ;
        cloneNode.modifier = modifier ;
        cloneNode.created = created ;
        cloneNode.changed = changed ;
        cloneNode.properties.setId(id) ;
        cloneNode.properties.setTypeId(this.typeId);

        return cloneNode ;
    }

    public Node clone(String typeId){
        Node cloneNode = new Node() ;
        cloneNode.properties = properties.clone() ;

        cloneNode.id = getId();
        cloneNode.typeId = typeId ;
        cloneNode.owner = owner ;
        cloneNode.modifier = modifier ;
        cloneNode.created = created ;
        cloneNode.changed = changed ;

        cloneNode.properties.setId(id) ;
        cloneNode.properties.setTypeId(typeId) ;
        return cloneNode ;
    }
    public void setUpdate(String userId, Date changed) {
        this.modifier = StringUtils.isEmpty(userId) ? ANONYMOUS : userId ;
        this.changed = changed ;
    }

    public Node toDisplay() {
        NodeType nodeType = NodeUtils.getNodeType(getTypeId()) ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
            Object value = get(pt.getPid()) ;
//            if(value == null && pt.hasDefaultValue()){
//                value = pt.getDefaultValue() ;
//            }
            if(value != null || pt.isReferenced()){
                value = NodeUtils.getDisplayValue(value, pt) ;
                if(value != null) {
                    put(pt.getPid(), value);
                }
            }else{
                put(pt.getPid(), null) ;
            }
        }
        return this ;
    }

    public Node toDisplay(ReadContext context) {
        if(context == null){
            return toDisplay() ;
        }
        NodeType nodeType = NodeUtils.getNodeType(getTypeId()) ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
            put(pt.getPid(), NodeUtils.getResultValue(context, pt, this)) ;
        }
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

            if(value == null || (value instanceof String && value.equals("_null_"))){
                remove(pt.getPid()) ;
            }else{
                put(pt.getPid(), value);
            }
        }
        return this ;
    }

    public Object getStoreValue(String pid) {
        return NodeUtils.getStoreValue(getValue(pid), NodeUtils.getNodeType(getTypeId()).getPropertyType(pid), getId()) ;
    }

    public Object getBindingValue(String pid) {
        return NodeUtils.getBindingValue(getValue(pid), NodeUtils.getNodeType(getTypeId()).getPropertyType(pid), getId()) ;
    }

    public Node getReferenceNode(String pid) {
        if(get(pid) == null) return null ;

        NodeType nodeType = NodeUtils.getNodeType(getTypeId()) ;
        PropertyType pt = nodeType.getPropertyType(pid) ;
        String referenceType = pt.getReferenceType() ;
        String refId = get(pid).toString() ;

        if(StringUtils.contains(refId, "::")){
            referenceType = StringUtils.substringBefore(refId, "::") ;
            refId = StringUtils.substringAfter(refId, "::") ;
        }

        return NodeUtils.getNode(referenceType, refId);
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
