package net.ion.ice.core.data.bind;

import net.ion.ice.core.data.DatabaseService;
import net.ion.ice.core.data.table.Column;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.PropertyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
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

    public void save(Map<String, String[]> parameterMap, String tid) {
        if (!nodeBindingInfoMap.containsKey(tid)) {
            NodeType nodeType = nodeService.getNodeType(tid);

            String dsId = String.valueOf(nodeType.getTableName()).split("#")[0];
            JdbcTemplate jdbcTemplate = databaseService.getJdbcTemplate(dsId);

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

//    public void createTable(String tid, HttpServletResponse response){
//        NodeType nodeType = nodeService.getNodeType(tid);
//        List<Column> createColumsList = new LinkedList<>();
//        for (PropertyType propertyType : nodeType.getPropertyTypes()) {
//            String columnName = propertyType.getPid();
//            Boolean isPk;
//            String dataType;
//            Integer dataLength;
//            Boolean isNullable;
//            Integer sqlType;
//            String dataDefault;
//
//            String columnName = propertyType.getPid();
//
//            if(propertyType.isIdable()){
//                isPk = true;
//            }
//
//            if ()
//            Column column = new Column();
//
//        }
//        String dsId = String.valueOf(nodeType.getTableName()).split("#")[0];
//        JdbcTemplate jdbcTemplate = databaseService.getJdbcTemplate(dsId);
//
//        NodeBindingInfo nodeBindingInfo = new NodeBindingInfo(nodeType, jdbcTemplate);
//        nodeBindingInfo.init();
//    }
}
