package net.ion.ice.core.node;

import com.hazelcast.core.IAtomicLong;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.cluster.ClusterService;
import net.ion.ice.core.context.ReadContext;
import net.ion.ice.core.file.FileService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.context.QueryContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.lucene.search.SortField;
import org.infinispan.Cache;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by jaehocho on 2017. 5. 17..
 */
public class NodeUtils {

    static NodeService nodeService;

    public static NodeService getNodeService() {
        if (nodeService == null) {
            nodeService = ApplicationContextManager.getBean(NodeService.class);
        }
        return nodeService;
    }

    public static NodeType getNodeType(String typeId) {
        if (getNodeService() == null) return null;
        return nodeService.getNodeType(typeId);
    }


    public static Node getNode(String typeId, String id) {
        if (getNodeService() == null) return null;
        return nodeService.getNode(typeId, id);
    }
    static InfinispanRepositoryService infinispanService;

    public static InfinispanRepositoryService getInfinispanService() {
        if (infinispanService == null) {
            infinispanService = ApplicationContextManager.getBean(InfinispanRepositoryService.class);
        }
        return infinispanService;
    }


    public static List<Node> makeNodeList(Collection<Map<String, Object>> nodeDataList, String typeId) {
        List<Node> nodeList = new ArrayList<Node>();
        nodeDataList.forEach(data -> nodeList.add(new Node(data)));
        return nodeList;
    }

    public static List<Node> getNodeList(String typeId, String searchText) {
        return getNodeService().getNodeList(typeId, searchText);
    }

