package net.ion.ice.core.data.bind;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.DBUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.data.context.DBQueryContext;
import net.ion.ice.core.data.context.DBQueryTerm;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
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
        nodeBindProcess(typeId);

        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        int callback = nodeBindingInfo.update(parameterMap);
        if (callback == 0) {
            nodeBindingInfo.insert(parameterMap);
        }
    }

    public void execute(ExecuteContext context) {
        Node node = context.getNode();
        nodeBindProcess(node.getTypeId());
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(node.getTypeId());
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

    public Map<String, Object> read(String typeId, String id) throws JsonProcessingException {
        nodeBindProcess(typeId);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        return nodeBindingInfo.retrieve(id);
    }

    public Map<String, Object> read(Map<String, String[]> parameterMap, String typeId) throws JsonProcessingException {
        nodeBindProcess(typeId);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);

        String id = "";

        for (String paramName : parameterMap.keySet()) {
            if (paramName.equals("id")) {
                id = parameterMap.get(paramName)[0];
            }
        }

        if (id.isEmpty()) {
            List<String> idablePids = NodeUtils.getNodeType(typeId).getIdablePIds();
            for (int i = 0; i < idablePids.size(); i++) {
                id = id + parameterMap.get(idablePids.get(i))[0] + (i < (idablePids.size() - 1) ? Node.ID_SEPERATOR : "");
            }
        }


        return nodeBindingInfo.retrieve(id);
    }

    public Map<String, Object> list(String typeId) {
        nodeBindProcess(typeId);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        return nodeBindingInfo.list();
    }

    public Map<String, Object> list(String typeId, WebRequest request) {
        nodeBindProcess(typeId);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        if(request.getParameterMap().isEmpty()){
            return nodeBindingInfo.list();
        }else{
            return nodeBindingInfo.list(DBQueryContext.makeDBQueryContextFromParameter(request.getParameterMap(), nodeService.getNodeType(typeId)));
        }
    }

    public void delete(Map<String, String[]> parameterMap, String typeId) {
        nodeBindProcess(typeId);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        nodeBindingInfo.delete(parameterMap);
    }

    public void delete(ExecuteContext context) {
        Node node = context.getNode();
        nodeBindProcess(node.getTypeId());
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(node.getTypeId());
        nodeBindingInfo.delete(node);
    }


    public void nodeBindProcess(String typeId) {
        NodeType nodeType = nodeService.getNodeType(typeId);

        if (!nodeBindingInfoMap.containsKey(typeId)) {

            String dsId = String.valueOf(nodeType.getTableName()).split("#")[0];
            JdbcTemplate jdbcTemplate = DBService.getJdbcTemplate(dsId);

            String tableName = String.valueOf(nodeType.getTableName()).split("#")[1];

            String DBType = new DBUtils(jdbcTemplate).getDBType();

            NodeBindingInfo nodeBindingInfo = new NodeBindingInfo(nodeType, jdbcTemplate, tableName, DBType);
            nodeBindingInfo.makeDefaultQuery();

            nodeBindingInfoMap.put(typeId, nodeBindingInfo);
        }
    }
}
