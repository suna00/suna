package net.ion.ice.core.node;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 5. 31..
 */
public class ExecuteContext {
    private Map<String, Object> data  ;
    private Node node;
    private NodeType nodeType;
    private Node existNode ;

    private boolean exist ;
    private String id ;

    public static ExecuteContext makeContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        ExecuteContext ctx = new ExecuteContext();

        Map<String, Object> data = new HashMap<>();
        for (String paramName : parameterMap.keySet()) {

            String[] values = parameterMap.get(paramName);
            String value = null;

            if (values != null && values.length > 0 && StringUtils.isNotEmpty(values[0])) {
                value = values[0];
            }

            data.put(paramName, value);
        }

        ctx.setData(data);
        ctx.setNodeType(nodeType);

        ctx.init() ;

        return ctx ;
    }

    private void init() {
        existNode = NodeUtils.getNodeService().getNode(nodeType.getTypeId(), id) ;
        exist = existNode != null ;

        if(exist){
            List<String> changedProperties = new ArrayList<String>() ;
            this.node = existNode.clone() ;
            for(PropertyType pt : nodeType.getPropertyTypes()){
                if(!data.containsKey(pt.getPid())){
                    continue;
                }
                Object newValue = NodeUtils.getStoreValue(data.get(pt.getPid()), pt) ;
                Object existValue = data.get(pt.getPid()) ;

                if(newValue == null && existValue == null){
                    continue;
                }else if(newValue == null && existValue != null){
                    node.remove(pt.getPid()) ;
                    changedProperties.add(pt.getPid()) ;
                }else if(newValue != null && existValue == null){
                    node.put(pt.getPid(), newValue) ;
                    changedProperties.add(pt.getPid()) ;
                }else if(!newValue.equals(existValue)){
                    node.put(pt.getPid(), newValue) ;
                    changedProperties.add(pt.getPid()) ;
                }
            }
        }else {
            this.node = new Node(data, nodeType.getTypeId());
        }


    }

    private void makeNode() {
        this.node = new Node(data, nodeType.getTypeId()) ;
    }

    public void setData(Map<String,Object> data) {
        this.data = data;
    }

    public Node getNode() {
        return node;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getId(){
        if(this.id == null){
            for(String key : data.keySet()){
                if(key.equals("id")){
                    id = data.get(key).toString();
                }
            }

            if(id == null){
                List<String> idablePids = nodeType.getIdablePIds() ;
                for(int i = 0 ; i < idablePids.size(); i++){
                    id = data.get(idablePids.get(i)) + (i < (idablePids.size() - 1) ? "/" : "") ;
                }
            }
        }
        return id ;
    }
}
