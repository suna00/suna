package net.ion.ice.core.node;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.thymeleaf.util.StringUtils;

import java.text.ParseException;
import java.util.*;

/**
 * Created by jaehocho on 2017. 5. 17..
 */
public class NodeUtils {

    static NodeService nodeService ;

    public static void setNodeService(NodeService nodeService) {
        NodeUtils.nodeService = nodeService ;
    }

    public static NodeType getNodeType(String typeId){
        return nodeService.getNodeType(typeId) ;
    }

    public static List<Node> makeNodeList(Collection<Map<String, Object>> nodeDataList) {
        List<Node> nodeList = new ArrayList<Node>();
        nodeDataList.forEach(data -> nodeList.add(new Node(data)));

//        for(Map<String, Object> data : nodeDataList){
//            nodeList.add(new Node(data)) ;
//        }
        return nodeList ;
    }

    public static List<Node> makeNodeList(Collection<Map<String, Object>> nodeDataList, String typeId) {
        List<Node> nodeList = new ArrayList<Node>();
        nodeDataList.forEach(data -> nodeList.add(new Node(data, typeId)));
        return nodeList ;
    }

    //노드 타입별 비교 및 노드 별 비교 로직 추가 필요
    public static List<Node> makeNodeListFilterBy(Collection<Map<String, Object>> nodeDataList, String lastChanged) {
        List<Node> nodeList = new ArrayList<Node>();
        for(Map<String, Object> data : nodeDataList){
            if(data.containsKey("changed")){
                if(lastChanged.compareTo((String) data.get("changed")) < 0){
                    nodeList.add(new Node(data)) ;
                }
            }else{
                nodeList.add(new Node(data)) ;
            }
        }
        return nodeList ;
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


}
