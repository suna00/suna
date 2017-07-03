package net.ion.ice.core.data.bind;

import net.ion.ice.core.data.DatabaseServiceIm;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by seonwoong on 2017. 6. 28..
 */
@Service("nodeBindingService")
public class NodeBindingService {
    @Autowired
    private NodeService nodeService;
    @Autowired
    private DatabaseServiceIm databaseServiceIm;

    private Map<String, NodeBindingInfo> nodeBindingInfoMap = new ConcurrentHashMap<>();

    public void save(Map<String, String[]> parameterMap, String tid) {
        if (!nodeBindingInfoMap.containsKey(tid)) {
            NodeType nodeType = nodeService.getNodeType(tid);
            String dsId = String.valueOf(nodeType.getTableName()).split("#")[0];
            JdbcTemplate jdbcTemplate = databaseServiceIm.getJdbcTemplate(dsId);

            NodeBindingInfo nodeBindingInfo = new NodeBindingInfo(nodeType, jdbcTemplate);

            nodeBindingInfo.init();
            nodeBindingInfoMap.put(tid, nodeBindingInfo);
        }
        NodeBindingInfo nodeBindingInfo = nodeBindingInfoMap.get(tid);
        int callback = nodeBindingInfo.update(parameterMap);
        if (callback == 0) {
            nodeBindingInfo.create(parameterMap);
        }
    }
}
