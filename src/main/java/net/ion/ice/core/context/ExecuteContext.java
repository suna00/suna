package net.ion.ice.core.context;

import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Created by jaeho on 2017. 5. 31..
 */
public class ExecuteContext implements Context{
    private Map<String, Object> data  ;
    private Node node;
    private NodeType nodeType;
    private Node existNode ;

    private boolean exist ;
    private boolean execute ;


    private List<String> changedProperties ;
    private String id ;

    private String userId ;
    private Date time ;





    public static ExecuteContext makeContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType) {
        ExecuteContext ctx = new ExecuteContext();

        Map<String, Object> data = ContextUtils.makeContextData(parameterMap);

        ctx.setData(data);
        ctx.setNodeType(nodeType);

        ctx.init() ;

        return ctx ;
    }

    public static ExecuteContext makeContextFromParameter(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, NodeType nodeType) {
        ExecuteContext ctx = new ExecuteContext();

        Map<String, Object> data = ContextUtils.makeContextData(parameterMap, multiFileMap);

        ctx.setData(data);
        ctx.setNodeType(nodeType);

        ctx.init() ;

        return ctx ;
    }


    public static ExecuteContext makeContextFromMap(Map<String, Object> data) {
        ExecuteContext ctx = new ExecuteContext();

        ctx.setData(data);

        NodeType nodeType = NodeUtils.getNodeType(data.get(Node.TYPEID).toString()) ;
        ctx.setNodeType(nodeType);

        ctx.init() ;

        return ctx ;
    }

    public static ExecuteContext makeContextFromMap(Map<String, Object> data, String typeId) {
        ExecuteContext ctx = new ExecuteContext();

        ctx.setData(data);

        NodeType nodeType = NodeUtils.getNodeType(typeId) ;
        ctx.setNodeType(nodeType);

        ctx.init() ;

        return ctx ;
    }

    private void init() {
        this.time = new Date() ;
        existNode = NodeUtils.getNodeService().getNode(nodeType.getTypeId(), getId()) ;
        exist = existNode != null ;

        if(exist){
            changedProperties = new ArrayList<>() ;
            this.node = existNode.clone() ;
            for(PropertyType pt : nodeType.getPropertyTypes()){
                if(!data.containsKey(pt.getPid())){
                    continue;
                }
                Object newValue = NodeUtils.getStoreValue(data.get(pt.getPid()), pt, node.getId()) ;
                Object existValue = existNode.get(pt.getPid()) ;

                if(newValue == null && existValue == null) {
                    continue;
                }else if(pt.isFile()){
                    if(newValue != null && newValue instanceof FileValue){
                        node.put(pt.getPid(), newValue) ;
                        changedProperties.add(pt.getPid()) ;
                    }
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
            execute = changedProperties.size() > 0 ;
            if(execute){
                node.setUpdate(userId, time);
            }

        }else {
            this.node = new Node(data, nodeType.getTypeId());
            execute = true ;
        }


    }


    public void setData(Map<String,Object> data) {
        this.data = data;
    }

    public Node getNode() {
        return node;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType ;
    }

    public String getId(){
        if(this.id == null){
            if(data.containsKey("id")){
                id = data.get("id").toString();
            }

            if(id == null){
                List<String> idablePids = nodeType.getIdablePIds() ;
                id = "" ;
                for(int i = 0 ; i < idablePids.size(); i++){
                    id = id + data.get(idablePids.get(i)) + (i < (idablePids.size() - 1) ? Node.ID_SEPERATOR : "") ;
                }
            }
        }
        return id ;
    }

    public boolean isExecute() {
        return execute;
    }

    public boolean isSyncTable() {
        return this.nodeType != null && this.nodeType.hasTableName();
    }

}
