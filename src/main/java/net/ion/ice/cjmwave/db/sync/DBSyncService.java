package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.IceRuntimeException;
import net.ion.ice.cjmwave.db.sync.utils.NodeMappingUtils;
import net.ion.ice.cjmwave.db.sync.utils.SyntaxUtils;
import net.ion.ice.cjmwave.external.utils.MigrationUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

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

//    @Autowired
//    S3UploadService s3UploadService;

    @Value("${file.default.path}")
    String defaultFilePath;

    private JdbcTemplate ice2Template;

    @PostConstruct
    public void init(){
        try{
            ice2Template = dbService.getJdbcTemplate("cjDb");
        } catch (Exception e) {
            logger.error("Could not initialize JdbcTemplate");
        }
    }

    private Map<String, Object> getMultiLanguageInfo (Map<String, Object> queryMap, String multiLangQuery, String foreignKey) {
        Map<String, Object> additional = new HashMap<String, Object>();
        try{
            if(foreignKey != null) {
                // 다국어 사용하는 쿼리 노드라면 추가쿼리를 해서 노드 정보에 다국어 정보를 추가한다.
                Map<String, Object> idMap = new HashMap<>();
                idMap.put("id", queryMap.get(foreignKey));
                Map<String, Object> jdbcParam = SyntaxUtils.parse(multiLangQuery, idMap);
                String jdbcQuery = String.valueOf(jdbcParam.get("query"));
                Object[] params = (Object[]) jdbcParam.get("params");

                List<Map<String, Object>> multiLanguageInformation = ice2Template.queryForList(jdbcQuery, params);
                // langcd 랑 뭐 이것저것 들었다고 치자
                for(Map<String, Object> singleLangMap : multiLanguageInformation) {
                    String langCd = String.valueOf(singleLangMap.get("langCd"));
                    Iterator<String> multiIter = singleLangMap.keySet().iterator();
                    while(multiIter.hasNext()) {
                        String k = multiIter.next();
                        Object v = singleLangMap.get(k);
                        if(!k.equals("langCd")) {
                            additional.put(k + "_" + langCd.toLowerCase(), v);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to set multi language information, but consider it as normal", e);
        }
        return additional;
    }


    // 복수 키 수용 못함
    private Map<String, Object> getImageInfo(Map<String, Object> queryMap, String targetNodeTypeId, String nodePKPid) {
        Map<String, Object> imageValues = new HashMap<String, Object>();
        if(targetNodeTypeId.equals("album")
                || targetNodeTypeId.equals("artist")){
            try {
                if(queryMap.containsKey(nodePKPid)){
                    String nodePKValue = String.valueOf(queryMap.get(nodePKPid));
                    String mnetFileUrl =
                            MigrationUtils.getMnetFileUrl(nodePKValue
                                    , targetNodeTypeId, targetNodeTypeId.equals("album") ? "360" : "320");
//                    File physicalFile = MigrationUtils.retrieveRemoteFile(defaultFilePath + "/mnet", mnetFileUrl);
//                    String s3Path = "";
//                    if(physicalFile != null) s3Path = s3UploadService.uploadToS3(targetNodeTypeId, physicalFile);
//                    logger.info("s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3");
//                    logger.info("s3 :: " + s3Path);
//                    logger.info("s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3 s3");
//                    imageValues.put("imgUrl", MigrationUtils.retrieveRemoteFile(defaultFilePath + "/mnet", mnetFileUrl));
                    imageValues.put("imgUrl", mnetFileUrl);
                }
            } catch (Exception e) {
                logger.error("Failed to Load Image ... ", e);
            }
        }
        return imageValues;
    }


    private List<String> executeSingleTaskAndRecord (Node executionNode, List<Map<String, Object>> queryRs, String mig_target, String mig_type) throws Exception {
        List<String> successIds = new ArrayList<>();
        int successCnt = 0;
        int skippedCnt = 0;
        Date startTime = new Date();
        String executeId = executionNode.getId();
        String targetNodeType = String.valueOf(executionNode.get("targetNodeType"));
        String failPolicy = String.valueOf(executionNode.get("onFail")).trim().toUpperCase();
        failPolicy = (!"NULL".equals(failPolicy) && "STOP".equals(failPolicy)) ? "STOP" : "SKIP";

        List<Node> mapperInfoList = NodeUtils.getNodeList(MAPPER_TID, "executeId_matching=" + executeId);
        Map<String, String> mapperStore = NodeMappingUtils.extractPropertyColumnMap(mapperInfoList);

        /*
            다국어 처리가 여기로 변경되어야 함
            {pid}_{langCd}
        * */
        final String MLANG_PID = "multiLanguageQuery";
        boolean useMultiLanguage = executionNode.containsKey(MLANG_PID) && executionNode.get(MLANG_PID) != null;
        String multiLangQuery = null;
        String currentNodePKPid = NodeMappingUtils.retrieveNodePrimaryKey(nodeService, targetNodeType);
        if(useMultiLanguage) {
            multiLangQuery = String.valueOf(executionNode.get("multiLanguageQuery"));
        }


        for (Map qMap : queryRs) {
            int rs = 0;

            Map<String, Object> fit = NodeMappingUtils.mapData(targetNodeType, qMap, mapperStore);
            // 다국어 처리
            try{
                if(useMultiLanguage) {
                    fit.putAll(getMultiLanguageInfo(qMap, multiLangQuery, currentNodePKPid));
                }
            } catch (Exception e) {
                logger.error("Failed to set multi language information, but consider it as normal");
            }
            fit.putAll(getImageInfo(qMap,targetNodeType, currentNodePKPid));


            logger.info("CREATE MIGRATION NODE :: " + String.valueOf(fit));
            try{
                Node finished = nodeService.saveNodeWithException(fit);
                successIds.add(finished.getId());
                successCnt ++;
                rs = 1;
            } catch (Exception e) {
                // 실패한다면 실패 기록을 DB 에 저장한다.
                logger.error("Recording exception :: ", e);
                if(failPolicy.equals("STOP")){
                    break;
                } else {
                    skippedCnt++;
                    if(e instanceof IceRuntimeException) {
                        MigrationUtils.saveFilureNodes2(ice2Template, ((IceRuntimeException) e).getRootCause().getClass().getName(), fit);
                    } else {
                        MigrationUtils.saveFilureNodes2(ice2Template, e.getClass().getName(), fit);
                    }
                }
            }
            // 실패 이외에 성공도 기록하고 싶으면 주석을 푸시오.
            //MigrationUtils.recordSingleDate(ice2Template, targetNodeType, String.valueOf(fit), rs); // 레코드에 대한 결과
        }

        long jobTaken = (new Date().getTime() - startTime.getTime());
        MigrationUtils.printReport(startTime, executeId, failPolicy, successCnt, skippedCnt);
        MigrationUtils.recordResult(ice2Template, mig_target, mig_type, null, null
                ,targetNodeType, successCnt, skippedCnt, jobTaken, startTime);
        return  successIds;
    }


    /*
    * 결과가 안나올 때까지 이터레이션하면서 처리한다, 쿼리에 반드시 limit @{start} @{unit} 있어야 한다
    * start unit 외 다른 파라미터는 받을 수 없음
    * 실패에 대한 처리를 어떻게 할 것인지
    * */
    public void executeWithIteration (String executeId) throws Exception {
//        int max = 1;
        logger.info("DBSyncService.executeWithIteration :: " + executeId);
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
            // TEST
//            if(start > max) {
//                loop = false;
//                continue;
//            }

            Node executionNode = nodeService.read(PROCESS_TID, executeId);
            if (executionNode == null) throw new Exception("[ " + executeId + " ] does not exists");
            String query = String.valueOf(executionNode.get("query"));
            targetNodeType = String.valueOf(executionNode.get("targetNodeType"));
            String targetDs = String.valueOf(executionNode.get("targetDs"));
            failPolicy = String.valueOf(executionNode.get("onFail")).trim().toUpperCase();
            failPolicy = (!"NULL".equals(failPolicy) && "STOP".equals(failPolicy)) ? "STOP" : "SKIP";



            /*
            다국어 처리가 여기로 변경되어야 함
            {pid}_{langCd}
            * */
            final String MLANG_PID = "multiLanguageQuery";
            boolean useMultiLanguage = executionNode.containsKey(MLANG_PID) && executionNode.get(MLANG_PID) != null;
            String multiLangQuery = null;
            String currentNodePKPid = NodeMappingUtils.retrieveNodePrimaryKey(nodeService, targetNodeType);
            if(useMultiLanguage) {
                multiLangQuery = String.valueOf(executionNode.get("multiLanguageQuery"));
            }

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
                    if(useMultiLanguage) {
                        fit.putAll(getMultiLanguageInfo(qMap, multiLangQuery, currentNodePKPid));
                    }
                    fit.putAll(getImageInfo(qMap,targetNodeType, currentNodePKPid));
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
                        if(e instanceof IceRuntimeException) {
                            MigrationUtils.saveFilureNodes2(ice2Template, ((IceRuntimeException) e).getRootCause().getClass().getName(), fit);
                        } else {
                            MigrationUtils.saveFilureNodes2(ice2Template, e.getClass().getName(), fit);
                        }
                    }
                    // 실패 이외에 성공도 기록하고 싶으면 주석을 푸시오.
                    //MigrationUtils.recordSingleDate(template, targetNodeType, String.valueOf(fit), rs);
                }
                i++;
            }
        }

        long jobTaken = (new Date().getTime() - startTime.getTime());
        MigrationUtils.printReport(startTime, executeId, failPolicy, successCnt, skippedCnt);
        MigrationUtils.recordResult(template, "MNET", "INIT", null, null
                ,targetNodeType, successCnt, skippedCnt, jobTaken, startTime);
    }


    public void executeForNewData (String mig_target, String executeId, Date provided) throws Exception {
        // last Execution 시간은 MIG_DATA_HISTORY 에서 찾을 수 있음
        // MSSQL 펑션이 있다고 생각하고 파라미터로 마지막 날짜를 던져서 노드 생성하면 됨
        String queryForLastExecution =
                "SELECT execution_date as lastUpdated "
                        + "FROM MIG_HISTORY "
                        + "WHERE mig_target = ? AND target_node = ? AND mig_type='SCHEDULE' "
                        + "ORDER BY execution_date DESC LIMIT 1";

        Node executionNode = nodeService.getNode(PROCESS_TID, executeId);
        String targetNodeType = String.valueOf(executionNode.get("targetNodeType"));
        String query = String.valueOf(executionNode.get("query"));

        Map<String, Object> lastExecutionRs = null;
        if(provided == null) {
            try {
                lastExecutionRs = ice2Template.queryForMap(queryForLastExecution, mig_target, targetNodeType);
            } catch (EmptyResultDataAccessException erda) {
                logger.info("No data found");
                lastExecutionRs = new HashMap<String, Object>();
                lastExecutionRs.put("lastUpdated", new Date());
            }
        } else {
            lastExecutionRs = new HashMap<String, Object>();
            lastExecutionRs.put("lastUpdated", provided);
        }

        // 그래봤자 ID 속성이 뭔지 모르잖아
        List<String> successIds = new ArrayList<>();
        Map<String, Object> jdbcParam = SyntaxUtils.parse(query, lastExecutionRs);
        String jdbcQuery = String.valueOf(jdbcParam.get("query"));
        Object[] params = (Object[]) jdbcParam.get("params");

        // executeId 실행하고 결과 처리
        List<Map<String, Object>> targets2Update =
                ice2Template.queryForList(jdbcQuery, params);

        successIds = executeSingleTaskAndRecord(executionNode, targets2Update, "MNET", "SCHEDULE");
    }
}