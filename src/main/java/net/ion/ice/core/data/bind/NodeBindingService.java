package net.ion.ice.core.data.bind;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ion.ice.core.data.DBUtils;
import net.ion.ice.core.data.DatabaseController;
import net.ion.ice.core.data.DatabaseService;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
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
@Service
public class NodeBindingService {
    private static Logger logger = LoggerFactory.getLogger(NodeBindingService.class);

    @Autowired
    private NodeService nodeService;
    @Autowired
    private DatabaseService databaseService;
    private Map<String, NodeBindingInfo> nodeBindingInfoMap = new ConcurrentHashMap<>();

    public void save(Map<String, String[]> parameterMap, String typeId) {
        nodeBindProcess(typeId);

        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        int callback = nodeBindingInfo.update(parameterMap);
        if (callback == 0) {
            nodeBindingInfo.insert(parameterMap);
        }
    }

    public void createTable(String typeId, HttpServletResponse response) {

        nodeBindProcess(typeId);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        nodeBindingInfo.create();
    }

    public Map<String, Object> read(WebRequest request, String typeId, String id) throws JsonProcessingException {
        nodeBindProcess(typeId);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(typeId);
        return nodeBindingInfo.retrieve(id);
    }

    public List<Map<String, Object>> list(String tid) {
        nodeBindProcess(tid);
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(tid);
        return nodeBindingInfo.list();
    }


    public void nodeBindProcess(String typeId) {
        NodeType nodeType = nodeService.getNodeType(typeId);

        if (!nodeBindingInfoMap.containsKey(typeId)) {

            String dsId = String.valueOf(nodeType.getTableName()).split("#")[0];
            JdbcTemplate jdbcTemplate = databaseService.getJdbcTemplate(dsId);

            String tableName = String.valueOf(nodeType.getTableName()).split("#")[1];

            String DBType = new DBUtils(jdbcTemplate).getDBType();

            NodeBindingInfo nodeBindingInfo = new NodeBindingInfo(nodeType, jdbcTemplate, tableName, DBType);
            nodeBindingInfo.init();

            nodeBindingInfoMap.put(typeId, nodeBindingInfo);
        }
    }

}
