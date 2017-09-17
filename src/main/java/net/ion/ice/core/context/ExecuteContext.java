package net.ion.ice.core.context;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryTerm;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by jaeho on 2017. 5. 31..
 */
public class ExecuteContext extends ReadContext{

    protected Node existNode ;

    protected boolean exist ;
    protected boolean execute ;


    protected List<String> changedProperties ;

    protected String userId ;
    protected Date time ;
    protected String event;

    protected String ifTest ;

    protected ExecuteContext parentContext ;
    protected List<ExecuteContext> subExecuteContexts ;

    public static ExecuteContext createContextFromParameter(Map<String, String[]> parameterMap, NodeType nodeType, String event, String id) {
        ExecuteContext ctx = new ExecuteContext();

        Map<String, Object> data = ContextUtils.makeContextData(parameterMap);

        ctx.setData(data);
        if(id != null){
            ctx.id = id ;
        }
        if(event != null) {
            ctx.event = event;
        }
        ctx.setNodeType(nodeType);

        ctx.init() ;

        return ctx ;
    }

    public static ExecuteContext makeContextFromParameter(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, NodeType nodeType, String event) {
        ExecuteContext ctx = new ExecuteContext();

        Map<String, Object> data = ContextUtils.makeContextData(parameterMap, multiFileMap);

        ctx.setData(data);

        if(event != null) {
            ctx.event = event;
        }
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

    public static ExecuteContext makeContextFromMap(Map<String, Object> data, String typeId, ExecuteContext parentContext) {
        ExecuteContext ctx = new ExecuteContext();
        ctx.parentContext = parentContext ;
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
        try {
            existNode = NodeUtils.getNode(nodeType.getTypeId(), getId());
        }catch(Exception e){
        }
        exist = existNode != null ;
        if(this.event == null && data.containsKey("event") && getNodeType().getPropertyType("event") == null){
            this.event = (String) data.get("event");
        }

        if(exist){
            if(event != null && event.equals("create")){
                throw new IceRuntimeException("Exist Node Error : " + getId()) ;
            }

            changedProperties = new ArrayList<>() ;
            this.node = existNode.clone() ;
            for(PropertyType pt : nodeType.getPropertyTypes()){
                if(!data.containsKey(pt.getPid()) && !pt.isI18n()){
                    continue;
                }


                Object newValue = NodeUtils.getStoreValue(data, pt, node.getId()) ;
                if(pt.isI18n() && newValue == null){
                    continue;
                }

                Object existValue = existNode.get(pt.getPid()) ;

                if(newValue == null && existValue == null) {
                    continue;
                }else if(existValue != null &&newValue instanceof String && "_null_".equals(newValue)){
                    node.remove(pt.getPid()) ;
                    changedProperties.add(pt.getPid()) ;
                }else if(pt.isFile()){
                    if(newValue != null && newValue instanceof String && ((String) newValue).contains("classpath:")) {
                        if (existValue == null) {
                            node.put(pt.getPid(), NodeUtils.getFileService().saveResourceFile(pt, id, (String) newValue));
                            changedProperties.add(pt.getPid());
                        } else if (existValue instanceof FileValue && !((FileValue) existValue).getFileName().equals(StringUtils.substringAfterLast((String) newValue, "/"))) {
                            node.put(pt.getPid(), NodeUtils.getFileService().saveResourceFile(pt, id, (String) newValue));
                            changedProperties.add(pt.getPid());
                        }
                    }else if(newValue != null && newValue instanceof FileValue){
                        node.put(pt.getPid(), newValue) ;
                        changedProperties.add(pt.getPid()) ;
                    }else if(newValue == null && existValue != null) {
                        node.remove(pt.getPid()) ;
                        changedProperties.add(pt.getPid()) ;
                    }else if(pt.isI18n()){
                        ((Map<String, Object>) existValue).putAll((Map<? extends String, ?>) newValue);
                        node.put(pt.getPid(), existValue);
                        changedProperties.add(pt.getPid()) ;
                    }
                    continue;
                }else if(newValue == null && existValue != null){
                    node.remove(pt.getPid()) ;
                    changedProperties.add(pt.getPid()) ;
                }else if(newValue != null && existValue == null){
                    node.put(pt.getPid(), newValue) ;
                    changedProperties.add(pt.getPid()) ;
                }else if(!newValue.equals(existValue)){
                    if(pt.isI18n()){
                        ((Map<String, Object>) existValue).putAll((Map<? extends String, ?>) newValue);
                        node.put(pt.getPid(), existValue);
                    }else {
                        node.put(pt.getPid(), newValue);
                    }
                    changedProperties.add(pt.getPid()) ;
                }
            }
            execute = changedProperties.size() > 0 ;
            if(execute) {
                node.setUpdate(userId, time);
            }else if(event != null && !event.equals("update") ){
                execute = true;
            }

        }else {
            if(event != null && !event.equals("create") && !event.equals("update") && !event.equals("save")) {
                execute = true;
                return;
            }else if(event != null && event.equals("update")){
                throw new IceRuntimeException("Not Exist Node Error : " + getId()) ;
            }
            try {
                this.node = new Node(data, nodeType.getTypeId());
                this.id = this.node.getId() ;
                execute = true;
            }catch(Exception e){
                execute =false ;
            }
        }

        for(String key : data.keySet()){
            Object value = data.get(key) ;
            if(value instanceof List && (this.nodeType.getPropertyType(key) == null || !this.nodeType.getPropertyType(key).isList()) && NodeUtils.getNodeType(key) != null){
                for(Map<String, Object> subData : (List<Map<String, Object>>)value){
                    Map<String, Object> _data = new HashMap<>() ;
                    _data.putAll(data);
                    _data.remove(key) ;
                    _data.putAll(subData);
                    for(String _key : subData.keySet()){
                        Object _val = subData.get(_key) ;
                        if(_val instanceof String && "_parentId_".equals(_val)){
                            _data.put(_key, this.id) ;
                        }
                    }
                    ExecuteContext subContext = ExecuteContext.makeContextFromMap(_data, key, this) ;
                    if(subExecuteContexts == null){
                        subExecuteContexts = new ArrayList<>() ;
                    }
                    if(subContext != null) {
                        subExecuteContexts.add(subContext);
                    }
                }
            }
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
            List<String> idPids = nodeType.getIdablePIds() ;
            if(data.containsKey("id")){
                String _id = data.get("id").toString() ;
                if(_id.contains(">")) {
                    id = data.get("id").toString();
                }else if(idPids.size() == 1){
                    id = data.get("id").toString();
                }
            }

            if(id == null){
                id = "" ;
                for(int i = 0 ; i < idPids.size(); i++){
                    id = id + data.get(idPids.get(i)) + (i < (idPids.size() - 1) ? Node.ID_SEPERATOR : "") ;
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
                _data.put(key, ContextUtils.getValue(subData.get(key), data)) ;
            }
            ctx.data = _data ;
        }else{
            ctx.data = data ;
        }

        if(config.containsKey("response")){
            ContextUtils.makeApiResponse((Map<String, Object>) config.get("response"), ctx);
        }

        if(config.containsKey("if")){
            ctx.ifTest =  ContextUtils.getValue(config.get("if"), data).toString();
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


    public boolean execute() {
        if(this.ifTest != null && !(this.ifTest.equalsIgnoreCase("true"))){
            return false ;
        }
        EventService eventService = ApplicationContextManager.getBean(EventService.class) ;
        eventService.execute(this) ;

        if(subExecuteContexts != null){
            for(ExecuteContext subExecuteContext : subExecuteContexts){
                eventService.execute(subExecuteContext);
            }
        }
        return true ;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public String getEvent() {
        return event == null ? (exist ? "update" : "create") : (StringUtils.equals(event, "save") ? (exist ? "update" : "create") : event ) ;
    }

    public Map<String,Object> getData() {
        return data;
    }

    public ExecuteContext makeRollbackContext() {
        EventExecuteContext ctx = new EventExecuteContext();
        ctx.nodeType = nodeType ;
        switch (getEvent()){
            case "create" :
                ctx.event = "delete" ;
                ctx.node = node ;
                ctx.execute = true ;
                break;
            case "update" :
                ctx.event = "update" ;
                ctx.node = existNode ;
                ctx.execute = true ;
                break ;
            case "delete" :
                ctx.event = "create" ;
                ctx.node = node ;
                ctx.execute = true ;
                break ;
            default:
                break ;
        }
        ctx.data = data ;
        return ctx ;
    }

    public Object getResult(){
        if(result != null){
            return result ;
        }else if(this.node != null){
            this.result = node ;
            return node ;
        }else{
            return NodeUtils.getNode(nodeType.getTypeId(), id) ;
        }
    }
}
