package net.ion.ice.core.node;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.cluster.ClusterService;
import net.ion.ice.core.context.*;
import net.ion.ice.core.event.Event;
import net.ion.ice.core.event.EventAction;
import net.ion.ice.core.event.EventListener;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.file.FileService;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.SimpleQueryResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.search.SortField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
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

    @Autowired
    private ClusterService clusterService ;

    @Autowired
    private I18nConfiguration i18nConfiguration ;

    @Autowired
    private ApplicationContextManager applicationContextManager;

    @Autowired
    private Environment environment;


    private Map<String, NodeType> nodeTypeCache ;
    private Map<String, NodeType> initNodeType  ;
    private Map<String, Node> datasource  ;


    @PostConstruct
    public void init(){
        initNodeType = new ConcurrentHashMap<>() ;
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

    public String getDefaultLocale(){
        return i18nConfiguration.getDefaults() ;
    }

    public NodeType getNodeType(String typeId) {
        if(typeId == null){
            logger.error("NOT FOUND TypeId is NULL!") ;
            return null ;
        }

        if(nodeTypeCache != null && nodeTypeCache.containsKey(typeId)) {
            return nodeTypeCache.get(typeId) ;
        }


        if(initNodeType.containsKey(typeId)){
            return initNodeType.get(typeId) ;
        }

        try {
            Node nodeTypeNode = infinispanRepositoryService.getNode("nodeType", typeId);
            if(nodeTypeNode == null) return null ;

            NodeType nodeType = new NodeType(nodeTypeNode);
            if(typeId.equals("propertyType")) {
                if(initNodeType.containsKey("propertyType")) {
                    nodeType.setPropertyTypes(getNodeList("propertyType", "tid_matching=" + typeId));
                }
            }else{
                nodeType.setPropertyTypes(getNodeList("propertyType", "tid_matching=" + typeId));
            }

            if(typeId.equals("event")) {
                if(initNodeType.containsKey("event")) {
                    nodeType.setEvents(getNodeList("event", "tid_matching=" + typeId));
                }
            }else{
                nodeType.setEvents(getNodeList("event", "tid_matching=" + typeId));
            }

            if(nodeTypeCache != null) {
                nodeTypeCache.put(typeId, nodeType);
            }
            return nodeType;
        }catch(Exception e){
            e.printStackTrace();
            logger.error("NOT FOUND nodeType : " + typeId + " - " + e.getMessage()) ;
            throw new RuntimeException("ERROR") ;
        }
    }


    public List<Node> getNodeList(String typeId, String searchText) {
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, getNodeType(typeId)) ;
        return infinispanRepositoryService.getSubQueryNodes(typeId, queryContext) ;
    }

    public List<Node> getNodeList(NodeType nodeType, List<QueryTerm> queryTerms) {
        QueryContext queryContext = QueryContext.createQueryContextFromTerms(queryTerms, nodeType) ;
        return infinispanRepositoryService.getSubQueryNodes(nodeType.getTypeId(), queryContext) ;

    }


    public List<Node> getNodeList(String typeId, QueryContext queryContext) {
        return infinispanRepositoryService.getSubQueryNodes(typeId, queryContext) ;
    }
    public List<Node> getDisplayNodeList(String typeId, QueryContext queryContext) {
        NodeType nodeType = getNodeType(typeId) ;
        List<Node> nodeList = queryContext.getQueryList() ;
        for(Node node : nodeList){
            node.toDisplay(queryContext) ;
            if (queryContext.isTreeable()) {
                for (PropertyType pt : nodeType.getPropertyTypes()) {
                    if (pt.isTreeable()) {
                        QueryContext subQueryContext = QueryContext.makeQueryContextForTree(nodeType, pt, node.getId().toString());
                        subQueryContext.setTreeable(true);
                        node.put("children", getDisplayNodeList(pt.getReferenceType(), subQueryContext));
                    }
                }
            }
        }
        return nodeList ;
    }



    public SimpleQueryResult getNodeList(String typeId, Map<String, String[]> parameterMap) {
        QueryContext queryContext = QueryContext.createQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        return infinispanRepositoryService.getQueryNodes(typeId, queryContext) ;
    }

    public SimpleQueryResult getNodeTree(String typeId, Map<String, String[]> parameterMap) {
        QueryContext queryContext = QueryContext.createQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        return infinispanRepositoryService.getQueryTreeNodes(typeId, queryContext) ;
    }

    private void initNodeType() throws IOException {
        initSaveNodeType("classpath:schema/core/nodeType.json");
        initPropertyType("classpath:schema/core/propertyType.json");

        Collection<Map<String, Object>> eventDataList = JsonUtils.parsingJsonResourceToList(applicationContextManager.getResource("classpath:schema/core/event.json")) ;

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

        initSaveNodeType("classpath:schema/core/datasource/nodeType.json");
        initPropertyType("classpath:schema/core/datasource/propertyType.json");

        datasource = new ConcurrentHashMap<>() ;

        initDatasource("classpath:schema/core/datasource/dataSource.json");

        try {
            initDatasource("classpath:schema/core/datasource/" + environment.getActiveProfiles()[0] + "/dataSource.json");
        }catch(Exception e){

        }


    }

    private void initDatasource(String file) throws IOException {
        Collection<Map<String, Object>> dsDataList = JsonUtils.parsingJsonResourceToList(applicationContextManager.getResource(file)) ;

        List<Node> dsList = NodeUtils.makeNodeList(dsDataList, "datasource") ;
        for(Node ds : dsList){
            datasource.put(ds.getId(), ds) ;
        }
    }

    private void initPropertyType(String file) throws IOException {
        Collection<Map<String, Object>> propertyTypeDataList = JsonUtils.parsingJsonResourceToList(applicationContextManager.getResource(file)) ;

        List<Node> propertyTypeList = NodeUtils.makeNodeList(propertyTypeDataList, "propertyType") ;
        for(Node propertyType : propertyTypeList){
            NodeType nodeType = initNodeType.get(propertyType.get("tid")) ;
            nodeType.addPropertyType(new PropertyType(propertyType));
        }
    }

    private void initSaveNodeType(String file) throws IOException {
        Collection<Map<String, Object>> nodeTypeDataList = JsonUtils.parsingJsonResourceToList(applicationContextManager.getResource(file)) ;

        List<Node> nodeTypeList = NodeUtils.makeNodeList(nodeTypeDataList, "nodeType") ;
        for(Node nodeType : nodeTypeList){
            initNodeType.put(nodeType.getId(), new NodeType(nodeType)) ;
        }
    }

    private void  initSchema() throws IOException {
        saveSchema("classpath:schema/core/*.json");
        saveSchema("classpath:schema/core/*/*.json");
        saveSchema("classpath:schema/core/datasource/" + environment.getActiveProfiles()[0] + "/dataSource.json");

//        saveSchema("classpath:schema/node/*.json", lastChanged);
//        saveSchema("classpath:schema/node/**/*.json");
//        saveSchema("classpath:schema/test/*.json", lastChanged);
//        saveSchema("classpath:schema/test/**/*.json");

    }
    public void saveSchema(String resourcePath) throws IOException {
        saveSchema(resourcePath, true);
        saveSchema(resourcePath, false);
    }

    private void saveSchema(String resourcePath, boolean core) throws IOException {
        Resource[] resources = applicationContextManager.getResources(resourcePath);
        if(core) {
            for (Resource resource : resources) {
                if (resource.getFilename().equals("nodeType.json")) {
                    fileNodeSave(resource);
                }
            }

            for (Resource resource : resources) {
                if (resource.getFilename().equals("propertyType.json")) {
                    fileNodeSave(resource);
                }
            }

            for (Resource resource : resources) {
                if (resource.getFilename().equals("event.json")) {
                    fileNodeSave(resource);
                }
            }
        }else {
            for (Resource resource : resources) {
                if (!(resource.getFilename().equals("nodeType.json") || resource.getFilename().equals("propertyType.json") || resource.getFilename().equals("event.json"))) {
                    fileNodeSave(resource);
                }
            }
        }
    }

    private void fileNodeSave(Resource resource) throws IOException {
        String fileName = StringUtils.substringBefore(resource.getFilename(), ".json");
        Collection<Map<String, Object>> nodeDataList = JsonUtils.parsingJsonResourceToList(resource) ;

//        List<Map<String, Object>> dataList = NodeUtils.makeDataListFilterBy(nodeDataList, lastChanged) ;
        nodeDataList.forEach(data -> saveNode(data));
    }


    public Node saveNode(Map<String, Object> data) {
        try {
            NodeType nodeType = getNodeType(data.get(Node.TYPEID).toString()) ;
            if(!clusterService.checkClusterGroup(nodeType)) return null;

            ExecuteContext context = ExecuteContext.makeContextFromMap(data);
            context.execute();
            Node saveNode =  context.getNode();
            return saveNode ;
        }catch (Exception e){
            logger.error(data.toString(), e);
        }
        return null ;
    }

    public Node createNode(Map<String, Object> data, String typeId) {
        return (Node) executeNode(data, typeId, EventService.CREATE);
    }

    public Node updateNode(Map<String, Object> data, String typeId) {
        return (Node) executeNode(data, typeId, EventService.UPDATE);
    }


    public Object executeNode(Map<String, Object> data, String typeId, String event) {
        ExecuteContext context = ExecuteContext.makeContextFromMap(data, typeId, event) ;
        context.execute();
        return context.getResult();
    }


    public Object executeNode(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, String typeId, String event) {
        NodeType nodeType = getNodeType(typeId) ;
        if(!clusterService.checkClusterGroup(nodeType)){
            throw new IceRuntimeException("Not Support Type Error") ;
        }

        ExecuteContext context = ExecuteContext.makeContextFromParameter(parameterMap, multiFileMap, nodeType, event) ;
        context.execute();
//        Node node = (Node) context.getResult();
//        node.toDisplay();
        return context.makeResult();
    }

    public Object deleteNode(Map<String, String[]> parameterMap, String typeId, String id) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.createContextFromParameter(parameterMap, nodeType, "delete", id) ;
        context.execute();

