package net.ion.ice.core.data.bind;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.DataQueryContext;
import net.ion.ice.core.context.DataReadContext;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.data.DBUtils;
import net.ion.ice.core.node.*;
import net.ion.ice.core.query.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
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
        Map<String, String[]> param = new HashMap<>(parameterMap);
        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);
        List<PropertyType> idAblePidList = NodeUtils.getNodeType(typeId).getIdablePropertyTypes();
        String[] value = new String[1];
        if (idAblePidList.size() > 0) {
            for (PropertyType idAblePid : idAblePidList) {
                if (idAblePid.getIdType().equals(PropertyType.IdType.autoIncrement)) {
                    if (param.containsKey(idAblePid.getPid())) {
                        int callback = nodeBindingInfo.update(param);
                        Long sequence = Long.valueOf(param.get(idAblePid.getPid())[0]);
                        if (callback == 0) {
                            sequence = NodeUtils.getSequenceValue(typeId);
                            value[0] = String.valueOf(sequence);
                            param.put(idAblePid.getPid(), value);
                            nodeBindingInfo.insert(param);

                        }
                        logger.info("Node Binding {} - {} : " + (callback == 0 ? "insert" : "update"), typeId, sequence);

                    } else {
                        Long sequence = NodeUtils.getSequenceValue(typeId);
                        value[0] = String.valueOf(sequence);
                        param.put(idAblePid.getPid(), value);
                        nodeBindingInfo.insert(param);
                        logger.info("Node Binding {} - {} : insert ", typeId, sequence);
                    }
                } else {
                    int callback = nodeBindingInfo.update(param);
                    if (callback == 0) {
                        nodeBindingInfo.insert(param);
                        logger.info("Node Binding {} - {} : " + (callback == 0 ? "insert" : "update"), typeId);
                    }
                }
            }
        }
    }

    public void execute(ExecuteContext context) {
        Node node = context.getNode();
        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(node.getTypeId());

        if(nodeBindingInfo == null) {
            logger.error("Node Binding Error : " +  node.getTypeId());
            throw new IceRuntimeException("Node Binding Error : " +  node.getTypeId()) ;
        }
        int callback = 0;
        try {
            callback = nodeBindingInfo.update(node);
            if (callback == 0) {
                nodeBindingInfo.insert(node);
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new IceRuntimeException("Node Binding Execute Error : " +  e.getMessage(), e) ;
        }
        context.setResult(node);
        logger.info("Node Binding {} - {} - {} :  " + (callback == 0 ? "insert" : "update"), node.getTypeId(), node.getId(), context.getEvent());
    }

    public void createTable(String typeId, HttpServletResponse response) {
        nodeBindProcess(typeId);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        nodeBindingInfo.create();
    }

    public QueryResult read(Map<String, String[]> parameterMap, String typeId, String id) throws JsonProcessingException {
        NodeType nodeType = NodeUtils.getNodeType(typeId);

        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);

        DataReadContext readContext = DataReadContext.createContextFromParameter(nodeBindingInfo, parameterMap, nodeType, id);
        return readContext.makeResult();
    }

    public QueryResult read(Map<String, String[]> parameterMap, String typeId) throws JsonProcessingException {
        NodeType nodeType = NodeUtils.getNodeType(typeId);

        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);

        DataReadContext readContext = DataReadContext.createContextFromParameter(nodeBindingInfo, parameterMap, nodeType, null);
        return readContext.makeResult();
    }


    public NodeBindingInfo getNodeBindingInfo(String typeId) {
        nodeBindProcess(typeId);
        return nodeBindingInfoMap.get(typeId);
    }

    public Map<String, Object> list(String typeId) {
        NodeType nodeType = NodeUtils.getNodeType(typeId);

        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);

        DataQueryContext queryContext = DataQueryContext.createQueryContextFromParameter(nodeBindingInfo, null, nodeType);
        QueryResult queryResult = queryContext.makeQueryResult();
        return queryResult;
    }

    public Map<String, Object> list(String typeId, WebRequest request) {
        NodeType nodeType = NodeUtils.getNodeType(typeId);

        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);

        DataQueryContext queryContext = DataQueryContext.createQueryContextFromParameter(nodeBindingInfo, request.getParameterMap(), nodeType);
        QueryResult queryResult = queryContext.makeQueryResult();
        return queryResult;
    }

    public List<Map<String, Object>> list(String tid, String searchText) {
        NodeType nodeType = NodeUtils.getNodeType(tid);
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType, null);
        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(tid);
        return nodeBindingInfo.list(queryContext);
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

    public void delete(String typeId, String id) {
        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);
        nodeBindingInfo.delete(id);
    }

    public Long sequence(String typeId) {
        NodeBindingInfo nodeBindingInfo = getNodeBindingInfo(typeId);
        return nodeBindingInfo.retrieveSequence();
    }


    public void nodeBindProcess(String typeId) {
        NodeType nodeType = nodeService.getNodeType(typeId);
        nodeBindProcess(nodeType);
    }

    public void nodeBindProcess(NodeType nodeType) {
        if (!nodeBindingInfoMap.containsKey(nodeType.getTypeId())) {

            String dsId = nodeType.getDsId();
            String tableName = nodeType.getTableName();

            JdbcTemplate jdbcTemplate = DBService.getJdbcTemplate(dsId);


            String DBType = new DBUtils(jdbcTemplate).getDBType();

            NodeBindingInfo nodeBindingInfo = new NodeBindingInfo(nodeType, jdbcTemplate, tableName, DBType);
            nodeBindingInfo.makeDefaultQuery();

            nodeBindingInfoMap.put(nodeType.getTypeId(), nodeBindingInfo);
        }
    }

}
