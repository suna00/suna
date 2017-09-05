package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.cjmwave.db.sync.utils.NodeMappingUtils;
import net.ion.ice.cjmwave.db.sync.utils.SyntaxUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 5..
 * 서비스에서 서비스를 Autowired
 */
@Service
public class DBSyncService {

    private Logger logger = Logger.getLogger(DBSyncService.class);

    private final String PROCESS_TID = "dbSyncProcess"
            , MAPPER_TID = "dbSyncMapper"
            , HISTORY_TID = "dbSyncHistory";


    @Autowired
    NodeService nodeService;

    @Autowired
    DBService dbService;

    /*
    * executeId 로 dbSyncProcess 노드를 뒤져서
    * DBService 로 원격 디비를 조회하고
    * NodeService 로 노드를 생성한다
    *
    * Transactional 이 필요할까?
    * Transactional 하지 않는다면 실패시 실패에 대한 기록을 쌓아 복구할 수 있는 인터페이스가 필요함
    * */
    @Transactional
    public List<Map> executeJob(String executeId, HttpServletRequest request) throws Exception {

        logger.info("Start executing database Sync process with Id [ " + executeId + " ]");
        List<Map> altered = new ArrayList<>();


        //dbSyncProcess Node 에서 가져오기
        Object dbSyncMetaInfo = nodeService.readNode(null, PROCESS_TID, executeId);
        if(dbSyncMetaInfo == null) throw new Exception("[ " + executeId + " ] does not exists");
        Map itemMap = (Map) ((Map) dbSyncMetaInfo).get("item");
        String query = String.valueOf(itemMap.get("query"));
        String targetNodeType = String.valueOf(itemMap.get("targetNodeType"));
        String targetDs = String.valueOf(itemMap.get("targetDs"));

        // 쿼리
        Map<String, Object> jdbcParam = SyntaxUtils.parse(query, request);
        String jdbcQuery = String.valueOf(jdbcParam.get("query"));
        Object[] params = (Object[]) jdbcParam.get("params");

        JdbcTemplate template = dbService.getJdbcTemplate(targetDs);
        List<Map<String, Object>> queryRs = template.queryForList(jdbcQuery, params);

        // mapper 정보 추출
        List<Node> mapperInfoList = NodeUtils.getNodeList(MAPPER_TID, "executeId_matching=" + executeId);
        Map<String, String> mapperStore = NodeMappingUtils.extractPropertyColumnMap(mapperInfoList);

        for(Map qMap : queryRs) {
            // mapping 정보에 맞게 변경
            Map<String, Object> fit = NodeMappingUtils.mapData(targetNodeType, qMap, mapperStore);
            nodeService.saveNode(fit);
            altered.add(fit);
        }


        return altered;
    }
}