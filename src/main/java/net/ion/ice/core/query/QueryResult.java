package net.ion.ice.core.query;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by jaeho on 2017. 6. 15..
 */
public class QueryResult implements Map<String, Object> {
    private Map<String, Object> result ;

    public QueryResult(){
        this.result = new LinkedHashMap<>() ;
    }

    @Override
    public int size() {
        return result.size();
    }

    @Override
    public boolean isEmpty() {
        return result.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return result.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return result.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return result.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return result.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return result.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        result.putAll(m);
    }

    @Override
    public void clear() {
        result.clear();
    }

    @Override
    public Set<String> keySet() {
        return result.keySet();
    }

    @Override
    public Collection<Object> values() {
        return result.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return result.entrySet();
    }
}
