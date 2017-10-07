package net.ion.ice.core.context;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import net.ion.ice.core.query.QueryTerm;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
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

    protected ExecuteContext parentContext ;
    protected List<ExecuteContext> subExecuteContexts ;

    protected HttpServletRequest httpRequest ;
    protected HttpServletResponse httpResponse ;

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
        if(this.event == null && data.containsKey("event") && getNodeType().getPropertyType("event") == null){
            this.event = (String) data.get("event");
        }


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
                    if(newValue != null) data.put(pt.getPid(), newValue) ;
                    if(newValue != null && newValue instanceof String && (((String) newValue).startsWith("classpath:") || ((String) newValue).startsWith("http://") || ((String) newValue).startsWith("/"))) {
                        if (existValue == null) {
                            node.put(pt.getPid(), NodeUtils.getFileService().saveResourceFile(pt, id, (String) newValue));
                            changedProperties.add(pt.getPid());
                        } else if (existValue instanceof FileValue && !((FileValue) existValue).getFileName().equals(StringUtils.substringAfterLast((String) newValue, "/"))) {
                            node.put(pt.getPid(), NodeUtils.getFileService().saveResourceFile(pt, id, (String) newValue));
                            changedProperties.add(pt.getPid());
                        }
                    }else if(newValue != null && newValue instanceof FileValue){
                        FileValue newFileValue = ((FileValue) newValue);
                        String newValueStorePath = newFileValue.getStorePath();
                        String[] newValueStorePathSplit = StringUtils.split(newValueStorePath, "/");
                        String newValueTid = newValueStorePathSplit.length > 1 ? newValueStorePathSplit[0] : "";
                        String newValuePid = newValueStorePathSplit.length > 2 ? newValueStorePathSplit[1] : "";

                        if (!StringUtils.equals(newValueTid, nodeType.getTypeId()) && !StringUtils.equals(newValuePid, pt.getPid()) ) {
                            Resource resource = NodeUtils.getFileService().loadAsResource(newValueTid, newValuePid, newValueStorePath);
                            File resourceFile = null;
                            try {
                                resourceFile = resource.getFile();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FileValue fileValue = NodeUtils.getFileService().saveFile(pt, id, resourceFile, newFileValue.getFileName(), newFileValue.getContentType());
                            node.put(pt.getPid(), fileValue) ;
                            changedProperties.add(pt.getPid()) ;
                        } else {
                            node.put(pt.getPid(), newValue) ;
                            changedProperties.add(pt.getPid()) ;
                        }
                    }else if(newValue == null && existValue != null) {
                        node.remove(pt.getPid()) ;
                        changedProperties.add(pt.getPid()) ;
                    }else if(pt.isI18n()){
                        i18nRemove((Map<? extends String, ?>) newValue, (Map<String, Object>) existValue);
                        for(String locKey : ((Map<String, Object>) existValue).keySet()){
                            Object locVal = ((Map<String, Object>) existValue).get(locKey) ;
                            if(locVal instanceof String && (((String) locVal).startsWith("classpath:") || ((String) locVal).startsWith("http://") || ((String) locVal).startsWith("/"))) {
                                ((Map<String, Object>) existValue).put(locKey, NodeUtils.getFileService().saveResourceFile(pt, id, (String) locVal));
                            }
                        }
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
                        i18nRemove((Map<? extends String, ?>) newValue, (Map<String, Object>) existValue);
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
            }else if(event != null && !(event.equals("update") || event.equals("save"))){
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

        List<String> subDataKeys = new ArrayList<>();

        for(String key : data.keySet()){
            Object value = data.get(key) ;
            if(value instanceof List && (this.nodeType.getPropertyType(key) == null || !this.nodeType.getPropertyType(key).isList()) && NodeUtils.getNodeType(key) != null){
                for(Map<String, Object> subData : (List<Map<String, Object>>)value){
                    Map<String, Object> _data = new HashMap<>() ;
//                    _data.putAll(data);
//                    _data.remove(key) ;
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

                subDataKeys.add(key);
            }
        }

        for(String subDataKey : subDataKeys) {
            data.remove(subDataKey);
        }
    }

    private void i18nRemove(Map<? extends String, ?> newValue, Map<String, Object> existValue) {
        if(existValue == null){
            existValue = (Map<String, Object>) newValue;
        }else {
            existValue.putAll(newValue);
        }
        List<String> removeLocale = new ArrayList<>() ;
        for(String key :  existValue.keySet()){
            Object val =  existValue.get(key) ;
            if(val instanceof String && val.equals("_null_")){
                removeLocale.add(key) ;
            }
        }
        for(String loc : removeLocale){
            existValue.remove(loc) ;
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
        EventService eventService = ApplicationContextManager.getBean(EventService.class);
        eventService.execute(this);

        if (subExecuteContexts != null) {
            for (ExecuteContext subExecuteContext : subExecuteContexts) {
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
        }else if(StringUtils.isNotEmpty(id)){
            try {
                return NodeUtils.getNode(nodeType.getTypeId(), id);
            }catch(Exception e){
                return new HashMap<String, Object>() ;
            }
        }else{
            return new HashMap<String, Object>() ;
        }
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }
}
