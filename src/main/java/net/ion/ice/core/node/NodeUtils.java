package net.ion.ice.core.node;

import com.hazelcast.core.IAtomicLong;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.cluster.ClusterService;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.query.QueryContext;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.lucene.search.SortField;
import org.thymeleaf.util.StringUtils;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by jaehocho on 2017. 5. 17..
 */
public class NodeUtils {

    static NodeService nodeService ;
    public static NodeService getNodeService(){
        if(nodeService == null) {
            nodeService =  ApplicationContextManager.getBean(NodeService.class);
        }
        return nodeService ;
    }

    public static NodeType getNodeType(String typeId){
        if(getNodeService() == null) return null ;
        return nodeService.getNodeType(typeId) ;
    }


    public static List<Node> makeNodeList(Collection<Map<String, Object>> nodeDataList, String typeId) {
        List<Node> nodeList = new ArrayList<Node>();
        nodeDataList.forEach(data -> nodeList.add(new Node(data, typeId)));
        return nodeList ;
    }

    //노드 타입별 비교 및 노드 별 비교 로직 추가 필요
    public static List<Map<String, Object>> makeDataListFilterBy(Collection<Map<String, Object>> nodeDataList, String lastChanged) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        for(Map<String, Object> data : nodeDataList){
            if(data.containsKey("changed")){
                if(lastChanged.compareTo((String) data.get("changed")) < 0){
                    dataList.add(data) ;
                }
            }else{
                dataList.add(data) ;
            }
        }
        return dataList ;
    }

    public static List<NodeType> makeNodeTypeList(Collection<Node> nodeList) {
        List<NodeType> nodeTypeList = new ArrayList<NodeType>();
        for(Node node : nodeList){
            nodeTypeList.add(new NodeType(node)) ;
        }
        return nodeTypeList ;
    }


    public static List<PropertyType> makePropertyTypeList(Collection<Node> nodeList) {
        List<PropertyType> propertyTypeList = new ArrayList<PropertyType>();
        for(Node node : nodeList){
            propertyTypeList.add(new PropertyType(node)) ;
        }
        return propertyTypeList ;
    }

    public static Long getLongValue(Object value){
        if(value == null) return null ;

        if(value instanceof Long){
            return (Long) value;
        }else if(StringUtils.isEmpty(value.toString())){
            return 0L ;
        }else{
            return Long.valueOf(value.toString()) ;
        }
    }

    public static Integer getIntValue(Object value){
        if(value == null) return null ;

        if(value instanceof Integer){
            return (Integer) value;
        }else if(StringUtils.isEmpty(value.toString())){
            return 0 ;
        }else{
            return Integer.valueOf(value.toString()) ;
        }
    }

    public static Double getDoubleValue(Object value){
        if(value == null) return null ;

        if(value instanceof Double){
            return (Double) value;
        }else if(StringUtils.isEmpty(value.toString())){
            return 0D ;
        }else{
            return Double.valueOf(value.toString()) ;
        }
    }

    public static Date getDateValue(Object value) {
        if(value == null) return null ;

        if(value instanceof Date){
            return (Date) value;
        }else{
            try {
                return DateUtils.parseDate(value.toString(),"yyyyMMddHHmmss", "yyyyMMddHHmmssSSS");
            } catch (ParseException e) {
                return null ;
            }
        }
    }

    public static String getDateStringValue(Object value) {
        if(value == null) return null ;

        if(value instanceof Date){
            return DateFormatUtils.format((Date) value, "yyyyMMddHHmmss");
        }else{
            return value.toString() ;
        }
    }

    public static Object getDisplayValue(Object value, PropertyType pt) {
        switch (pt.getValueType()){
            case CODE : {
                return pt.getCode().get(value) ;
            }
            case REFERENCE: {
                return NodeUtils.getReferenceValue(value, pt) ;
            }
            case DATE :{
                return getDateStringValue(value) ;
            }
            default:
                return null ;
        }
    }

    public static Code getReferenceValue(Object value, PropertyType pt) {
        try {
            NodeService nodeService = getNodeService() ;
            Node refNode = nodeService.read(pt.getReferenceType(), value.toString());
            NodeType nodeType = nodeService.getNodeType(pt.getReferenceType());
            return new Code(refNode, nodeType);
        }catch(NotFoundNodeException e){
            return new Code(value, value.toString()) ;
        }
    }

    public static Object getResultValue(Object value, PropertyType pt, Node node) {
        switch (pt.getValueType()){
            case CODE : {
                return pt.getCode().get(value) ;
            }
            case REFERENCE: {
                return NodeUtils.getReferenceValue(value, pt) ;
            }
            case DATE :{
                return getDateStringValue(value) ;
            }
            case REFERENCED:{
                QueryContext subQueryContext = QueryContext.makeQueryContextForReferenced(getNodeType(node.getTypeId()), pt, node) ;
                return getNodeService().getNodeList(pt.getReferenceType(), subQueryContext) ;
            }
            default:
                return value ;
        }
    }

    public static Object getStoreValue(Object value, PropertyType pt) {
        if(value == null) return null ;
        if(value instanceof Code) {
            return ((Code) value).getValue() ;
        }
        switch (pt.getValueType()){
            case DATE :{
                if(value instanceof String) {
                    return getDateValue(value);
                }else if(value instanceof Date){
                    return value ;
                }
            }
            case STRING:case TEXT:{
                if(value instanceof String){
                    return value ;
                }else{
                    return value.toString() ;
                }
            }
            case LONG:{
                if(value instanceof Long){
                    return value ;
                }else{
                    return Long.valueOf(value.toString()) ;
                }
            }
            case INT:{
                if(value instanceof Integer){
                    return value ;
                }else{
                    return Integer.valueOf(value.toString()) ;
                }
            }
            case DOUBLE:{
                if(value instanceof Double){
                    return value ;
                }else{
                    return Double.valueOf(value.toString()) ;
                }
            }
            case BOOLEAN:{
                if(value instanceof Boolean){
                    return value ;
                }else{
                    return BooleanUtils.toBoolean(value.toString()) ;
                }
            }
            default:
                return value ;
        }
    }

    static ClusterService clusterService ;

    static ConcurrentMap<String, IAtomicLong> sequenceHolder = new ConcurrentHashMap<>() ;

    public static Long getSequenceValue(String typeId) {
        if(!sequenceHolder.containsKey(typeId)) {
            Long max = (Long) getNodeService().getSortedValue(typeId, "id", SortField.Type.LONG, true);
            IAtomicLong sequence = getClusterService().getSequence(typeId);
            Long current = sequence.get() ;
            if(max == null || max == 0){
                sequence.set(100);
            }else if (max > current) {
                sequence.set(max + 10) ;
            }
            sequenceHolder.put(typeId, sequence) ;
        }
        IAtomicLong sequence = sequenceHolder.get(typeId) ;
        return sequence.incrementAndGet() ;
    }

    public static ClusterService getClusterService(){
        if(clusterService == null ) {
            clusterService = ApplicationContextManager.getBean(ClusterService.class);
        }
        return clusterService ;
    }
}
