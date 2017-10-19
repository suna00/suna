package net.ion.ice.cjmwave.external.mnet.data;

import net.ion.ice.cjmwave.db.sync.utils.QueryUtils;
import net.ion.ice.cjmwave.db.sync.utils.SyntaxUtils;
import net.ion.ice.cjmwave.external.utils.MigrationUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by juneyoungoh on 2017. 9. 16..
 * 해당 서비스는 Mnet MSSQL 에 접근(SP 실행)하여 개별 Database 의 변경 분을
 * MYSQL(ice2) 레플리카 데이터베이스에 밀어넣는다
 *
 * 상대 테이블에 상응하는 모든 테이블이 존재하여야 함
 *
 * 규칙 1. 테이블명은 MSSQL 원본 테이블.toUpperCase 로 지정한다
 * 규칙 2. MYSSQL 테이블은 대소문자를 구분한다 (하지만 반드시 테이블명은 대문자로 만들기)
 * 규칙 3. 작업 기록에 대한 리포트(요약 정보) 를 기록한다
 *
 * MSSQL 없어서 테스트 못해봄
 */
@Service
public class MnetDataDumpService {

    @Autowired
    DBService dbService;

    @Autowired
    NodeService nodeService;

    private static String REP_TID = "msSqlReplication";
    private Logger logger = Logger.getLogger(MnetDataDumpService.class);
    private JdbcTemplate mnetMSSQLTemplate;
    private JdbcTemplate ice2Template;

    /*
    * 한 테이블에 얼마나 정보를 밀어넣었는가를 판단하는 기준이므로 MySQL 테이블명을 키로 잡는다
    * */
    Map<String, Object> migrationReports;

    /*
    * 서비스 기동에 필요한 자원 준비
    * */
    @PostConstruct
    private void initTemplate () {
        try{
            ice2Template = dbService.getJdbcTemplate("cjDb");
            mnetMSSQLTemplate = dbService.getJdbcTemplate("mnetDb");
        } catch (Exception e) {
            logger.error("Failed to Initialize. Disabled to migrate", e);
        }
    }

    /*
    * Mnet 신규데이터 가져오기
    * */
    private List<Map<String, Object>> queryMsSql (String query, Object[] params) {
        List<Map<String, Object>> newMnetData = new ArrayList<>();
        try {
            newMnetData = mnetMSSQLTemplate.queryForList(query, params);
        } catch (EmptyResultDataAccessException erda) {
            logger.warn("No Data found");
        }
        return newMnetData;
    }


