package net.ion.ice.core.node;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.event.Event;
import net.ion.ice.core.event.EventAction;
import net.ion.ice.core.event.EventListener;
import net.ion.ice.core.file.FileService;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.ResultField;
import net.ion.ice.core.query.SimpleQueryResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.SortField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 4. 3..
 */
@Service("nodeService")
public class NodeService {
    private Logger logger = LoggerFactory.getLogger(NodeService.class);

    @Autowired
    private InfinispanRepositoryService infinispanRepositoryService ;

    @Autowired
    private FileService fileService ;

    private Map<String, NodeType> nodeTypeCache = new ConcurrentHashMap<>() ;
    private Map<String, NodeType> initNodeType = new ConcurrentHashMap<>() ;

    @PostConstruct
    public void init(){
        try {
            initNodeType() ;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            initSchema();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.nodeTypeCache = new ConcurrentHashMap<>() ;
    }


    public NodeType getNodeType(String typeId) {
        if(nodeTypeCache.containsKey(typeId)) {
            return nodeTypeCache.get(typeId) ;
        }

        if(initNodeType.containsKey(typeId)){
            return initNodeType.get(typeId) ;
        }

        try {
            Node nodeTypeNode = infinispanRepositoryService.getNode("nodeType", typeId);

            NodeType nodeType = new NodeType(nodeTypeNode);
            if(!typeId.equals("propertyType")) {
                nodeType.setPropertyTypes(getNodeList("propertyType", "tid_matching=" + typeId));
            }

            if(!typeId.equals("event")) {
                nodeType.setEvents(getNodeList("event", "tid_matching=" + typeId));
            }

            nodeTypeCache.put(typeId, nodeType);
            return nodeType;
        }catch(Exception e){
            e.printStackTrace();
            logger.error("NOT FOUND nodeType : " + typeId + " - " + e.getMessage()) ;
            throw new RuntimeException("ERROR") ;
        }
    }


    public List<Node> getNodeList(String typeId, String searchText) {
        QueryContext queryContext = QueryContext.makeQueryContextFromText(searchText, getNodeType(typeId)) ;
        return infinispanRepositoryService.getSubQueryNodes(typeId, queryContext) ;
    }

    public List<Node> getNodeList(String typeId, QueryContext queryContext) {
        return infinispanRepositoryService.getSubQueryNodes(typeId, queryContext) ;
    }

    public SimpleQueryResult getNodeList(String typeId, Map<String, String[]> parameterMap) {
        QueryContext queryContext = QueryContext.makeQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        return infinispanRepositoryService.getQueryNodes(typeId, queryContext) ;
    }

    public SimpleQueryResult getNodeTree(String typeId, Map<String, String[]> parameterMap) {
        QueryContext queryContext = QueryContext.makeQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        return infinispanRepositoryService.getQueryTreeNodes(typeId, queryContext) ;
    }

    public SimpleQueryResult getNodeCode(String typeId, Map<String, String[]> parameterMap) {
        QueryContext queryContext = QueryContext.makeQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        return infinispanRepositoryService.getQueryCodeNodes(typeId, queryContext) ;
    }

    private void initNodeType() throws IOException {
        Collection<Map<String, Object>> nodeTypeDataList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("classpath:schema/core/nodeType.json")) ;

        List<Node> nodeTypeList = NodeUtils.makeNodeList(nodeTypeDataList, "nodeType") ;
        for(Node nodeType : nodeTypeList){
            initNodeType.put(nodeType.getId(), new NodeType(nodeType)) ;
        }

        Collection<Map<String, Object>> propertyTypeDataList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("classpath:schema/core/propertyType.json")) ;

        List<Node> propertyTypeList = NodeUtils.makeNodeList(propertyTypeDataList, "propertyType") ;
        for(Node propertyType : propertyTypeList){
            NodeType nodeType = initNodeType.get(propertyType.get("tid")) ;
            nodeType.addPropertyType(new PropertyType(propertyType));
        }

