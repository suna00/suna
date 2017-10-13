package net.ion.ice.core.node;

import net.ion.ice.core.file.FileValue;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.*;

/**
 * Created by jaeho on 2017. 5. 31..
 */
public class Properties<K,V> extends LinkedHashMap<K, V> implements Map<K,V>, Serializable, Cloneable {
    private transient String id;
    private transient String typeId ;


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
                if(pt.isFile() && value instanceof String && (((String) value).startsWith("classpath:") || ((String) value).startsWith("http://") || ((String) value).startsWith("https://") || ((String) value).startsWith("/"))) {
                    FileValue fileValue = NodeUtils.getFileService().saveResourceFile(pt, id, (String) value);
                    put(pt.getPid(), fileValue);
                    m.put(pt.getPid(), fileValue);
                }else  if(pt.isFile() && value instanceof MultipartFile){
                    FileValue fileValue = NodeUtils.getFileService().saveMultipartFile(pt, id, (MultipartFile) value);
                    put(pt.getPid(), fileValue);
                    m.put(pt.getPid(), fileValue) ;
                }else {
                    put(pt.getPid(), value);
                }
            }
            if(pt.isI18n()){
                String i18nPrefix = pt.getPid() + "_" ;
                for(String fieldName : m.keySet()){
                    if(fieldName.startsWith(i18nPrefix)) {
                        if (pt.isFile() && m.get(fieldName) instanceof String && (((String) m.get(fieldName)).startsWith("classpath:") || ((String) m.get(fieldName)).startsWith("http://") || ((String) m.get(fieldName)).startsWith("https://") || ((String) m.get(fieldName)).startsWith("/"))) {
                            FileValue fileValue = NodeUtils.getFileService().saveResourceFile(pt, id, (String) m.get(fieldName));
                            put(fieldName, fileValue);
                            m.put(fieldName, fileValue);
                        } else if(pt.isFile() && m.get(fieldName) instanceof MultipartFile){
                            FileValue fileValue = NodeUtils.getFileService().saveMultipartFile(pt, id, (MultipartFile) m.get(fieldName));
                            put(fieldName, fileValue);
                            m.put(fieldName, fileValue);
                        } else {
                            put(fieldName, m.get(fieldName));
                        }
                    }
                }
            }
        }
    }

    private void put(String pid, Object value) {
        put((K) pid, (V) value) ;
    }


    public void toCode() {
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;
        List<String> labelablePids = nodeType.getLabelablePIds();
        String value = this.id;
        Object label = labelablePids.isEmpty() ? "" : get(labelablePids.get(0));
        clear();
        put("value",  value);
        put("label",  label);
    }

    public Properties clone(){
        Properties cloneProperties =  new Properties() ;
        cloneProperties.id = id ;
        cloneProperties.typeId = typeId ;
        cloneProperties.putAll(this);
        return cloneProperties ;
    }

}
