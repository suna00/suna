package net.ion.ice.core.node;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by jaeho on 2017. 5. 31..
 */
public class Properties implements Map<String, Object>, Serializable {
    private Map<String, Object> values ;
    private transient Object id;
    private transient String typeId ;

    public Properties(){
        this.values  = new HashMap<>() ;
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

    public void setId(Object id) {
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
                NodeUtils.getValue(value, pt) ;
                values.put(pt.getPid(), value) ;
            }
        }
    }
}