        Collection<Map<String, Object>> eventDataList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("classpath:schema/core/event.json")) ;

        List<Node> eventList = NodeUtils.makeNodeList(eventDataList, "event") ;
        for(Node event : eventList){
            if(event.get("typeId").equals("event")){
                NodeType nodeType = initNodeType.get(event.get("tid")) ;
                nodeType.addEvent(new Event(event));
            }else if(event.get("typeId").equals("eventAction")){
                EventAction eventAction = new EventAction(event) ;
                NodeType nodeType = initNodeType.get(eventAction.getTid()) ;
                nodeType.addEventAction(eventAction);
            }else if(event.get("typeId").equals("eventListener")){
                EventListener eventListener = new EventListener(event) ;
                NodeType nodeType = initNodeType.get(eventListener.getTid()) ;
                nodeType.addEventListener(eventListener);
            }
        }

    }

    private void  initSchema() throws IOException {

        NodeValue nodeValue = infinispanRepositoryService.getLastCacheNodeValue() ;
        String lastChanged = nodeValue == null ? "0" : DateTools.dateToString(nodeValue.getChanged(), DateTools.Resolution.SECOND);

        logger.info("LAST CHANGED : " + lastChanged);


        saveSchema("classpath:schema/core/*.json", lastChanged);
        saveSchema("classpath:schema/core/*/*.json", lastChanged);
        saveSchema("classpath:schema/node/*.json", lastChanged);
        saveSchema("classpath:schema/node/**/*.json", lastChanged);
        saveSchema("classpath:schema/test/*.json", lastChanged);
        saveSchema("classpath:schema/test/**/*.json", lastChanged);

    }

    private void saveSchema(String resourcePath, String lastChanged) throws IOException {

        Resource[] resources = ApplicationContextManager.getResources(resourcePath);

        for (Resource resource : resources) {
            if (resource.getFilename().equals("nodeType.json")) {
                fileNodeSave(lastChanged, resource);
            }
        }

        for (Resource resource : resources) {
            if (resource.getFilename().equals("propertyType.json")) {
                fileNodeSave(lastChanged, resource);
            }
        }

        for (Resource resource : resources) {
            if (resource.getFilename().equals("event.json")) {
                fileNodeSave(lastChanged, resource);
            }
        }

        for (Resource resource : resources) {
            if (!(resource.getFilename().equals("nodeType.json") || resource.getFilename().equals("propertyType.json") || resource.getFilename().equals("event.json"))) {
                fileNodeSave(lastChanged, resource);
            }
        }
    }

    private void fileNodeSave(String lastChanged, Resource resource) throws IOException {
        String fileName = StringUtils.substringBefore(resource.getFilename(), ".json");
        Collection<Map<String, Object>> nodeDataList = JsonUtils.parsingJsonResourceToList(resource) ;

        if(fileName.startsWith("20") && fileName.length() == 14 && lastChanged.compareTo(fileName) < 0){
            nodeDataList.forEach(data -> saveNode(data));
        }else{
            List<Map<String, Object>> dataList = NodeUtils.makeDataListFilterBy(nodeDataList, lastChanged) ;
            dataList.forEach(data -> saveNode(data));
        }
    }


    public Node saveNode(Map<String, Object> data) {
        try {
            ExecuteContext context = ExecuteContext.makeContextFromMap(data);
            context.execute();
            Node saveNode =  context.getNode();
            return saveNode ;
        }catch (Exception e){
            logger.error(data.toString(), e);
        }
        return null ;
    }


    public Node executeNode(Map<String, Object> data, String typeId, String event) {
        ExecuteContext context = ExecuteContext.makeContextFromMap(data, typeId, event) ;

        context.execute();
        Node node =  context.getNode();
        return node ;
    }


    public Node saveNode(Map<String, String[]> parameterMap, String typeId) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeContextFromParameter(parameterMap, nodeType) ;
//        if(context.isExecute() && context.isSyncTable()){
//            nodeBindingService.save(parameterMap, typeId);
//        }
//
//        Node node = infinispanRepositoryService.execute(context);
        context.execute();
        Node node =  context.getNode();
        node.toDisplay();
        return node;
    }

    public Node saveNode(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, String typeId) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeContextFromParameter(parameterMap, multiFileMap, nodeType) ;
        context.execute();
        Node node =  context.getNode();
        node.toDisplay();
        return node;
    }

    public Node deleteNode(Map<String, String[]> parameterMap, String typeId) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeContextFromParameter(parameterMap, nodeType) ;
        Node node = context.getNode() ;
        infinispanRepositoryService.deleteNode(node) ;
        return node ;
    }

    public Node deleteNode(String typeId, String id) {
        Node node = infinispanRepositoryService.getNode(typeId, id) ;
        infinispanRepositoryService.deleteNode(node) ;
        return node ;
    }

    public Node readNode(Map<String, String[]> parameterMap, String typeId, String id) {
        return readNode(typeId, id) ;
    }

    public Node readNode(Map<String, String[]> parameterMap, String typeId) {
        String id = null ;
        for(String paramName : parameterMap.keySet()){
            if(paramName.equals("id")){
                id = parameterMap.get(paramName)[0] ;
            }
        }

        if(id == null){
            List<String> idablePids = NodeUtils.getNodeType(typeId).getIdablePIds() ;
            id = "" ;
            for(int i = 0 ; i < idablePids.size(); i++){
                id = id + parameterMap.get(idablePids.get(i))[0] + (i < (idablePids.size() - 1) ? Node.ID_SEPERATOR : "") ;
            }
        }

        return readNode(typeId, id) ;

    }

    public Node readNode(String typeId, String id) {
        Node node = infinispanRepositoryService.getNode(typeId, id) ;
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;
        node.toDisplay();

        for(PropertyType pt : nodeType.getPropertyTypes(PropertyType.ValueType.REFERENCED)){
            QueryContext subQueryContext = QueryContext.makeQueryContextForReferenced(nodeType, pt, node) ;
            node.put(pt.getPid(), infinispanRepositoryService.getSubQueryNodes(pt.getReferenceType(), subQueryContext)) ;
        }

        return node ;
    }

    public Node getNode(String typeId, String id) {
        Node node = infinispanRepositoryService.getNode(typeId, id) ;
        return node ;
    }

    public Node read(String typeId, String id) {
        return infinispanRepositoryService.read(typeId, id) ;

    }

    public Object getSortedValue(String typeId, String pid, SortField.Type sortType, boolean reverse) {
        return infinispanRepositoryService.getSortedValue(typeId, pid, sortType, reverse) ;
    }

    public QueryResult getQueryResult(String query) {
        QueryContext queryContext = QueryContext.makeQueryContextFromQuery(query) ;
        QueryResult queryResult = queryContext.makeQueryResult( null);
        return queryResult;
    }




    public Node event(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, String typeId, String event) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeEventContextFromParameter(parameterMap, multiFileMap, nodeType, event) ;

        context.execute();

        return context.getNode() ;
    }

    public void changeNodeType(ExecuteContext context){

    }

    public List<Object> executeQuery(QueryContext queryContext) {
        return infinispanRepositoryService.executeQuery(queryContext) ;
    }
}
