package net.ion.ice.cjmwave.external.mnet.data;

import net.ion.ice.cjmwave.db.sync.utils.QueryUtils;
import net.ion.ice.cjmwave.db.sync.utils.SyntaxUtils;
import net.ion.ice.cjmwave.external.utils.MigrationUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
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
    * 서비스 기동에 필요한 자원 준비
    * */
    @PostConstruct
    private void initTemplate () {
        try{
            mnetMSSQLTemplate = dbService.getJdbcTemplate("mnetDb");
            ice2Template = dbService.getJdbcTemplate("cjDb");
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
                "WHERE mssqlTable = ? AND myssqlTable = ? " +
                "ORDER BY jobStarted DESC LIMIT 1";
        Map<String, Object> qResult = null;
        try {
            qResult = ice2Template.queryForMap(query, fromTable, toTable);
        } catch (EmptyResultDataAccessException erda) {
            logger.warn("No MSSQL migration report found");
        }

        if(qResult == null || qResult.isEmpty()) {
            return new Date();
        } else {
            Date lastDate = (Date) qResult.get("jobStarted");
            return lastDate;
        }
    }


    /*
    * 신규 데이터 MySql 에 밀어넣기
    * 키를 받아도 복합키같은 부분이 문제
    * */
    private Map<String, Integer> upsertData (String toTable, List<Map<String, Object>> newData) throws Exception {
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
                try {
                    Object[] uQueryParams = QueryUtils.prepareQueryParams(orderedUpdateFields, data);
                    int upRs = ice2Template.update(updateQuery, uQueryParams);
                    if(upRs < 1) {
                        Object[] iQueryParams = QueryUtils.prepareQueryParams(orderedInsertFields, data);
                        int inRs = ice2Template.update(insertQuery, iQueryParams);
                    }
                    successCnt++;
                } catch (Exception e) {
                    skippedCnt++;
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
                migrate(subTask, foreignKey);
            }
        }
    }


    private void migrate(Node replicationNode, Object foreignKey) {

        Date startDate = new Date();
        Map<String, Object> migReport = new HashMap<String, Object>();
        try{

            String q = String.valueOf(replicationNode.get("query"));
            String fromTable = String.valueOf(replicationNode.get("fromTable"));
            String toTable = String.valueOf(replicationNode.get("toTable"));
            String subTasks = String.valueOf(replicationNode.get("subTasks"));
            String subTaskKey = String.valueOf(replicationNode.get("subTaskKey")).trim();
            Date lastUpdated = getLastUpdated(fromTable, toTable);
            boolean standAlone = (boolean) replicationNode.get("standAlone");


            // 이 노드가 standAlone 이 아니라면 파라미터는 subTaskKey 를 써야 함
            // standAlone 이라면 날짜가 테이블에 있다.
            Map<String, Object> preparedQuery = null;
            Map<String, Object> params = new HashMap<>();
            if(standAlone) {
                params.put("lastUpdated", lastUpdated);
            } else if(foreignKey != null) {
                subTaskKey = String.valueOf(replicationNode.get("subTaskKey")).trim();
                params.put(subTaskKey, foreignKey);
            }
            preparedQuery = SyntaxUtils.parse(q, params);


            // MSSQL 로부터 데이터 가져오기
            List<Map<String, Object>> newData = queryMsSql(
                    String.valueOf(preparedQuery.get("query"))
                    , (Object[]) preparedQuery.get("params"));

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
            migReport.putAll(upsertData(toTable, newData));
        } catch (Exception e) {
            logger.error("error", e);
        }
        Date end = new Date();
        migReport.put("jobStarted", startDate);
        migReport.put("jobFinished", end);
        migReport.put("jobDuration", (end.getTime() - startDate.getTime()));
        MigrationUtils.recordDataCopyRecord(ice2Template, migReport);
    }


    // 특별한 테이블이 지정되지 않은 경우 모든 테이블에 대한 작업을 수행함
    public void copyData () {
        copyData("ALL");
    }


    public void copyData (String target) {
        // 각자 레포트 보고
        try{
            switch (target) {
                case  "ALL" :
                    List<Node> repNodeList = nodeService.getNodeList(REP_TID, "");
                    for(Node repNode : repNodeList) {
                        if((boolean)repNode.get("standAlone")) {
                            migrate(repNode, null);
                        }
                    }
                default:
                    Node repNode = nodeService.getNode(REP_TID, target);
                    if((boolean)repNode.get("standAlone")) {
                        migrate(repNode, null);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error("Error occurred while copy data from MSSQL to MYSQL", e);
        }
    }
}