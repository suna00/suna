package net.ion.ice.core.node;

import com.hazelcast.core.Member;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.cluster.ClusterService;
import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.query.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.lucene.search.SortField;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("nodeHelperService")
public class NodeHelperService  {
    private Logger logger = LoggerFactory.getLogger(NodeHelperService.class);

    @Autowired
    private NodeService nodeService ;

    @Autowired
    private InfinispanRepositoryService infinispanRepositoryService ;


    @Autowired
    private ClusterService clusterService ;

    @Autowired
    private DBService dbService ;


    private Map<String, Date> lastChangedMap = new ConcurrentHashMap<>() ;

    public void  initSchema(String profile) throws IOException {
        saveResourceSchema("classpath:schema/core/*.json");
        saveResourceSchema("classpath:schema/core/*/*.json");
        try {
            saveResourceSchema("classpath:schema/core/datasource/" + profile + "/dataSource.json");
        }catch (Exception e){}
        syncSchema() ;
    }

    public void syncSchema() throws IOException {
//        syncNodeList(nodeService.getNodeType("nodeType"), "limit=1000");
        syncNodeType("nodeType");

        syncNodeType("propertyType");

        Cache<String, Node> nodeTypeCache = infinispanRepositoryService.getNodeCache("nodeType") ;

        for(String typeId : nodeTypeCache.keySet()){
            try {
                syncNodeType(typeId);
            }catch (Exception e){
                logger.error("sync list error : " + typeId);
            }
        }
    }

    private void syncNodeType(String typeId) {
        NodeType nodeType = nodeService.getNodeType(typeId) ;
        if(nodeType.isNode() && !clusterService.checkClusterGroup(nodeType)) return  ;
        Date nodeTypeLast = (Date) nodeService.getSortedValue(nodeType.getTypeId(), "changed", SortField.Type.LONG, true );
        if(nodeTypeLast == null){
            logger.info(nodeType.getTypeId() + " ALL Sync : ");
            syncNodeList(nodeType, "limit=100&sorting=changed desc", null);
        }else {
            String lastChanged = DateFormatUtils.format(nodeTypeLast, "yyyyMMddHHmmss");
            logger.info(nodeType.getTypeId() + " Last Sync : " + nodeTypeLast);
            syncNodeList(nodeType, "limit=100&sorting=changed desc&changed_excess=" + lastChanged, null);
        }
    }

    public void syncNodeList(NodeType nodeType, String query, String server) {
        List<Member> cacheServers = clusterService.getClusterCacheSyncServers() ;

        for(Member cacheServer : cacheServers){
            if(server == null || cacheServer.getAddress().getHost().equals(server)) {
                List<Map<String, Object>> items = ClusterUtils.callNodeList(cacheServer, nodeType.getTypeId(), query);
                if (items != null && items.size() > 0) {
                    for (Map<String, Object> item : items) {
                        Node node = new Node(item);
//                    logger.info("nodeSync : " + item + "\n" + node );

                        if (node != null) {
                            infinispanRepositoryService.cacheNode(node);
                        }
                    }
                    return;
                }
            }
        }
    }

    public void reloadSchema(String resourcePath) throws IOException {
        if(resourcePath.equals("node")){
            saveResourceSchema("classpath:schema/node/**/*.json");
        }else if(resourcePath.equals("test")) {
            saveResourceSchema("classpath:schema/test/**/*.json");
        }else if(!resourcePath.startsWith("/")){
            saveResourceSchema("classpath:schema/" + resourcePath + "/**/*.json");
        }else {
            saveFileSchema(resourcePath);
        }
    }

    private void saveFileSchema(String resourcePath) throws IOException {
        File[] files = new File(resourcePath).listFiles();
        for (File file : files) {
            if (file.getName().equals("nodeType.json")) {
                fileNodeSave(file);
            }
        }

        for (File file : files) {
            if (file.getName().equals("propertyType.json")) {
                fileNodeSave(file);
            }
        }

        for (File file : files) {
            if (file.getName().equals("event.json")) {
                fileNodeSave(file);
            }
        }
        for (File file : files) {
            if (file.getName().endsWith(".json") && !(file.getName().equals("nodeType.json") || file.getName().equals("propertyType.json") || file.getName().equals("event.json"))) {
                fileNodeSave(file);
            }
        }

        for (File file : files) {
            if(file.isDirectory()){
                saveFileSchema(file.getAbsolutePath());
            }
        }
    }

