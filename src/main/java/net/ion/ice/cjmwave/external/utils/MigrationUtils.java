package net.ion.ice.cjmwave.external.utils;

import net.ion.ice.core.node.NodeType;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 14..
 */
public class MigrationUtils {

    private static Logger logger = Logger.getLogger(MigrationUtils.class);

    public static void recordDataCopyRecord (JdbcTemplate template, Map<String, Object> report) {
        String query = "INSERT INTO MSSQL_DUMP_REPORT " +
                "(msssqlTable, mysqlTable, successCnt, skippedCnt" +
                ", jobStarted, jobFinished, jobDuration) VALUES " +
                "(?, ?, ?, ?" +
                ", ?, ?, ?)";
        try{
            logger.info(
                    "\n#### DATA COPY Report :: #####" +
                            "\nMsSql Table : " + report.get("mssqlTable") +
                            "\nMySql Table : " + report.get("mysqlTable") +
                            "\nTime takes(ms) : " + report.get("jobDuration") + " ms" +
                            "\nSuccess Records Count : " + report.get("successCnt") +
                            "\nSkipped Records Count : " + report.get("skippedCnt") +
                            "\n##############################");

            template.update(query
                    , report.get("mssqlTable"), report.get("mysqlTable")
                    , report.get("successCnt"), report.get("skippedCnt")
                    , report.get("jobStarted"), report.get("jobFinished")
                    , report.get("jobDuration"));
        } catch (Exception e) {
            logger.error("FAILED TO HAND OUT MSSQL-MYSQL DATA COPY REPORT", e);
        }
    }


    public static void recordResult(JdbcTemplate template
            , String mig_target, String mig_type, String mig_parameter, String requestIp
            , String targetNode, int successCnt, int failCnt, long taskDuration, Date executeDate) {
        String query = "INSERT INTO MIG_HISTORY " +
                "(mig_target, mig_type, mig_parameter, request_ip, target_node" +
                ", success_cnt, fail_cnt, task_duration, execution_date)" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int cnt = template.update(query, mig_target, mig_type, mig_parameter, requestIp, targetNode
                , successCnt, failCnt, taskDuration, executeDate);

        logger.info("INSERT INTO MIG_HISTORY :: row count :: " + cnt);
    }

    public static void recordSingleDate(JdbcTemplate template, String nodeType, String dataStr, int result) {
        String insertQuery = "INSERT INTO MIG_DATA_HISTORY" +
                " (target_node, data_str, rs, created)" +
                " VALUES (?, ?, ?, now())";
        template.update(insertQuery, nodeType, dataStr, result);
    }


    public static void printReport(Date startTime, String executeId, String failPolicy, int successCnt, int skippedCnt) {
        long jobTaken = (new Date().getTime() - startTime.getTime());
        logger.info(
                "\n##### Execute Report :: ######" +
                        "\nExecutionId : " + executeId +
                        "\nStarted on : " + startTime +
                        "\nTime takes(ms) : " + jobTaken + " ms" +
                        "\nOnFail action : " + ((failPolicy == null || "null".equals(failPolicy)) ? "SKIP" : failPolicy) +
                        "\nSuccess Records Count : " + successCnt +
                        "\nSkipped Records Count : " + skippedCnt +
                        "\n##############################");
    }


    public static void saveFailureNodes(JdbcTemplate template, String keyProperty, Map<String, Object> mapNode) {
        try{
            String nodeType = String.valueOf(mapNode.get("typeId"));
            String nodeId = String.valueOf(mapNode.get(keyProperty));
            String nodeValue = String.valueOf(mapNode);
            String query = "INSERT INTO MIG_FAIL_DATA (nodeType, nodeId, nodeValue, created) "
                    + "VALUES(?, ?, ?, NOW())";
            template.update(query, nodeType, nodeId, nodeValue);
        } catch (Exception e) {
            logger.error("FAILED TO STORE FAILED NODE INFO ", e);
        }
    }
}