    //노드 타입별 비교 및 노드 별 비교 로직 추가 필요
    public static List<Map<String, Object>> makeDataListFilterBy(Collection<Map<String, Object>> nodeDataList, String lastChanged) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Map<String, Object> data : nodeDataList) {
            if (data.containsKey("changed")) {
                if (lastChanged.compareTo((String) data.get("changed")) < 0) {
                    dataList.add(data);
                }
            } else {
                dataList.add(data);
            }
        }
        return dataList;
    }

    public static List<NodeType> makeNodeTypeList(Collection<Node> nodeList) {
        List<NodeType> nodeTypeList = new ArrayList<NodeType>();
        for (Node node : nodeList) {
            nodeTypeList.add(new NodeType(node));
        }
        return nodeTypeList;
    }


    public static List<PropertyType> makePropertyTypeList(Collection<Node> nodeList) {
        List<PropertyType> propertyTypeList = new ArrayList<PropertyType>();
        for (Node node : nodeList) {
            propertyTypeList.add(new PropertyType(node));
        }
        return propertyTypeList;
    }

    public static Long getLongValue(Object value) {
        if (value == null) return null;

        if (value instanceof Long) {
            return (Long) value;
        } else if (StringUtils.isEmpty(value.toString())) {
            return 0L;
        } else {
            return Long.valueOf(value.toString());
        }
    }

    public static Integer getIntValue(Object value) {
        if (value == null) return null;

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (StringUtils.isEmpty(value.toString())) {
            return 0;
        } else {
            return Integer.valueOf(value.toString());
        }
    }

    public static Double getDoubleValue(Object value) {
        if (value == null) return null;

        if (value instanceof Double) {
            return (Double) value;
        } else if (StringUtils.isEmpty(value.toString())) {
            return 0D;
        } else {
            return Double.valueOf(value.toString());
        }
    }

    public static Date getDateValue(Object value) {
        if (value == null) return null;

        if (value instanceof Date) {
            return (Date) value;
        } else {
            try {
                return DateUtils.parseDate(value.toString(), "yyyyMMddHHmmss", "yyyyMMdd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd");
            } catch (ParseException e) {
                return null;
            }
        }
    }

    public static String getDateStringValue(Object value) {
        if (value == null) return null;

        if (value instanceof Date) {
            return DateFormatUtils.format((Date) value, "yyyyMMddHHmmss");
        } else {
            return value.toString();
        }
    }

    public static Object getDisplayValue(Object value, PropertyType pt) {
        switch (pt.getValueType()) {
            case CODE: {
                return pt.getCode().get(value);
            }
            case REFERENCE: {
                if(pt.isReferenceView()){
                    if (value instanceof ReferenceView) {
                        return value;
                    }
                    return NodeUtils.getReferenceValueView(value, pt);
                }else {
                    if (value instanceof Reference) {
                        return value;
                    }
                    return NodeUtils.getReferenceValue(value, pt);
                }
            }
            case REFERENCES: {
                if (value instanceof List) {
                    return value;
                } else {
                    List<Reference> refValues = new ArrayList<>();
                    if (value != null && StringUtils.isNotEmpty(value.toString())) {
                        for (String refVal : StringUtils.split(value.toString(), ",")) {
                            if(pt.isReferenceView()) {
                                refValues.add(NodeUtils.getReferenceValueView(refVal, pt));
                            }else{
                                refValues.add(NodeUtils.getReferenceValue(refVal, pt));
                            }
                        }
                    }
                    return refValues;
                }
            }
            case DATE: {
                return getDateStringValue(value);
            }
            case FILE: {
                if (value instanceof FileValue) {
                    return value;
                }
                return null;
            }
            default:
                return null;
        }
    }

    public static ReferenceView getReferenceValueView(Object value, PropertyType pt) {
        try {
            NodeService nodeService = getNodeService();
            Node refNode = nodeService.getNode(pt.getReferenceType(), value.toString());
            NodeType nodeType = nodeService.getNodeType(pt.getReferenceType());
            return new ReferenceView(refNode, nodeType);
        } catch (NotFoundNodeException e) {
            return new ReferenceView(value.toString(), value.toString());
        }
    }

    public static Reference getReferenceValue(Object value, PropertyType pt) {
        try {
            NodeService nodeService = getNodeService();
            Node refNode = nodeService.read(pt.getReferenceType(), value.toString());
            NodeType nodeType = nodeService.getNodeType(pt.getReferenceType());
            return new Reference(refNode, nodeType);
        } catch (NotFoundNodeException e) {
            return new Reference(value.toString(), value.toString());
        }
    }

    public static Object getResultValue(ReadContext context, PropertyType pt, Node node) {
        Object value = node.get(pt.getPid()) ;
        switch (pt.getValueType()){
            case CODE : {
                if(value ==  null) return null ;
                if(value instanceof Code) {
                    return value ;
                }
                return pt.getCode().get(value) ;
            }
            case REFERENCE: {
                if(value ==  null) return null ;
                if(context.isReferenceView(pt.getPid())){
                    if (value instanceof ReferenceView) {
                        return value;
                    }
                    return NodeUtils.getReferenceValueView(value, pt);
                }else {
                    if (value instanceof Reference) {
                        return value;
                    }
                    return NodeUtils.getReferenceValue(value, pt);
                }
            }
            case REFERENCES: {
                if(value ==  null) return null ;
                if(value instanceof List) {
                    return value ;
                }
                List<Reference> refValues = new ArrayList<>() ;
                if(value != null && StringUtils.isNotEmpty(value.toString())){
                    for(String refVal : StringUtils.split(value.toString(), ",")){
                        if(context.isReferenceView(pt.getPid())) {
                            refValues.add(NodeUtils.getReferenceValueView(refVal, pt));
                        }else{
                            refValues.add(NodeUtils.getReferenceValue(refVal, pt));
                        }
                    }
                }
                return refValues;
            }
            case DATE :{
                if(value ==  null) return null ;
                return getDateStringValue(value) ;
            }
            case REFERENCED: {
                QueryContext subQueryContext = QueryContext.makeQueryContextForReferenced(getNodeType(node.getTypeId()), pt, node);
                return getNodeService().getNodeList(pt.getReferenceType(), subQueryContext);
            }
            default:
                return value;
        }
    }



    public static Object getStoreValue(Object value, PropertyType pt, String id) {
        if (value == null || StringUtils.equals(StringUtils.trim(value.toString()), "null") || StringUtils.isEmpty(StringUtils.trim(value.toString())))
            return null;
        if (value instanceof Code) {
            return ((Code) value).getValue();
        }
        switch (pt.getValueType()) {
            case DATE: {
                if (value instanceof String) {
                    return getDateValue(value);
                } else if (value instanceof Date) {
                    return value;
                }
            }
            case STRING:
            case TEXT: {
                if (value instanceof String) {
                    return value;
                } else if (pt.isI18n() && value instanceof Map) {
                    return value;
                } else {
                    return value.toString();
                }
            }
            case LONG: {
                if (value instanceof Long) {
                    return value;
                } else {
                    return Long.valueOf(value.toString());
                }
            }
            case INT: {
                if (value instanceof Integer) {
                    return value;
                } else {
                    return Integer.valueOf(value.toString());
                }
            }
            case DOUBLE: {
                if (value instanceof Double) {
                    return value;
                } else {
                    return Double.valueOf(value.toString());
                }
            }
            case BOOLEAN: {
                if (value instanceof Boolean) {
                    return value;
                } else {
                    return BooleanUtils.toBoolean(value.toString());
                }
            }
            case CODE: {
                if (value instanceof Code) {
                    return ((Code) value).getValue();
                } else if (value instanceof Map) {
                    return ((Map) value).get("value");
                } else {
                    return value;
                }
            }
            case REFERENCE: {
                if (value instanceof Reference) {
                    return ((Reference) value).getRefId();
                } else if (value instanceof Map) {
                    if(((Map) value).containsKey("refId")){
                        return ((Map) value).get("refId");
                    }else {
                        return ((Map) value).get("value");
                    }
                } else {
                    return value;
                }
            }
            case REFERENCES: {
                if (value instanceof List) {
                    String refsValues = "";
                    for (Object val : (List) value) {
                        if (val instanceof Reference) {
                            refsValues += ((Reference) val).getRefId() + ",";
                        } else if (value instanceof Map) {
                            if(((Map) value).containsKey("refId")){
                                refsValues += ((Map) val).get("refId") + ",";
                            }else {
                                refsValues += ((Map) val).get("value") + ",";
                            }
                        } else {
                            refsValues += val.toString().trim() + ",";
                        }
                    }
                    if (refsValues.endsWith(",")) return StringUtils.substringBeforeLast(refsValues, ",");
                    else return refsValues;
                } else {
                    String refsValues = "";
                    for (Object val : StringUtils.split(value.toString(), ",")) {
                        refsValues += val.toString().trim() + ",";
                    }
                    if (refsValues.endsWith(",")) return StringUtils.substringBeforeLast(refsValues, ",");
                    else return refsValues;
                }
            }
            case FILE: {
                if (value instanceof FileValue) {
                    return value;
                } else if (value instanceof MultipartFile) {
                    FileValue fileValue = getFileService().saveMultipartFile(pt, id, (MultipartFile) value);
                    return fileValue;
                } else if (value instanceof String) {
                    return null;
                }
            }
            case OBJECT: {
                if (value instanceof Map) {
                    return value;
                }
                if (value instanceof String) {
                    try {
                        if (JsonUtils.isList((String) value)) {
                            return JsonUtils.parsingJsonToList((String) value);
                        } else {
                            return JsonUtils.parsingJsonToMap((String) value);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return value;
                    }
                }
            }
            default:
                return value;
        }
    }


    public static Object getBindingValue(Object value, PropertyType pt, String id) {
        if (value == null || "".equals(value.toString().trim())) return null;
        if (value instanceof Code) {
            return ((Code) value).getValue();
        }
        if(value instanceof Reference){
            return ((Reference) value).getValue() ;
        }
        if (pt.isI18n() && value instanceof Map) {
            return ((Map) value).get("en");
        }
        switch (pt.getValueType()) {
            case FILE: {
                if (value instanceof FileValue) {
                    return ((FileValue) value).getStorePath();
                } else if (value instanceof MultipartFile) {
                    FileValue fileValue = getFileService().saveMultipartFile(pt, id, (MultipartFile) value);
                    return fileValue.getStorePath();
                } else if (value instanceof String) {
                    return value;
                }
            }
            case OBJECT: {
                if (value instanceof Map) {
                    return JsonUtils.toJsonString((Map<String, Object>) value);
                }
                if (value instanceof String) {
                    return value;
                }
            }
            case DATE: {
                return getDateStringValue(value);
            }
            default:
                return getStoreValue(value, pt, id);
        }
    }

    static FileService fileService;

    public static FileService getFileService() {
        if (fileService == null) {
            fileService = ApplicationContextManager.getBean(FileService.class);
        }
        return fileService;
    }


    static ClusterService clusterService;

    static ConcurrentMap<String, IAtomicLong> sequenceHolder = new ConcurrentHashMap<>();

    public static Long getSequenceValue(String typeId) {
        if (!sequenceHolder.containsKey(typeId)) {
            List<PropertyType> idablePts = NodeUtils.getNodeType(typeId).getIdablePropertyTypes();
            Long max = null;

            if (idablePts.size() == 0 || idablePts.isEmpty()) {
                throw new RuntimeException("ID is NULL");
            }

            String id = idablePts.get(0).getPid();
            switch (idablePts.get(0).getValueType()){
                case INT:
                    max = Long.parseLong(String.valueOf(getNodeService().getSortedValue(typeId, id, SortField.Type.INT, true)));
                    break;
                case LONG:
                    max = (Long) getNodeService().getSortedValue(typeId, id, SortField.Type.LONG, true);
                    break;
            }
            IAtomicLong sequence = getClusterService().getSequence(typeId);
            Long current = sequence.get();
            if (max == null || max == 0) {
                sequence.set(100);
            } else if (max > current) {
                sequence.set(max + 10);
            }
            sequenceHolder.put(typeId, sequence);
        }
        IAtomicLong sequence = sequenceHolder.get(typeId);
        return sequence.incrementAndGet();
    }

    public static ClusterService getClusterService() {
        if (clusterService == null) {
            clusterService = ApplicationContextManager.getBean(ClusterService.class);
        }
        return clusterService;
    }


    public static List<Node> initNodeList(String typeId, List<Object> list) {
        Cache<String, NodeValue> nodeValueCache = getInfinispanService().getNodeValueCache();

        List<Node> nodeList = new ArrayList<>();

        for (Object item : list) {
            Node srcNode = (Node) item;
            if (srcNode.getNodeValue() == null) {
                srcNode.setNodeValue(nodeValueCache.get(typeId + NodeValue.NODEVALUE_SEPERATOR + srcNode.getId()));
            }
            nodeList.add(srcNode.clone().toDisplay());
        }

        return nodeList;
    }

    public static Object getStoreValue(Map<String, Object> data, PropertyType pt, String id) {
        if (data == null) return null;
        Object value = data.get(pt.getPid());

        if (pt.isI18n()) {
            String i18nPrefix = pt.getPid() + "_";
            Map<String, Object> i18nData = new HashMap<>();
            value = NodeUtils.getStoreValue(value, pt, id);
            if (value instanceof String) {
                i18nData.put("en", value);
            } else if (value instanceof Map) {
                i18nData = (Map<String, Object>) value;
            }

            List<String> removePids = new ArrayList<>();
            for (String fieldName : data.keySet()) {
                if (fieldName.startsWith(i18nPrefix)) {
                    i18nData.put(org.apache.commons.lang.StringUtils.substringAfter(fieldName, i18nPrefix), data.get(fieldName));
                    removePids.add(fieldName);
                }
            }
            for (String fieldName : removePids) {
                data.remove(fieldName);
            }
            return i18nData;
        }
        return NodeUtils.getStoreValue(value, pt, id);
    }
}