//        Node node = (Node) context.getResult();
        return context.makeResult() ;
    }

    public Node deleteNode(String typeId, String id) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.createContextFromParameter(null, nodeType, "delete", id) ;
        context.execute();

//        Node node = context.getNode() ;

//        Node node = infinispanRepositoryService.getNode(typeId, id) ;
//        infinispanRepositoryService.deleteNode(node) ;
        return context.getNode() ;
    }

    public Object readNode(Map<String, String[]> parameterMap, String typeId, String id) {
        NodeType nodeType = getNodeType(typeId) ;

        ReadContext readContext = ReadContext.createContextFromParameter(parameterMap, nodeType, id) ;
        return readContext.makeResult() ;
//        return readNode(typeId, id) ;
    }

    public Object readNode(Map<String, String[]> parameterMap, String typeId) {
        NodeType nodeType = getNodeType(typeId) ;

        ReadContext readContext = ReadContext.createContextFromParameter(parameterMap, nodeType, null) ;
        return readContext.makeResult() ;
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

    public QueryResult getQueryResult(Map<String, String[]> parameterMap) throws IOException {
        Map<String, Object> config = JsonUtils.parsingJsonToMap(parameterMap.get("_config_")[0]) ;
        Map<String, Object> data = ContextUtils.makeContextData(parameterMap) ;

        ApiQueryContext queryContext = ApiQueryContext.makeContextFromConfig(config, data) ;
        QueryResult queryResult = queryContext.makeQueryResult();
        return queryResult;
    }

    public QueryResult getQueryResult(String typeId, Map<String, String[]> parameterMap) {
        QueryContext queryContext = QueryContext.createQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        QueryResult queryResult = queryContext.makeQueryResult();
        return queryResult;
    }

    public QueryResult getReferenceQueryResult(String typeId, Map<String, String[]> parameterMap) {
        ReferenceQueryContext queryContext = ReferenceQueryContext.createQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        QueryResult queryResult = queryContext.makeQueryResult();
        return queryResult;
    }

    public QueryResult getCodeQueryResult(String typeId, Map<String, String[]> parameterMap) {
        ReferenceQueryContext queryContext = ReferenceQueryContext.createQueryContextFromParameter(parameterMap, getNodeType(typeId)) ;
        QueryResult queryResult = queryContext.makeQueryResult();
        return queryResult;
    }

    public Node event(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, String typeId, String event) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeEventContextFromParameter(parameterMap, multiFileMap, nodeType, event) ;

        context.execute();

        return context.getNode() ;
    }


    public List<Object> executeQuery(QueryContext queryContext) {
        return infinispanRepositoryService.executeQuery(queryContext) ;
    }

    public void removeNodeTypeCache(String typeId){
        if(this.nodeTypeCache != null){
            this.nodeTypeCache.remove(typeId) ;
        }
    }

    public void changeNodeType(ExecuteContext context){
        removeNodeTypeCache(context.getNode().getId()) ;
        logger.info("Change NodeType : " + context.getNode().getId());
    }

    public void changePropertyType(ExecuteContext context){
        Node node = context.getNode() ;
        NodeType nodeType = getNodeType(node.getStringValue("tid")) ;
        nodeType.addPropertyType(new PropertyType(node));
        logger.info("Change PropertyType : " + node.getStringValue("tid"));
    }

    public void changeEvent(ExecuteContext context){
        Node node = context.getNode() ;
        NodeType nodeType = getNodeType(node.getStringValue("tid")) ;
        nodeType.addEvent(new Event(node));
        logger.info("Change Event : " + context.getNode().getStringValue("tid"));
    }


    public void changeEventAction(ExecuteContext context){
        Node node = context.getNode() ;
        NodeType nodeType = getNodeType(StringUtils.substringBefore(node.getStringValue("event"), Node.ID_SEPERATOR)) ;

        EventAction eventAction = new EventAction(node) ;
        nodeType.addEventAction(eventAction);

        logger.info("Change EventAction : " + StringUtils.substringBefore(context.getNode().getStringValue("event"), Node.ID_SEPERATOR));
    }


    public void changeEventListener(ExecuteContext context){
        Node node = context.getNode() ;
        NodeType nodeType = getNodeType(StringUtils.substringBefore(node.getStringValue("event"), Node.ID_SEPERATOR)) ;

        EventListener eventListener = new EventListener(node) ;
        nodeType.addEventListener(eventListener);

        logger.info("Change EventAction : " + StringUtils.substringBefore(context.getNode().getStringValue("event"), Node.ID_SEPERATOR));
    }


    public void startBatch(String typeId){
        infinispanRepositoryService.startBatch(typeId);
    }
    public void endBatch(String typeId, boolean commit){
        infinispanRepositoryService.endBatch(typeId, commit);
    }

    public Node getDatasource(String dsId) {
        return this.datasource.get(dsId) ;
    }
}
