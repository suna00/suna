package net.ion.ice.cjmwave.external.monitor;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 10. 2..
 */
@Controller
@RequestMapping(value = {"/migration/monitor"})
public class MigrationMonitorController {

    @Autowired
    MigrationMonitorService migrationMonitorService;

    private Logger logger = Logger.getLogger(MigrationMonitorService.class);

    /*
    * 마이그레이션 내역 조회
    * */
    @RequestMapping(value = {"historyList"}, produces = { "application/json" })
    public @ResponseBody String retrieveMigrationHistory() throws Exception {
        JSONObject response = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";
        List<Map<String, Object>> items = new ArrayList<>();
        try{
            items = migrationMonitorService.getMigrationHistory();
            result = "200";
            result_msg = "SUCCESS";
            logger.info("History ITEM.size() :: " + items.size());
        } catch (Exception e) {
            e.printStackTrace();
            cause = e.getMessage();
        }
        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("items", JSONObject.wrap(items));
        response.put("cause", cause);
        return String.valueOf(response);
    }



    /*
    * 마이그레이션 실패 내역 조회
    * */
    @RequestMapping(value = {"failList"}, produces = { "application/json" })
    public @ResponseBody String retrieveFailNodes() throws Exception {
        JSONObject response = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";
        List<Map<String, Object>> items = new ArrayList<>();
        try{
            items = migrationMonitorService.getFailureHistory();
            result = "200";
            result_msg = "SUCCESS";
            logger.info("Failure ITEM.size() :: " + items.size());
        } catch (Exception e) {
            e.printStackTrace();
            cause = e.getMessage();
        }
        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("items", JSONObject.wrap(items));
        response.put("cause", cause);
        return String.valueOf(response);
    }



    /*
    * 마이그레이션 프로세스 조회
    * */
    @RequestMapping(value = {"processList"}, produces = { "application/json" })
    public @ResponseBody String retrieveMigrationProcess() throws Exception {
        JSONObject response = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";
        List<Map<String, Object>> items = new ArrayList<>();
        try{
            items = migrationMonitorService.getProcessList();
            result = "200";
            result_msg = "SUCCESS";
            logger.info("Process ITEM.size() :: " + items.size());
        } catch (Exception e) {
            e.printStackTrace();
            cause = e.getMessage();
        }
        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("items", JSONObject.wrap(items));
        response.put("cause", cause);
        return String.valueOf(response);
    }
}
