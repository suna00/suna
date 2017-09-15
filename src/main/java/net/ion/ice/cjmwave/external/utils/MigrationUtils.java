package net.ion.ice.cjmwave.external.utils;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

/**
 * Created by juneyoungoh on 2017. 9. 14..
 */
public class MigrationUtils {

    private static Logger logger = Logger.getLogger(MigrationUtils.class);

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
}
