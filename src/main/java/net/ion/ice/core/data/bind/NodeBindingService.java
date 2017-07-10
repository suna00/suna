package net.ion.ice.core.data.bind;

import net.ion.ice.core.data.DBUtils;
import net.ion.ice.core.data.DatabaseService;
import net.ion.ice.core.data.table.TableMeta;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by seonwoong on 2017. 6. 28..
 */
@Service
public class NodeBindingService {
    @Autowired
    private NodeService nodeService;
    @Autowired
    private DatabaseService databaseService;

    private Map<String, NodeBindingInfo> nodeBindingInfoMap = new ConcurrentHashMap<>();
    private Map<String, TableMeta> tableMetaMap = new ConcurrentHashMap<>();

    public void save(Map<String, String[]> parameterMap, String tid) {
        if (!nodeBindingInfoMap.containsKey(tid)) {
            NodeType nodeType = nodeService.getNodeType(tid);
            String dsId = String.valueOf(nodeType.getTableName()).split("#")[0];
            String tableName = String.valueOf(nodeType.getTableName()).split("#")[1];
            JdbcTemplate jdbcTemplate = databaseService.getJdbcTemplate(dsId);

            DBUtils dbUtils = new DBUtils(jdbcTemplate);
            String DBType = dbUtils.getDBType();

            TableMeta tableMeta = new TableMeta(tableName, DBType);

            NodeBindingInfo nodeBindingInfo = new NodeBindingInfo(nodeType, jdbcTemplate, tableMeta);
            nodeBindingInfo.init();

            tableMetaMap.put(tid, tableMeta);
            nodeBindingInfoMap.put(tid, nodeBindingInfo);
        }

        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(tid);
        int callback = nodeBindingInfo.update(parameterMap);
        if (callback == 0) {
            nodeBindingInfo.insert(parameterMap);
        }
    }

    public void createTable(String tid, HttpServletResponse response) {

        if (!nodeBindingInfoMap.containsKey(tid)) {
            NodeType nodeType = nodeService.getNodeType(tid);
            String dsId = String.valueOf(nodeType.getTableName()).split("#")[0];
            String tableName = String.valueOf(nodeType.getTableName()).split("#")[1];
            JdbcTemplate jdbcTemplate = databaseService.getJdbcTemplate(dsId);

            DBUtils dbUtils = new DBUtils(jdbcTemplate);
            String DBType = dbUtils.getDBType();

            TableMeta tableMeta = new TableMeta(tableName, DBType);

            NodeBindingInfo nodeBindingInfo = new NodeBindingInfo(nodeType, jdbcTemplate, tableMeta);
            nodeBindingInfo.init();

            tableMetaMap.put(tid, tableMeta);
            nodeBindingInfoMap.put(tid, nodeBindingInfo);
        }

        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(tid);
        nodeBindingInfo.create();
    }
}