    private void fileNodeSave(File file) throws IOException {
        Collection<Map<String, Object>> nodeDataList = JsonUtils.parsingJsonFileToList(file) ;
        nodeDataList.forEach(data -> nodeService.saveNode(data));
    }


    public void saveResourceSchema(String resourcePath) throws IOException {
        saveSchema(resourcePath, true);
        saveSchema(resourcePath, false);
    }

    private void saveSchema(String resourcePath, boolean core) throws IOException {
        Resource[] resources = ApplicationContextManager.getResources(resourcePath);
        if(core) {
            for (Resource resource : resources) {
                if (resource.getFilename().equals("nodeType.json")) {
                    fileNodeSave(resource, core);
                }
            }

            for (Resource resource : resources) {
                if (resource.getFilename().equals("propertyType.json")) {
                    fileNodeSave(resource, core);
                }
            }

            for (Resource resource : resources) {
                if (resource.getFilename().equals("event.json")) {
                    fileNodeSave(resource, core);
                }
            }
        }else {
            for (Resource resource : resources) {
                if (!(resource.getFilename().equals("nodeType.json") || resource.getFilename().equals("propertyType.json") || resource.getFilename().equals("event.json"))) {
                    fileNodeSave(resource, core);
                }
            }
        }
    }

    private void fileNodeSave(Resource resource, boolean core) throws IOException {
        String fileName = StringUtils.substringBefore(resource.getFilename(), ".json");
        Date last = new Date(resource.lastModified()) ;

        Collection<Map<String, Object>> nodeDataList = JsonUtils.parsingJsonResourceToList(resource) ;


        for(Map<String, Object> data : nodeDataList){
            String typeId = data.get(Node.TYPEID).toString() ;
            if(!lastChangedMap.containsKey(typeId)){
                Date changed = (Date) nodeService.getSortedValue(typeId, "changed", SortField.Type.LONG, true );
                if(changed != null) {
                    lastChangedMap.put(typeId, changed);
                }
            }

            Date changed = lastChangedMap.get(typeId) ;

//            if(core || changed == null || changed.before(last)){
//                data.put("changed", last) ;
            nodeService.saveNode(data) ;
//            }else{
//                logger.info("After last schema : " + typeId);
//            }
        }
    }

    public QueryResult syncNodeQuery(String typeId, String query, String ds) {
        JdbcTemplate jdbcTemplate = dbService.getJdbcTemplate(ds);
        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query);

        Node lastNode = null;
        for (Map<String, Object> data : resultList) {
            lastNode = nodeService.saveNode(data);
        }

        QueryResult queryResult = new QueryResult();
        queryResult.put("result", "200");
        queryResult.put("resultMessage", "SUCCESS");
        queryResult.put("syncSize", resultList.size());
        queryResult.put("lastNode", lastNode);
        return queryResult;
    }

    public QueryResult syncNodeBinding(String typeId, String id, Integer limit, Integer count) {
        NodeType nodeType = nodeService.getNodeType(typeId);
        List<String> ids = nodeType.getIdablePIds();
        if (limit == null || limit == 0) {
            limit = 100;
        }
        if (count == null || count == 0) {
            count = 1000;
        }

        String query = String.format("sorting=%s desc&limit=%d&%s_under=", ids.get(0), limit, ids.get(0));

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo(nodeType.getTypeId());
        QueryResult queryResult = new QueryResult();
        int totalCount = 0;
        for (int i = 0; i < count; i++) {
            long start = System.currentTimeMillis();
            QueryContext queryContext = QueryContext.createQueryContextFromText(query + id, nodeType, null);
            List<Map<String, Object>> resultList = nodeBindingInfo.list(queryContext);
            totalCount += resultList.size();
            Node lastNode = null;
            for (Map<String, Object> data : resultList) {
                data.put("typeId", typeId);
                lastNode = nodeService.saveNode(data);
            }
            if (lastNode == null) {
                logger.info("sync node biding : {}, lastId = {}, limit = {}, size = {}, roofCount = {}, time = {}",
                        typeId, id, limit, count, queryResult.size(), System.currentTimeMillis() - start);
                break;
            }
            id = lastNode.getId();
            logger.info("sync node biding : {}, lastId = {}, limit = {}, size = {}, time = {}",
                    typeId, lastNode.getId(), limit, count, System.currentTimeMillis() - start);
            if (resultList.size() == 0) {
                break;
            }
        }
        queryResult.put("result", "200");
        queryResult.put("resultMessage", "SUCCESS");
        queryResult.put("syncSize", totalCount);
        queryResult.put("lastId", id);
        return queryResult;

    }
}
