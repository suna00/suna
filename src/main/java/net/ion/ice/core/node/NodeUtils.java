package net.ion.ice.core.node;

import com.hazelcast.core.IAtomicLong;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.cluster.ClusterService;
import net.ion.ice.core.context.DataQueryContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.context.ReadContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.file.FileService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.json.JsonUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.lucene.document.DateTools;
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
        return getNode(getNodeType(typeId), id);
    }

    public static Node getNode(NodeType nodeType, String id) {
        if(nodeType == null) return null ;
        if(nodeType.getRepositoryType().equals("data")){
            if (getNodeBindingService() == null) return null ;
            Map<String, Object> resultData =  getNodeBindingService().getNodeBindingInfo(nodeType.getTypeId()).retrieve(id) ;
            return new Node(resultData, nodeType.getTypeId());
        }else {
            if (getNodeService() == null) return null;
            return nodeService.getNode(nodeType.getTypeId(), id);
        }
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
        nodeDataList.forEach(data -> nodeList.add( new Node(data)));
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

    public static Long getDateLongValue(Object value){
        return DateTools.round(NodeUtils.getDateValue(value), DateTools.Resolution.MILLISECOND).getTime() ;
    }

    public static String getDateStringValue(Object value, String dateFormat) {
        if (value == null) return null;

        if(StringUtils.isEmpty(dateFormat)){
            dateFormat = "yyyyMMddHHmmss" ;
        }

        if (value instanceof Date) {
            return DateFormatUtils.format((Date) value, dateFormat);
        } else if (value instanceof String && ((String) value).length() == 14 && dateFormat.equals("yyyyMMddHHmmss")) {
            return (String) value;
        } else {
//            try {
//                return DateFormatUtils.format(DateTools.stringToDate(value.toString()), dateFormat);//DateTools에서 시간이 변환될때 value에 +9시간 되서 수정
                return DateFormatUtils.format(NodeUtils.getDateValue(value), dateFormat);
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//            return value.toString();
        }
    }

    public static Object getDisplayValue(Object value, PropertyType pt) {
        switch (pt.getValueType()) {
            case CODE: {
                return pt.getCode().get(value);
            }
            case REFERENCE: {
                if (pt.isReferenceView()) {
                    if (value instanceof ReferenceView) {
                        return value;
                    }
                    return NodeUtils.getReferenceValueView(null, value, pt);
                } else {
                    if (value instanceof Reference) {
                        return value;
                    }
                    return NodeUtils.getReferenceValue(null, value, pt);
                }
            }
            case REFERENCES: {
                if (value instanceof List) {
                    return value;
                } else {
                    List<Reference> refValues = new ArrayList<>();
                    if (value != null && StringUtils.isNotEmpty(value.toString())) {
                        for (String refVal : StringUtils.split(value.toString(), ",")) {
                            if (pt.isReferenceView()) {
                                refValues.add(NodeUtils.getReferenceValueView(null, refVal, pt));
                            } else {
                                refValues.add(NodeUtils.getReferenceValue(null, refVal, pt));
                            }
                        }
                    }
                    return refValues;
                }
            }
            case DATE: {
                return getDateStringValue(value, null);
            }
            case FILE: {
//                if (value instanceof FileValue) {
                return value;
//                }else {
//
//                }
            }

            default:
                return null;
        }
    }

    public static ReferenceView getReferenceValueView(ReadContext context, Object value, PropertyType pt) {
        try {
            Node refNode = getReferenceNode(value, pt);
            return new ReferenceView(refNode.toDisplay(context), context);

        } catch (Exception e) {
            return new ReferenceView(value.toString(), value.toString());
        }
    }

    public static Reference getReferenceValue(ReadContext context, Object value, PropertyType pt) {
        try {
            Node refNode = getReferenceNode(value, pt);
            return new Reference(refNode, context);
        } catch (Exception e) {
            return new Reference(value.toString(), value.toString());
        }
    }

    public static Node getReferenceNode(Object value, PropertyType pt) {
        if(value == null || pt == null) return null;
        String referenceType = pt.getReferenceType() ;
        String refId = value.toString() ;

        if(StringUtils.contains(refId, "::")){
            referenceType = StringUtils.substringBefore(refId, "::") ;
            refId = StringUtils.substringAfter(refId, "::") ;
        }
        if(StringUtils.isNotEmpty(pt.getCodeFilter()) && !StringUtils.contains(refId, Node.ID_SEPERATOR)){
            refId = pt.getCodeFilter() + Node.ID_SEPERATOR + refId ;
        }

        return getNode(referenceType, refId);
    }

    public static Object getResultValue(ReadContext context, PropertyType pt, Map<String, Object> node) {
        Object value = node.get(pt.getPid());
        return getResultValue(context, pt, node, value);
    }

    public static Object getResultValue(ReadContext context, PropertyType pt, Map<String, Object> node, Object value) {
        switch (pt.getValueType()) {
            case CODE: {
                if (value == null) return null;
                if (value instanceof Code) {
                    return value;
                }
                return pt.getCode().get(value);
            }
            case REFERENCE: {
                if (value == null) return null;
                if (context.isReferenceView(pt.getPid())) {
                    if (value instanceof ReferenceView) {
                        return NodeUtils.getReferenceValueView(context, ((ReferenceView) value).getRefId(), pt);
                    }
                    return NodeUtils.getReferenceValueView(context, value, pt);
                } else {
                    if (value instanceof Reference) {
                        return value;
                    }
                    return NodeUtils.getReferenceValue(context, value, pt);
                }
            }
            case REFERENCES: {
                if (value == null) return null;
                if (value instanceof List) {
                    return value;
                }
                List<Reference> refValues = new ArrayList<>();
                if (value != null && StringUtils.isNotEmpty(value.toString())) {
                    for (String refVal : StringUtils.split(value.toString(), ",")) {
                        if (context.isReferenceView(pt.getPid())) {
                            refValues.add(NodeUtils.getReferenceValueView(context, refVal, pt));
                        } else {
                            refValues.add(NodeUtils.getReferenceValue(context, refVal, pt));
                        }
                    }
                }
                return refValues;
            }
            case DATE: {
                if (value == null) return null;
                return getDateStringValue(value, context.getDateFormat());
            }
            case FILE: {
                if (value == null) return null;
                if(pt.isI18n() && context.hasLocale() && value instanceof Map){
                    if(StringUtils.isNotEmpty(context.getLocale()) && ((Map) value).containsKey(context.getLocale())){
                        return getFileResultValue(context, pt, ((Map) value).get(context.getLocale()));
                    }else{
                        return getFileResultValue(context, pt, ((Map) value).get(getNodeService().getDefaultLocale())) ;
                    }
                }
                return getFileResultValue(context, pt, value);
            }
            case REFERENCED: {
                if (context != null && context.isIncludeReferenced() && context.getLevel() < 5 && node instanceof Node) {
                    QueryContext subQueryContext = QueryContext.makeQueryContextForReferenced(getNodeType(((Node)node).getTypeId()), pt, (Node) node);
                    subQueryContext.setDateFormat(context.getDateFormat()) ;
                    subQueryContext.setFileUrlFormat(context.getFileUrlFormat()) ;
                    subQueryContext.setLevel(context.getLevel() + 1);
                    return getNodeService().getDisplayNodeList(pt.getReferenceType(), subQueryContext);
                }
                return null;
            }
            default:
                if(pt.isI18n() && context.hasLocale() && value instanceof Map){
                    if(StringUtils.isNotEmpty(context.getLocale()) &&((Map) value).containsKey(context.getLocale())){
                        return ((Map) value).get(context.getLocale()) ;
                    }else{
                        return ((Map) value).get(getNodeService().getDefaultLocale()) ;
                    }
                }
                return value;
        }
    }

    private static Object getFileResultValue(ReadContext context, PropertyType pt, Object value) {
        if(context.getFileUrlFormat() != null && context.getFileUrlFormat().containsKey(pt.getFileHandler())){
            String fileUrlFormat = (String) context.getFileUrlFormat().get(pt.getFileHandler());
            if (value instanceof FileValue) {
                return fileUrlFormat + ((FileValue) value).getStorePath();
            }
        }else {
            return value;
        }
        return null;
    }


    public static Object getStoreValue(Object value, PropertyType pt, String id) {
        if (value == null || StringUtils.equals(StringUtils.trim(value.toString()), "null") || StringUtils.isEmpty(StringUtils.trim(value.toString())))
            return null;

        switch (pt.getValueType()) {
            case DATE: {
//                return DateTools.dateToString(NodeUtils.getDateValue(value), DateTools.Resolution.SECOND);
                return DateFormatUtils.format(NodeUtils.getDateValue(value), "yyyyMMddHHmmss");
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
                    if (((Map) value).containsKey("refId")) {
                        return getRefereceStoreValue(((Map) value).get("refId"), pt);
                    } else {
                        return getRefereceStoreValue(((Map) value).get("value"), pt);
                    }
                } else {
                    return getRefereceStoreValue(value, pt);
                }
            }
            case REFERENCES: {
                if (value instanceof List) {
                    String refsValues = "";
                    for (Object val : (List) value) {
                        if (val instanceof Reference) {
                            refsValues += ((Reference) val).getRefId() + ",";
                        } else if (value instanceof Map) {
                            if (((Map) value).containsKey("refId")) {
                                refsValues += getRefereceStoreValue(((Map) val).get("refId"), pt) + ",";
                            } else {
                                refsValues += getRefereceStoreValue(((Map) val).get("value"), pt) + ",";
                            }
                        } else {
                            refsValues += getRefereceStoreValue(val.toString().trim(), pt) + ",";
                        }
                    }
                    if (refsValues.endsWith(",")) return StringUtils.substringBeforeLast(refsValues, ",");
                    else return refsValues;
                } else {
                    String refsValues = "";
                    for (Object val : StringUtils.split(value.toString(), ",")) {
                        refsValues += getRefereceStoreValue(val.toString().trim(), pt) + ",";
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
                } else if (value instanceof String && JsonUtils.isJson((String) value)) {
                    FileValue fileValue = getFileService().fileValueMapper((String) value);
                    return fileValue;
                } else if (value instanceof String) {
                    return value ;
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

    public static Object getRefereceStoreValue(Object value, PropertyType pt) {
        if (value == null) return null;
        String codeFitler = pt.getCodeFilter();
        if (StringUtils.isEmpty(codeFitler)) {
            return value;
        }

        if (StringUtils.contains(value.toString(), Node.ID_SEPERATOR)) {
            return value;
        } else {
            return codeFitler + Node.ID_SEPERATOR + value;
        }
    }


    public static Object getBindingValue(Object value, PropertyType pt, String id) {
        if (value == null || "".equals(value.toString().trim())) return null;
        if (value instanceof Code) {
            return ((Code) value).getValue();
        }
        if (value instanceof Reference) {
            if (StringUtils.isEmpty(pt.getCodeFilter())) {
                return ((Reference) value).getRefId();
            } else {
                return ((Reference) value).getValue();
            }
        }
        if (pt.isI18n() && value instanceof Map) {
            Object localeValue =  ((Map) value).get(getNodeService().getDefaultLocale());
            if(localeValue instanceof FileValue){
                return ((FileValue) localeValue).getStorePath();
            }else{
                return localeValue ;
            }
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
                if (value instanceof List) {
                    return JsonUtils.toJsonString((List<?>) value);
                }
                if (value instanceof String) {
                    return value;
                }
            }
            case DATE: {
                return getDateStringValue(value, null);
            }
            case REFERENCE: {
                if (StringUtils.isEmpty(pt.getCodeFilter())) {
                    return value;
                } else if(StringUtils.contains(value.toString(), Node.ID_SEPERATOR)){
                    return StringUtils.substringAfterLast(value.toString(), Node.ID_SEPERATOR);
                } else {
                    return value ;
                }
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
        if (NodeUtils.getNodeType(typeId).isNode()) {
            if (!sequenceHolder.containsKey(typeId)) {
                List<PropertyType> idablePts = NodeUtils.getNodeType(typeId).getIdablePropertyTypes();
                Long max = null;

                if (idablePts.size() == 0 || idablePts.isEmpty()) {
                    throw new RuntimeException("ID is NULL");
                }

                String id = idablePts.get(0).getPid();

                try {
                    max = (Long) getNodeService().getSortedValue(typeId, id, SortField.Type.LONG, true);
                } catch (NumberFormatException e) {
                    max = Long.parseLong(String.valueOf(getNodeService().getSortedValue(typeId, id, SortField.Type.INT, true)));
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
        } else {

            NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);
            Long sequence = nodeBindingInfo.retrieveSequence();

            return (Long) sequence;
        }
    }

    public static ClusterService getClusterService() {
        if (clusterService == null) {
            clusterService = ApplicationContextManager.getBean(ClusterService.class);
        }
        return clusterService;
    }


    static NodeBindingService nodeBindingService;
    public static NodeBindingService getNodeBindingService() {
        if (nodeBindingService == null) {
            nodeBindingService = ApplicationContextManager.getBean(NodeBindingService.class);
        }
        return nodeBindingService;
    }

    public static NodeBindingInfo getNodeBindingInfo(String typeId){
        return getNodeBindingService().getNodeBindingInfo(typeId) ;
    }

    public static List<Node> initNodeList(String typeId, List<Object> list) {
        List<Node> nodeList = new ArrayList<>();

        for (Object item : list) {
            Node srcNode = (Node) item;
            nodeList.add(srcNode.clone());
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
            if (value instanceof String || value instanceof FileValue) {
                i18nData.put(getDefaultLocale(), value);
                data.put(pt.getPid(), value) ;
            } else if (value instanceof Map) {
                i18nData = (Map<String, Object>) value;
            }

            List<String> removePids = new ArrayList<>();
            for (String fieldName : data.keySet()) {
                if (fieldName.startsWith(i18nPrefix)) {
                    Object val = NodeUtils.getStoreValue(data.get(fieldName), pt, id);
                    i18nData.put(StringUtils.substringAfter(fieldName, i18nPrefix), val);
                    removePids.add(fieldName);
                }
            }
            for (String fieldName : removePids) {
                data.remove(fieldName);
            }

            if(i18nData.size() > 0) {
                return i18nData;
            }else{
                return null ;
            }
        }
        Object resultValue = NodeUtils.getStoreValue(value, pt, id);
        if(resultValue instanceof FileValue){
            data.put(pt.getPid(), resultValue) ;
        }
        return resultValue ;
    }

    public static List<Node> initDataNodeList(String typeId, List<Map<String, Object>> resultList) {
        List<Node> nodeList = new ArrayList<>(resultList.size());
        for (Map<String, Object> data : resultList) {
            nodeList.add(new Node(data, typeId));
        }
        return nodeList;
    }

    public static String getDefaultLocale(){
        return getNodeService().getDefaultLocale() ;
    }
}
