package net.ion.ice.cjmwave.external.utils;

import com.opencsv.CSVReader;
import net.ion.ice.core.json.JsonUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.FileReader;
import java.util.*;

/**
 * Created by juneyoungoh on 2017. 9. 14..
 */
public class MigrationUtils {

    private static Logger logger = Logger.getLogger(MigrationUtils.class);

    public static final String
        DB_FROMTABLE = "fromTable"
        , DB_TOTABLE = "toTable"
        , JOB_DURATION = "duration"
        , JOB_STARTON = "start"
        , JOB_SUCCESS = "successCnt"
        , JOB_SKIPPED = "skippedCnt"
        , MIGTYPE = "migrationType"
        , MIGTARGET = "migrationTarget"
        , NET_REQIP = "requestIp"
        , NET_PARAMS = "migrationParams"
        , NODE_TARGET = "targetNode";




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

    public static void recordResult(JdbcTemplate template, Map<String, Object> report) {
        recordResult(template,
                String.valueOf(report.get(MIGTARGET)), String.valueOf(report.get(MIGTYPE))
                , String.valueOf(report.get(NET_PARAMS)), String.valueOf(report.get(NET_REQIP))
                , String.valueOf(report.get(NODE_TARGET))
                , (int) report.get(JOB_SUCCESS)
                , (int) report.get(JOB_SKIPPED)
                , (long) report.get(JOB_DURATION)
                , (Date) report.get(JOB_STARTON)
        );
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

    public static void handoutNodeFailReport(JdbcTemplate template, String exName, Map<String, Object> mapNode) {
        try{
            String reportIn = "INSERT INTO NODE_CREATION_FAIL " +
                    "(nodeType, nodeId, exception, jsonValue) VALUES " +
                    "(?, ?, ?, ?)";
            String nodeType = String.valueOf(mapNode.get("typeId"));
            template.update(reportIn, nodeType, "", exName, JsonUtils.toJsonString(mapNode));
        } catch (Exception e) {
            logger.error("UNABLE TO HAND OUT NODE CREATE FAIL REPORT :: But consider it as normal");
        }
    }

    public static void handoutDB2DBFailReport(JdbcTemplate template, String fromTable, String toTable, String exception, String ppk) {
        try{
            String reportIn = "INSERT INTO MSSQL_DUMP_FAIL" +
                    " (mysqlTable, mssqlTable, exception, ppk)" +
                    " VALUES (?, ?, ?, ?)";
            template.update(reportIn, fromTable, toTable, exception, ppk);
        } catch (Exception e) {
            logger.error("UNABLE TO HAND OUT DB2DB DUMP FAIL REPORT :: But consider it as normal");
        }
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


    public static List<Map<String, Object>> readFromCSV(String csvFullPath, String delimiter) throws Exception {
        CSVReader csvReader = null;
        List<String> headers = new ArrayList<>();
        List<Map<String, Object>> rtn = new ArrayList<>();

        csvReader = new CSVReader(new FileReader(csvFullPath));
        String [] line;
        int i = 0;
        while ((line = csvReader.readNext()) != null) {
            if(i == 0) {
                headers = Arrays.asList(line);
            } else {
                Map<String, Object> row = new HashMap<String, Object>();
                for(String header : headers) {
                    int headerIndex = headers.indexOf(header);
                    row.put(header, line[headerIndex]);
                }
                rtn.add(row);
                logger.info("New CSV Data [ " + String.valueOf(row) + " ]");
            }
            i++;
        }
        return rtn;
    }
}
