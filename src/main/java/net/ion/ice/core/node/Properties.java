package net.ion.ice.core.node;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Created by jaeho on 2017. 5. 31..
 */
public class Properties implements Map<String, Object>, Serializable, Cloneable {
    private Map<String, Object> values ;
    private transient String id;
    private transient String typeId ;

    public Properties(){
        this.values  = new LinkedHashMap<>() ;
    }


    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return values.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return values.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return values.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        values.putAll(m);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public Collection<Object> values() {
        return values.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getTypeId() {
        return typeId;
    }

    public void putAll(Map<? extends String, ?> m, NodeType nodeType) {
        for(PropertyType pt : nodeType.getPropertyTypes()){
            Object value = m.get(pt.getPid()) ;
            if(value == null && pt.hasDefaultValue()){
                value = pt.getDefaultValue() ;
            }
            if(value != null){
                values.put(pt.getPid(), value) ;
            }
            if(pt.isI18n()){
                String i18nPrefix = pt.getPid() + "_" ;
                for(String fieldName : m.keySet()){
                    if(fieldName.startsWith(i18nPrefix)){
                        values.put(fieldName, m.get(fieldName)) ;
                    }
                }
            }
        }
    }

    public void toDisplay(NodeValue nodeValue) {
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
//            if(pt.isI18n()){
//                String i18nPrefix = pt.getPid() + "_" ;
//                Map<String, Object> i18nData = new HashMap<>() ;
//
//                for(String fieldName : values.keySet()){
//                    if(fieldName.startsWith(i18nPrefix)){
//                        i18nData.put(StringUtils.substringAfter(fieldName, i18nPrefix), values.get(fieldName)) ;
////                        values.remove(fieldName) ;
//                    }
//                }
//                values.put(pt.getPid(), i18nData) ;
//                continue;
//            }

            Object value = values.get(pt.getPid()) ;
            if(value == null && pt.hasDefaultValue()){
                value = pt.getDefaultValue() ;
            }
            if(value == null && nodeValue.containsKey(pt.getPid())){
                value = nodeValue.getValue(pt.getPid()) ;
            }
            if(value != null){
                value = NodeUtils.getDisplayValue(value, pt) ;
                if(value != null) {
                    values.put(pt.getPid(), value);
                }
            }else{
                values.put(pt.getPid(), null) ;
            }
        }
    }

    public void toStore() {
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
            Object value = values.get(pt.getPid()) ;

            if(pt.isI18n()){
                String i18nPrefix = pt.getPid() + "_" ;
                Map<String, Object> i18nData = new HashMap<>() ;
                value = NodeUtils.getStoreValue(value, pt, this.id) ;
                if(value instanceof String){
                    i18nData.put("en", value) ;
                }else if(value instanceof Map){
                    i18nData = (Map<String, Object>) value;
                }

                List<String> removePids = new ArrayList<>();
                for(String fieldName : values.keySet()){
                    if(fieldName.startsWith(i18nPrefix)){
                        i18nData.put(StringUtils.substringAfter(fieldName, i18nPrefix), values.get(fieldName)) ;
                        removePids.add(fieldName) ;
                    }
                }
                for(String fieldName : removePids) {
                    values.remove(fieldName);
                }
                values.put(pt.getPid(), i18nData) ;
                continue;
            }
            if(value != null){
                value = NodeUtils.getStoreValue(value, pt, this.id) ;
                if(value != null) {
                    values.put(pt.getPid(), value);
                }else{
                    values.remove(pt.getPid()) ;
                }
            }else{
                values.remove(pt.getPid()) ;
            }
        }
    }

    public void toCode() {
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;
        List<String> labelablePids = nodeType.getLabelablePIds();
        String value = this.id;
        Object label = labelablePids.isEmpty() ? "" : values.get(labelablePids.get(0));
        clear();
        values.put("value", value);
        values.put("label", label);
    }

    public Properties clone(){
        Properties cloneProperties =  new Properties() ;
        cloneProperties.id = id ;
        cloneProperties.typeId = typeId ;
        cloneProperties.values.putAll(values);
        return cloneProperties ;
    }


    public String toString(){
        return this.values.toString() ;
    }
}
