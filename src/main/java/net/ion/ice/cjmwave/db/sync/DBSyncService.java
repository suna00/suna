package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.cjmwave.db.sync.utils.NodeMappingUtils;
import net.ion.ice.cjmwave.db.sync.utils.SyntaxUtils;
import net.ion.ice.cjmwave.external.utils.MigrationUtils;
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
import java.util.Date;
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


    /*
    * 결과가 안나올 때까지 이터레이션하면서 처리한다, 쿼리에 반드시 limit @{start} @{unit} 있어야 한다
    * start unit 외 다른 파라미터는 받을 수 없음
    * 실패에 대한 처리를 어떻게 할 것인지
    * */
    public void executeWithIteration (String executeId) throws Exception {
        boolean loop = true;
        int i = 0;
        int unit = 100;
        int successCnt = 0;
        int skippedCnt = 0;
        Date startTime = new Date();
        String failPolicy = "SKIP";
        String targetNodeType = null;
        JdbcTemplate template = null;


        while(loop) {
            // i 가 0 부터 99 까지
            // 100 부터 199 까지

            int start = i * 100;

            Object dbSyncMetaInfo = nodeService.readNode(null, PROCESS_TID, executeId);
            if (dbSyncMetaInfo == null) throw new Exception("[ " + executeId + " ] does not exists");
            Map itemMap = (Map) ((Map) dbSyncMetaInfo).get("item");
            String query = String.valueOf(itemMap.get("query"));
            targetNodeType = String.valueOf(itemMap.get("targetNodeType"));
            String targetDs = String.valueOf(itemMap.get("targetDs"));
            failPolicy = String.valueOf(itemMap.get("onFail")).trim().toUpperCase();
            failPolicy = (!"NULL".equals(failPolicy) && "STOP".equals(failPolicy)) ? "STOP" : "SKIP";


            // 쿼리
            Map<String, Object> jdbcParam = SyntaxUtils.parseWithLimit(query, start, unit);
            String jdbcQuery = String.valueOf(jdbcParam.get("query"));

            Object[] params = (Object[]) jdbcParam.get("params");

            template = dbService.getJdbcTemplate(targetDs);
            List<Map<String, Object>> queryRs = template.queryForList(jdbcQuery, params);

            if (queryRs == null || queryRs.isEmpty()) {
                loop = false;
                continue;
            } else {
                List<Node> mapperInfoList = NodeUtils.getNodeList(MAPPER_TID, "executeId_matching=" + executeId);
                Map<String, String> mapperStore = NodeMappingUtils.extractPropertyColumnMap(mapperInfoList);

                for (Map qMap : queryRs) {
                    // mapping 정보에 맞게 변경
                    int rs = 0;

                    Map<String, Object> fit = NodeMappingUtils.mapData(targetNodeType, qMap, mapperStore);
                    logger.info("CREATE INITIAL MIGRATION NODE :: " + String.valueOf(fit));
                    try{
                        nodeService.saveNodeWithException(fit);
                        successCnt ++;
                        rs = 1;
                    } catch (Exception e) {
                        // 실패한다면 실패 기록을 DB 에 저장한다.
                        logger.error("Recording exception :: ", e);
                        if(failPolicy.equals("STOP")){
                            loop = false;
                        } else {
                            skippedCnt++;
                        }
                    }
                    MigrationUtils.recordSingleDate(template, targetNodeType, String.valueOf(fit), rs);
                }
                i++;
            }
        }

        long jobTaken = (new Date().getTime() - startTime.getTime());
        MigrationUtils.printReport(startTime, executeId, failPolicy, successCnt, skippedCnt);
        MigrationUtils.recordResult(template, "MNET", "INIT", null, null
                ,targetNodeType, successCnt, skippedCnt, jobTaken, startTime);
    }


    public void executeForNewData (String mig_target, String executeId) throws Exception {
        // last Execution 시간은 MIG_DATA_HISTORY 에서 찾을 수 있음
        // MSSQL 펑션이 있다고 생각하고 파라미터로 마지막 날짜를 던져서 노드 생성하면 됨

    }
}