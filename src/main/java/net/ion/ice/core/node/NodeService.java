package net.ion.ice.core.node;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.file.FileService;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.SimpleQueryResult;
import net.ion.ice.core.query.ResultField;
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
import java.util.*;
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
    private NodeBindingService nodeBindingService;

    @Autowired
    private FileService fileService ;

    private NodeType nodeType ;
    private NodeType propertyType ;

    private Map<String, NodeType> nodeTypeCache = new ConcurrentHashMap<>() ;

    @PostConstruct
    public void init(){
        try {
            initNodeType(true) ;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            initNodeType(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.nodeTypeCache = new ConcurrentHashMap<>() ;
    }


    public NodeType getNodeType(String typeId) {
        if(nodeTypeCache.containsKey(typeId)) {
            return nodeTypeCache.get(typeId) ;
        }

        if(typeId.equals("nodeType")){
            return nodeType ;
        }else if(typeId.equals("propertyType")){
            return propertyType ;
        }

        Node nodeTypeNode = infinispanRepositoryService.getNode("nodeType", typeId) ;

        NodeType nodeType = new NodeType(nodeTypeNode) ;
        nodeType.setPropertyTypes(getNodeList("propertyType", "tid_matching=" + typeId));

        nodeTypeCache.put(typeId, nodeType) ;
        return nodeType ;
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

    private void initNodeType(boolean preConstruct) throws IOException {
        Collection<Map<String, Object>> nodeTypeDataList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("classpath:schema/core/nodeType.json")) ;

        List<Node> nodeTypeList = NodeUtils.makeNodeList(nodeTypeDataList, "nodeType") ;
        for(Node nodeType : nodeTypeList){
            if(preConstruct) {
                if (nodeType.getId().equals("nodeType")) {
                    this.nodeType = new NodeType(nodeType);
                } else if (nodeType.getId().equals("propertyType")) {
                    this.propertyType = new NodeType(nodeType);
                }
            }else {
                saveFileNode(nodeType, "nodeType");
            }
        }

        Collection<Map<String, Object>> propertyTypeDataList = JsonUtils.parsingJsonResourceToList(ApplicationContextManager.getResource("classpath:schema/core/propertyType.json")) ;

        List<Node> propertyTypeList = NodeUtils.makeNodeList(propertyTypeDataList, "propertyType") ;
        for(Node propertyType : propertyTypeList){
            if(preConstruct) {
                if (propertyType.get("tid").equals("nodeType")) {
                    this.nodeType.addPropertyType(new PropertyType(propertyType));
                } else if (propertyType.get("tid").equals("propertyType")) {
                    this.propertyType.addPropertyType(new PropertyType(propertyType));
                }
            }else {
                saveFileNode(propertyType, "propertyType");
            }
        }

        if(!preConstruct) {
            initSchema("classpath:schema/core");
            initSchema("classpath:schema/node");
        }

    }

    private void initSchema(String resourcePath) throws IOException {
        Resource resource = ApplicationContextManager.getResource(resourcePath) ;
        File initNodeDir =  resource.getFile() ;

        NodeValue nodeValue = infinispanRepositoryService.getLastCacheNodeValue() ;
        String lastChanged = DateTools.dateToString(nodeValue.getChanged(), DateTools.Resolution.SECOND);

        logger.info("LAST CHANGED : " + lastChanged);
        for(File dir : initNodeDir.listFiles((File f) -> { return f.isDirectory() ; })){
            for(File f : dir.listFiles((File f) -> {return f.getName().equals("nodeType.json");})){
                fileNodeSave(lastChanged, f);
            }
            for(File f : dir.listFiles((File f) -> {return f.getName().equals("propertyType.json");})){
                fileNodeSave(lastChanged, f);
            }
            for(File f : dir.listFiles((File f) -> {return f.getName().endsWith(".json") && !(f.getName().equals("nodeType.json") || f.getName().equals("propertyType.json"));})){
                fileNodeSave(lastChanged, f);
            }
        }
    }

    private void fileNodeSave(String lastChanged, File f) throws IOException {
        String fileName = StringUtils.substringBefore(f.getName(), ".json");
        Collection<Map<String, Object>> nodeDataList = JsonUtils.parsingJsonFileToList(f) ;

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

            Node saveNode =  infinispanRepositoryService.execute(context);
            if(context.isExecute() && context.isSyncTable()){
//                nodeBindingService.save(data);
            }
            return saveNode ;
        }catch (Exception e){
            logger.error(data.toString(), e);
        }
        return null ;
    }

    public Node saveFileNode(Map<String, Object> data, String typeId) {
        ExecuteContext context = ExecuteContext.makeContextFromMap(data, typeId) ;
        return infinispanRepositoryService.execute(context);
    }

    public Node saveNode(Map<String, String[]> parameterMap, String typeId) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeContextFromParameter(parameterMap, nodeType) ;
        if(context.isExecute() && context.isSyncTable()){
            nodeBindingService.save(parameterMap, typeId);
        }

        return infinispanRepositoryService.execute(context);
    }

    public Node saveNode(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, String typeId) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeContextFromParameter(parameterMap, multiFileMap, nodeType) ;

        return infinispanRepositoryService.execute(context);
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
        QueryResult queryResult = new QueryResult() ;
        makeQueryResult(queryResult, queryContext, null);
        return queryResult;
    }

    private QueryResult makeQueryResult(QueryResult queryResult, QueryContext queryContext, Object result) {
        NodeType nodeType = queryContext.getNodetype() ;
        Node node = null ;

        if(result instanceof Node){
            node = (Node) result;
        }

        for(ResultField resultField :  queryContext.getResultFields()){
            if(resultField.getQueryContext() != null){
                QueryContext subQueryContext = resultField.getQueryContext() ;
                List<Object> resultList = infinispanRepositoryService.executeQuery(subQueryContext) ;
                List<QueryResult> queryResults = new ArrayList<>(resultList.size()) ;
                if(subQueryContext.getResultFields() != null){
                    for(Object obj : resultList){
                        queryResults.add(makeQueryResult(new QueryResult(), subQueryContext, obj)) ;
                    }
                }
                queryResult.put(resultField.getFieldName(), queryResults) ;
            }else if(node != null){
                String fieldValue = resultField.getFieldValue() ;
                fieldValue = fieldValue == null || StringUtils.isEmpty(fieldValue) ? resultField.getFieldName() : fieldValue ;
                queryResult.put(resultField.getFieldName(), NodeUtils.getResultValue(node.get(fieldValue), nodeType.getPropertyType(fieldValue), node)) ;
            }
        }
        return queryResult ;
    }


    public Node event(Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, String typeId, String event) {
        NodeType nodeType = getNodeType(typeId) ;

        ExecuteContext context = ExecuteContext.makeEventContextFromParameter(parameterMap, multiFileMap, nodeType, event) ;

        return infinispanRepositoryService.execute(context);
    }
}
