package net.ion.ice.core.node;

import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.file.TolerableMissingFileException;
import org.springframework.web.multipart.MultipartFile;

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

    public void putAll(Map<String, Object> m, NodeType nodeType) {
        for(PropertyType pt : nodeType.getPropertyTypes()){
            Object value = m.get(pt.getPid()) ;
            if(value == null && pt.hasDefaultValue()){
                value = pt.getDefaultValue() ;
            }
            if(value != null && !(value instanceof List)){
                if(pt.isFile() && value instanceof String && (((String) value).startsWith("classpath:") || ((String) value).startsWith("http://") || ((String) value).startsWith("/"))) {
                    FileValue fileValue = null;
                    try{
                        fileValue = NodeUtils.getFileService().saveResourceFile(pt, id, (String) value);
                    } catch (TolerableMissingFileException e) {
                        System.err.println("Fetching not found image is not an error");
                    }
                    values.put(pt.getPid(), fileValue);
                    m.put(pt.getPid(), fileValue);
                }else  if(pt.isFile() && value instanceof MultipartFile){
                    FileValue fileValue = NodeUtils.getFileService().saveMultipartFile(pt, id, (MultipartFile) value);
                    values.put(pt.getPid(), fileValue);
                    m.put(pt.getPid(), fileValue) ;
                }else {
                    values.put(pt.getPid(), value);
                }
            }
            if(pt.isI18n()){
                String i18nPrefix = pt.getPid() + "_" ;
                for(String fieldName : m.keySet()){
                    if(fieldName.startsWith(i18nPrefix)) {
                        if (pt.isFile() && m.get(fieldName) instanceof String && (((String) m.get(fieldName)).startsWith("classpath:") || ((String) m.get(fieldName)).startsWith("http://") || ((String) m.get(fieldName)).startsWith("/"))) {
                            FileValue fileValue = NodeUtils.getFileService().saveResourceFile(pt, id, (String) m.get(fieldName));
                            values.put(fieldName, fileValue);
                            m.put(fieldName, fileValue);
                        } else if(pt.isFile() && m.get(fieldName) instanceof MultipartFile){
                            FileValue fileValue = null;
                            try {
                                fileValue = NodeUtils.getFileService().saveMultipartFile(pt, id, (MultipartFile) value);
                            } catch (TolerableMissingFileException e) {
                                e.printStackTrace();
                            }
                            values.put(fieldName, fileValue);
                            m.put(fieldName, fileValue);
                        } else {
                            values.put(fieldName, m.get(fieldName));
                        }
                    }
                }
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
