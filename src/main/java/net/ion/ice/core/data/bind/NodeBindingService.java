package net.ion.ice.core.data.bind;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ion.ice.core.context.*;
import net.ion.ice.core.data.DBUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by seonwoong on 2017. 6. 28..
 */
@Service("nodeBindingService")
public class NodeBindingService {
    private static Logger logger = LoggerFactory.getLogger(NodeBindingService.class);

    @Autowired
    private NodeService nodeService;
    @Autowired
    private DBService DBService;
    private Map<String, NodeBindingInfo> nodeBindingInfoMap = new ConcurrentHashMap<>();

    public void save(Map<String, String[]> parameterMap, String typeId) {
        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);

        int callback = nodeBindingInfo.update(parameterMap);
        if (callback == 0) {
            nodeBindingInfo.insert(parameterMap);
        }
    }

    public void execute(ExecuteContext context) {
        Node node = context.getNode();
        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(node.getTypeId());

        int callback = nodeBindingInfo.update(node);
        if (callback == 0) {
            nodeBindingInfo.insert(node);
        }
        logger.info("Node Binding {} - {} :  " + (callback == 0 ? "insert" : "update"), node.getTypeId(), node.getId());
    }

    public void createTable(String typeId, HttpServletResponse response) {
        nodeBindProcess(typeId);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        nodeBindingInfo.create();
    }

    public QueryResult read(Map<String, String[]> parameterMap, String typeId, String id) throws JsonProcessingException {
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;

        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);

        DataReadContext readContext = DataReadContext.createContextFromParameter(nodeBindingInfo, parameterMap, nodeType, id) ;
        return readContext.makeResult() ;
    }

    public QueryResult read(Map<String, String[]> parameterMap, String typeId) throws JsonProcessingException {
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;

        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);

        DataReadContext readContext = DataReadContext.createContextFromParameter(nodeBindingInfo, parameterMap, nodeType, null) ;
        return readContext.makeResult() ;
    }


    public NodeBindingInfo getNodeBindingInfo(String typeId){
        nodeBindProcess(typeId);
        return nodeBindingInfoMap.get(typeId);
    }

    public Map<String, Object> list(String typeId) {
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;

        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);

        DataQueryContext queryContext = DataQueryContext.createQueryContextFromParameter(nodeBindingInfo, null, nodeType) ;
        QueryResult queryResult = queryContext.makeQueryResult( null, null);
        return queryResult;
    }

    public Map<String, Object> list(String typeId, WebRequest request) {
        NodeType nodeType = NodeUtils.getNodeType(typeId) ;

        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);

        DataQueryContext queryContext = DataQueryContext.createQueryContextFromParameter(nodeBindingInfo, request.getParameterMap(), nodeType) ;
        QueryResult queryResult = queryContext.makeQueryResult( null, null);
        return queryResult;
    }

    public void delete(Map<String, String[]> parameterMap, String typeId) {
        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);
        nodeBindingInfo.delete(parameterMap);
    }

    public void delete(ExecuteContext context) {
        Node node = context.getNode();
        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(node.getTypeId());
        nodeBindingInfo.delete(node);
    }


    public void nodeBindProcess(String typeId) {
        NodeType nodeType = nodeService.getNodeType(typeId);
        nodeBindProcess(nodeType);
    }

    public void nodeBindProcess(NodeType nodeType) {
        if (!nodeBindingInfoMap.containsKey(nodeType.getTypeId())) {

            String dsId = String.valueOf(nodeType.getTableName()).split("#")[0];
            JdbcTemplate jdbcTemplate = DBService.getJdbcTemplate(dsId);

            String tableName = String.valueOf(nodeType.getTableName()).split("#")[1];

            String DBType = new DBUtils(jdbcTemplate).getDBType();

            NodeBindingInfo nodeBindingInfo = new NodeBindingInfo(nodeType, jdbcTemplate, tableName, DBType);
            nodeBindingInfo.makeDefaultQuery();

            nodeBindingInfoMap.put(nodeType.getTypeId(), nodeBindingInfo);
        }
    }

}
