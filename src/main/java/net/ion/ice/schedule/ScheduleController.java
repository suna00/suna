package net.ion.ice.schedule;

import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * Created by juneyoungoh on 2017. 8. 21..
 * 현재 애플리케이션 컨텍스트에 등록된 스케줄의
 * - 상태 조회
 * - 수정은 안됨
 * - 기동/중지할 수 있음
 *
 * - 기본은 yml 에서 읽은 스케쥴러
 */
@Controller(value = "scheduleController")
@RequestMapping(value = "applicationSchedule")
public class ScheduleController {
    private Logger logger = Logger.getLogger(ScheduleController.class);

    @Autowired
    SchedulerStore store;

    /*
    * 상태값 어떻게 가져올지 고민하기
    * */
    @RequestMapping(value = "list", produces = {"application/json"})
    public @ResponseBody String getSchedulerInfos () {
        JSONObject response = new JSONObject();
        List<Map<String, Object>> schedulerList = new ArrayList<>();
        List<Map<String, Object>> mergedList = new ArrayList<>();
        String result = "500", result_msg = "ERROR", cause = "";
        try{
            schedulerList = store.toMapList();
            for(Map<String, Object> single : schedulerList) {
                Map<String, Object> merged = new HashMap<String, Object>();
                String sId = String.valueOf(single.get("schedulerId"));
                merged.put("schedulerId", sId);
                merged.putAll((Map)store.getInfo(sId));
                mergedList.add(merged);
            }

            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error("Failed to list up schedulers :: ", e);
            cause = e.getMessage();
        }

        response.put("items", mergedList);
        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("cause", cause);
        return String.valueOf(response);
    }

    /*
    * yml 정보를 유지한 채로 특정 스케쥴러를 정지하고 싶을 때 사용
    * */
    @RequestMapping(value = "stop/{schedulerId}", produces = {"application/json"})
    public @ResponseBody String stopScheduler (@PathVariable String schedulerId) {
        JSONObject response = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";
        try{
            Timer t = store.getTimer(schedulerId);
            if(t == null) throw new Exception("Scheduler [ " + schedulerId + " ] does not exists");
            t.cancel();
            t.purge();
            result = "200"; result_msg = "SUCCESS";
        } catch (Exception e) {
            cause = e.getMessage();
        }

        response.put("scheduler_id", schedulerId);
        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("cause", cause);
        return String.valueOf(response);
    }


    /*
    *  파일 다시 말아 올림
    */
    @RequestMapping(value = "reload", produces = {"application/json"})
    public @ResponseBody String reload () {
        JSONObject response = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";

        try{
            store.init();
            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            cause = e.getMessage();
        }
        response.put("result", result);
        response.put("result_msg", result_msg);
        response.put("cause", cause);
        return String.valueOf(response);
    }
}