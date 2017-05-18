package net.ion.ice.core.node;

import org.hibernate.search.annotations.*;

import javax.persistence.Id;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 3. 31..
 */

@ProvidedId
@Indexed
public class Node implements Map<String, Object>, Serializable{
    public static final String ID = "id";
    public static final String TID = "tid";
    public static final String ANONYMOUS = "anonymous";

    @Id
    private String id ;

    @Field
    @FieldBridge(impl = PropertiesFieldBridge.class)
    private Map<String, Object> properties ;

    private NodeValue nodeValue ;

    public Node(){
        properties = new ConcurrentHashMap<>() ;
    }

    public Node(String id, String tid){
        this(id, tid, ANONYMOUS) ;
    }

    public Node(String id, String tid, String userId){
        this.id = id ;
        properties = new ConcurrentHashMap<>() ;
        this.properties.put(ID, id) ;
        this.properties.put(TID, tid) ;
        this.nodeValue = new NodeValue(id, tid, userId) ;
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
        properties.putAll(m);
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

    public String getTid() {
        return nodeValue.getTid();
    }

    public void setTid(String tid) {
        this.properties.put(TID, tid) ;
        nodeValue.setTid(tid) ;
    }

    @Override
    public String toString(){
        return this.properties.toString() ;
    }

    public NodeValue getNodeValue() {
        return nodeValue;
    }

    public void setNodeValue(NodeValue nodeValue) {
        this.nodeValue = nodeValue;
    }
}
