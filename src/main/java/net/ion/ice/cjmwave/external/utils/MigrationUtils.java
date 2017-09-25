package net.ion.ice.cjmwave.external.utils;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Created by juneyoungoh on 2017. 9. 14..
 */
public class MigrationUtils {

    private static Logger logger = Logger.getLogger(MigrationUtils.class);

    public static void recordDataCopyRecord (JdbcTemplate template, Map<String, Object> report) {
        String query = "INSERT INTO MSSQL_DUMP_REPORT " +
                "(mssqlTable, mysqlTable, successCnt, skippedCnt" +
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

    public static void recordSingleData(JdbcTemplate template, String nodeType, String dataStr, int result) {
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


    /*
    * URL 로 파일을 요청하고 파일객체 반환
    * */
    public static File retrieveRemoteFile (String basicPath, String url) {
        File file = null;
        UUID uuid = UUID.randomUUID();
        try{
            file = new File(basicPath + "/" + uuid.toString() + ".jpg");
            URL requestUrl = new URL(url);
            int connectionTimeout = 3000, readTimeout = 60000;
            org.apache.commons.io.FileUtils.copyURLToFile(requestUrl, file, connectionTimeout, readTimeout);
        } catch (Exception e) {
            // file retrieve 실패 null 을 반환함
            file = null;
        }
        return file;
    }

    /*
    * 이 부분의 로직은 고객이 전달한 부분임
    * */
    public static String getMnetFileUrl (String mediaId, String mediaType, String sizeType) {
        String imgUrl = "http://cmsimg.global.mnet.com/clipimage";
        String strClipName = "";

        String upperCased = mediaType.toUpperCase();
        if("ALBUM".equals(upperCased) || "ARTIST".equals(upperCased)) {
            mediaType = upperCased.toLowerCase();
        } else if ("VOD".equals(upperCased) || "PROGRAM".equals(upperCased)) {
            mediaType = "vod";
        }

        strClipName = "0000000000" + mediaId;
        int sLen = strClipName.length() - 9;
        strClipName = strClipName.substring(sLen);
        return imgUrl + "/" + mediaType
                + "/" + sizeType
                + "/" + strClipName.substring(0, 3)
                + "/" + strClipName.substring(3, 6)
                + "/" + mediaId + ".jpg";
    }
}