    /*
    * 마지막 레플리케이션 작업 시점 가져오기
    * */
    private Date getLastUpdated (String fromTable, String toTable) {
        String query = "SELECT * FROM MSSQL_DUMP_REPORT " +
                "WHERE mssqlTable = ? AND mysqlTable = ? " +
                "ORDER BY jobStarted DESC LIMIT 1";
        Map<String, Object> qResult = null;
        try {
            qResult = ice2Template.queryForMap(query, fromTable, toTable);
        } catch (EmptyResultDataAccessException erda) {
            logger.warn("No MSSQL migration report found");
        }

        if(qResult == null || qResult.isEmpty()) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 14);
            return cal.getTime();
        } else {
            Date lastDate = (Date) qResult.get("jobStarted");
            return lastDate;
        }
    }


    /*
    * 신규 데이터 MySql 에 밀어넣기
    * 키를 받아도 복합키같은 부분이 문제
    * */
    private Map<String, Integer> upsertData (String fromTable, String toTable, List<Map<String, Object>> newData) throws Exception {
        Map<String, Integer> rtn = new HashMap<>();
        int successCnt = 0, skippedCnt = 0;
        if(newData != null && !newData.isEmpty()) {
            Map<String, Object> insertInfo = QueryUtils.prepareInsertQuery(toTable, newData.get(0));
            Map<String, Object> updateInfo = QueryUtils.prepareUpdateQuery(ice2Template, toTable, newData.get(0));
            String updateQuery = String.valueOf(updateInfo.get("updateQ"));
            String insertQuery = String.valueOf(insertInfo.get("insertQ"));

            List<String> orderedUpdateFields = (List<String>) updateInfo.get("orderedParameterKeys");
            List<String> orderedInsertFields = (List<String>) insertInfo.get("orderedParameterKeys");

            for(Map<String, Object> data : newData) {
                Object[] uQueryParams = null;
                try {
                    uQueryParams = QueryUtils.prepareQueryParams(orderedUpdateFields, data);
                    int upRs = ice2Template.update(updateQuery, uQueryParams);
                    if(upRs < 1) {
                        Object[] iQueryParams = QueryUtils.prepareQueryParams(orderedInsertFields, data);
                        int inRs = ice2Template.update(insertQuery, iQueryParams);
                    }
                    successCnt++;
                } catch (Exception e) {
                    logger.error("Failed to copy data :: ", e);
                    skippedCnt++;
                    MigrationUtils.handoutDB2DBFailReport(ice2Template, fromTable, toTable, e.getClass().getName(), Arrays.toString(uQueryParams));
                }
            }
        }
        rtn.put("successCnt", successCnt);
        rtn.put("skippedCnt", skippedCnt);
        return rtn;
    }

    /*
    * 전달받은 노드의 하위 데이터가 있다면 해당 프로세스도 실행한다.
    * */
    private void executeSubTask (Node replicaNode, Object foreignKey) {
        String subTasksStr = String.valueOf(replicaNode.get("subTasks"));
        if(!"null".equals(subTasksStr)) {
            //sub Tasks 에 대한 잡을 실행함
            List<String> subTasksList = new ArrayList<>();
            String[] subTasksArr = subTasksStr.split(",");
            for(int i = 0; i < subTasksArr.length; i++) {
                subTasksList.add(subTasksArr[i].trim());
            }

            // 걸러진 태스크에 대해서 아래 키로 조회
            for(String subExecuteId : subTasksList) {
                Node subTask = nodeService.getNode(REP_TID, subExecuteId);
                migrate(subTask, foreignKey, null);
            }
        }
    }

    /*
    * 본정보와 부가정보의 리포트를 종합해서 사용하기 위한 용도
    * 가령 artist 와 artist meta 가 있으면 해당 맵 정보를 마이그레이션 종료 시점까지
    * 지속적으로 업데이트함
    * */
    private void updateReport(String toTable, Map<String, Object> report) throws Exception {
        if (migrationReports.containsKey(toTable)) {
            // 기존 자료 업데이트
            /*
            * 업데이트 되는 항목은 + successCnt, skippedCnt, jobDuration, jobFinished
            *
            * */
            int currentSuccessCnt = (int) report.get("successCnt");
            int currentSkippedCnt = (int) report.get("skippedCnt");
            long currentJobDuration = (long) report.get("jobDuration");
            Map<String, Object> formerData = (Map) migrationReports.get(toTable);
            formerData.put("jobFinished", report.get("jobFinished"));

            if(formerData.containsKey("successCnt")) {
                formerData.put("successCnt", (currentSuccessCnt + (int) formerData.get("successCnt")));
                formerData.put("skippedCnt", (currentSkippedCnt + (int) formerData.get("skippedCnt")));
                formerData.put("jobDuration", (currentJobDuration + (long) formerData.get("jobDuration")));
            }
            migrationReports.put(toTable, formerData);
        } else {
            // 신규 등록
            migrationReports.put(toTable, report);
        }
    }


    private void  migrate(Node replicationNode, Object foreignKey, Date provided) {

        logger.info("in migrate method :: DATE");
        Date startDate = new Date();
        Map<String, Object> migReport = new HashMap<String, Object>();
        String fromTable = "", toTable = "";

        try{

            String q = String.valueOf(replicationNode.get("query"));
            fromTable = String.valueOf(replicationNode.get("fromTable"));
            toTable = String.valueOf(replicationNode.get("toTable"));
            String subTasks = String.valueOf(replicationNode.get("subTasks"));
            String subTaskKey = String.valueOf(replicationNode.get("subTaskKey")).trim();
            Date lastUpdated = getLastUpdated(fromTable, toTable);
            boolean standAlone = (boolean) replicationNode.get("standAlone");


            // 이 노드가 standAlone 이 아니라면 파라미터는 subTaskKey 를 써야 함
            // standAlone 이라면 날짜가 테이블에 있다.
            Map<String, Object> preparedQuery = null;
            Map<String, Object> params = new HashMap<>();
            if(standAlone) {
                if(provided != null) {
                    params.put("lastUpdated", provided);
                } else {
                    params.put("lastUpdated", lastUpdated);
                }
            } else if(foreignKey != null) {
                subTaskKey = String.valueOf(replicationNode.get("subTaskKey")).trim();
                params.put("id", foreignKey);
            }
            preparedQuery = SyntaxUtils.parse(q, params);


            logger.info("print jdbcTemplate Query :: " + String.valueOf(preparedQuery.get("query")));

            // MSSQL 로부터 데이터 가져오기
            List<Map<String, Object>> newData = queryMsSql(
                    String.valueOf(preparedQuery.get("query"))
                    , (Object[]) preparedQuery.get("params"));

            logger.info("Length of Query result :: " + newData.size());

            if(standAlone && !"null".equals(subTasks)) {
                for(Map<String, Object> singleDataToInput : newData){
                    Object subTaskKeyValue = null;
                    if(singleDataToInput.containsKey(subTaskKey.toLowerCase())){
                        subTaskKeyValue = singleDataToInput.get(subTaskKey.toLowerCase());
                        executeSubTask(replicationNode, subTaskKeyValue);
                    } else if(singleDataToInput.containsKey(subTaskKey.toUpperCase())) {
                        subTaskKeyValue = singleDataToInput.get(subTaskKey.toUpperCase());
                        executeSubTask(replicationNode, subTaskKeyValue);
                    }
                }
            }

            //mySql 에 밀어넣기
            migReport.putAll(upsertData(fromTable, toTable, newData));
        } catch (Exception e) {
            logger.error("error", e);
        }
        Date end = new Date();
        migReport.put("mssqlTable", fromTable);
        migReport.put("mysqlTable", toTable);
        migReport.put("jobStarted", startDate);
        migReport.put("jobFinished", end);
        migReport.put("jobDuration", (end.getTime() - startDate.getTime()));
        try{
            updateReport(toTable, migReport);
        } catch (Exception e) {
            logger.info("It does not make you fool even though you are a poor reporter..." + toTable);
        }
    }

    private void  migrateWithList(Node replicationNode, Object foreignKey, List<String> ids) {

        logger.info("in migrate method :: LIST");
        Date startDate = new Date();
        Map<String, Object> migReport = new HashMap<String, Object>();
        String fromTable = "", toTable = "";

        try{

            String q = String.valueOf(replicationNode.get("query"));
            fromTable = String.valueOf(replicationNode.get("fromTable"));
            toTable = String.valueOf(replicationNode.get("toTable"));
            String subTasks = String.valueOf(replicationNode.get("subTasks"));
            String subTaskKey = String.valueOf(replicationNode.get("subTaskKey")).trim();
            Date lastUpdated = getLastUpdated(fromTable, toTable);
            boolean isTopNode = (boolean) replicationNode.getId().toLowerCase().contains("retrieve");

            // 이 노드가 standAlone 이 아니라면 파라미터는 subTaskKey 를 써야 함
            // standAlone 이라면 날짜가 테이블에 있다.
            Map<String, Object> preparedQuery = null;
            Map<String, Object> params = new HashMap<>();
            if(isTopNode) {
                System.out.println("========= IS TOP NODE ============= ");
                if(ids.size() == 1) {
                    params.put("id", ids.get(0));
                } else if(ids.size() == 10) {
                    for(int i = 1; i < 11; i++) {
                        params.put("id" + i, ids.get(i - 1));
                    }
                }
                System.out.println("========= IDS ============= " + StringUtils.join(ids.toArray()));
            } else if(foreignKey != null) {
                subTaskKey = String.valueOf(replicationNode.get("subTaskKey")).trim();
                params.put("id", foreignKey);
            }

            System.out.println("Query is :: " + q);
            System.out.println("Query is :: " + String.valueOf(params));
            preparedQuery = SyntaxUtils.parse(q, params);


            logger.info("print jdbcTemplate Query :: " + String.valueOf(preparedQuery.get("query")));

            // MSSQL 로부터 데이터 가져오기
            List<Map<String, Object>> newData = queryMsSql(
                    String.valueOf(preparedQuery.get("query"))
                    , (Object[]) preparedQuery.get("params"));

            logger.info("Length of Query result :: " + newData.size());

            if(isTopNode && !"null".equals(subTasks)) {
                for(Map<String, Object> singleDataToInput : newData){
                    Object subTaskKeyValue = null;
                    if(singleDataToInput.containsKey(subTaskKey.toLowerCase())){
                        subTaskKeyValue = singleDataToInput.get(subTaskKey.toLowerCase());
                        executeSubTask(replicationNode, subTaskKeyValue);
                    } else if(singleDataToInput.containsKey(subTaskKey.toUpperCase())) {
                        subTaskKeyValue = singleDataToInput.get(subTaskKey.toUpperCase());
                        executeSubTask(replicationNode, subTaskKeyValue);
                    }
                }
            }

            //mySql 에 밀어넣기
            migReport.putAll(upsertData(fromTable, toTable, newData));
        } catch (Exception e) {
            logger.error("error", e);
        }
        Date end = new Date();
        migReport.put("mssqlTable", fromTable);
        migReport.put("mysqlTable", toTable);
        migReport.put("jobStarted", startDate);
        migReport.put("jobFinished", end);
        migReport.put("jobDuration", (end.getTime() - startDate.getTime()));
        try{
            updateReport(toTable, migReport);
        } catch (Exception e) {
            logger.info("It does not make you fool even though you are a poor reporter..." + toTable);
        }
    }



    public void copyData (String target, Date provided) {
        // 각자 레포트 보고
        logger.info("START COPY DATA :: " + target);
        // 리포트 정보 초기화
        migrationReports = new HashMap<>();
        String type = target.trim().toLowerCase();
        try{
            switch (type) {
                case  "all" :
                    List<Node> repNodeList = nodeService.getNodeList(REP_TID, "");
                    for(Node repNode : repNodeList) {
                        if((boolean)repNode.get("standAlone")) {
                            migrate(repNode, null, provided);
                        }
                    }
                case  "chart" :
                    Node charMstNode = nodeService.getNode(REP_TID, "chartMst");
                    logger.info("repNode :: " + charMstNode);
                    migrate(charMstNode, null, provided);
                    Node chartLstNode = nodeService.getNode(REP_TID, "chartLst");
                    logger.info("repNode :: " + chartLstNode);
                    migrate(chartLstNode, null, provided);
                    break;
                default:
                    Node repNode = nodeService.getNode(REP_TID, target);
                    logger.info("repNode :: " + repNode);
//                    if((boolean)repNode.get("standAlone")) {
                    if("true".equals(String.valueOf(repNode.get("standAlone")))) {
                        logger.info("Starting head node migration...");
                        migrate(repNode, null, provided);
                    } else {
                        logger.info("This is a tail node");
                    }
                    break;
            }

            Iterator<String> migRepoIterator = migrationReports.keySet().iterator();
            while(migRepoIterator.hasNext()) {
                String reportKey = migRepoIterator.next();
                MigrationUtils.recordDataCopyRecord(ice2Template, (Map) migrationReports.get(reportKey));
            }

        } catch (Exception e) {
            logger.error("Error occurred while copy data from MSSQL to MYSQL", e);
        }
    }

    /*
    * ===================== 아 이게 될려나 ...
    * ===================== 임시 처리를 위해 추가되는 부분
    * */
    public void copyData (String target, List<String> ids) {
        // 각자 레포트 보고
        logger.info("START TEMPORARY COPY DATA :: " + target);
        // 리포트 정보 초기화
        migrationReports = new HashMap<>();
        target = target.trim().toLowerCase();
        // 멀티를 뽑을건지 싱글을 뽑을 건지 여기서 결정되어야 함
        try {
            int targetLength = ids.size();
            int unit = 10;
            int head = targetLength / unit;
            int tail = targetLength % unit;
            int toIndex = 1;

            Node replicationMultiLogic = null;
            Node replicationSingleLogic = null;
            switch (target.toLowerCase()) {
                case "artist":
                    replicationMultiLogic = nodeService.read(REP_TID, "retrieveMultiArtist");
                    replicationSingleLogic = nodeService.read(REP_TID, "retrieveSingleArtist");
                    break;
                case "album":
                    replicationMultiLogic = nodeService.read(REP_TID, "retrieveMultiAlbum");
                    replicationSingleLogic = nodeService.read(REP_TID, "retrieveSingleAlbum");
                    break;
                case "musicvideo":
                    replicationMultiLogic = nodeService.read(REP_TID, "retrieveMultiMusicVideo");
                    replicationSingleLogic = nodeService.read(REP_TID, "retrieveSingleMusicVideo");
                    break;
                case "song" :
                    replicationMultiLogic = nodeService.read(REP_TID, "retrieveMultiSong");
                    replicationSingleLogic = nodeService.read(REP_TID, "retrieveSingleSong");
                    break;
                default:
                    logger.info("Invalid Type :: " + target);
                    return;
            }

            logger.info("TotalCount :: " + ids.size());
            logger.info("UnitCount :: " + head);
            logger.info("LeftCount :: " + tail);

            // 10 개씩 묶어서 처리
            for(int i = 0; i < head; i++) {
                int startIdx = (i * unit);
                toIndex = ((i + 1) * unit);
                List<String> subListByUnit = ids.subList(startIdx, toIndex);
                migrateWithList(replicationMultiLogic, null, subListByUnit);
            }

            // 남은 항목에 대한 개별 처리
            List<String> left = ids.subList(toIndex -1, ids.size());
            for(int i = 0; i < left.size(); i++) {
                // 1 개짜리 복사
                List<String> singleList = new ArrayList<>();
                singleList.add(left.get(i));
                migrateWithList(replicationSingleLogic, null, singleList);
            }

            Iterator<String> migRepoIterator = migrationReports.keySet().iterator();
            while(migRepoIterator.hasNext()) {
                String reportKey = migRepoIterator.next();
                MigrationUtils.recordDataCopyRecord(ice2Template, (Map) migrationReports.get(reportKey));
            }
        } catch (Exception e) {
            logger.error("Error occurred while copy data from MSSQL to MYSQL", e);
        }
    }
}