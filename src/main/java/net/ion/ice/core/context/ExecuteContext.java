package net.ion.ice.core.context;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.util.*;

/**
 * Created by jaeho on 2017. 5. 31..
 */
public class ExecuteContext implements Context{
    protected Map<String, Object> data  ;
    protected Node node;
    protected NodeType nodeType;
    protected Node existNode ;

    protected boolean exist ;
    protected boolean execute ;


    protected List<String> changedProperties ;
    protected String id ;

    protected String userId ;
    protected Date time ;
    protected String event;


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

    public static ExecuteContext makeContextFromMap(Map<String, Object> data, String typeId, String event) {
        EventExecuteContext ctx = new EventExecuteContext();

        ctx.setData(data);

        NodeType nodeType = NodeUtils.getNodeType(typeId) ;
        ctx.setNodeType(nodeType);

        ctx.event = event ;
        ctx.init() ;

        return ctx ;

    }

    protected void init() {
        this.time = new Date() ;
        if(nodeType == null){
            this.node = new Node(data);
            execute = true ;
            return ;
        }

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

    public boolean isDataType(){
        if(this.nodeType.getRepositoryType() == NodeType.DATA){
            return true;
        }else{
            return false;
        }
    }

    public static ExecuteContext makeContextFromConfig(Map<String, Object> config, Map<String, Object> data) {
        ExecuteContext ctx = new ExecuteContext();


        NodeType nodeType = NodeUtils.getNodeType((String) ContextUtils.getValue(config.get("typeId"), data));
        ctx.setNodeType(nodeType);

        ctx.event = (String) ContextUtils.getValue(config.get("event"), data);

        if(config.containsKey("data")){
            Map<String, Object> _data = new HashMap<>();
            Map<String, Object> subData = (Map<String, Object>) config.get("data");
            for(String key : subData.keySet()){
                _data.put(key, ContextUtils.getValue(key, data)) ;
            }
            ctx.data = _data ;
        }else{
            ctx.data = data ;
        }

        ctx.init() ;

        return ctx ;
    }

    public static ExecuteContext makeEventContextFromParameter(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, NodeType nodeType, String event) {
        EventExecuteContext ctx = new EventExecuteContext();

        Map<String, Object> data = ContextUtils.makeContextData(parameterMap, multiFileMap);

        ctx.setData(data);
        ctx.setNodeType(nodeType);
        ctx.event = event ;

        ctx.init() ;

        return ctx ;
    }


    public void execute() {
        EventService eventService = ApplicationContextManager.getBean(EventService.class) ;
        eventService.execute(this) ;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public String getEvent() {
        return event == null ? (exist ? "update" : "create") : event ;
    }

    public Map<String,Object> getData() {
        return data;
    }
}
