//package net.ion.ice.schedule;
//
//import net.minidev.json.JSONObject;
//import org.apache.log4j.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//import java.util.*;
//
///**
// * Created by juneyoungoh on 2017. 8. 21..
// * 현재 애플리케이션 컨텍스트에 등록된 스케줄의
// * - 상태 조회
// * - 수정은 안됨
// * - 기동/중지할 수 있음
// *
// * - 기본은 yml 에서 읽은 스케쥴러
// */
//@Controller(value = "scheduleController")
//@RequestMapping(value = "applicationSchedule")
//public class ScheduleController {
//    private Logger logger = Logger.getLogger(ScheduleController.class);
//
//    @Autowired
//    SchedulerStore store;
//
//
//    @RequestMapping(value = "list", produces = {"application/json"})
//    public @ResponseBody String getSchedulerInfos () {
//        JSONObject response = new JSONObject();
//        List<Map<String, Object>> schedulerList = new ArrayList<>();
//        String result = "500", result_msg = "ERROR", cause = "";
//        try{
//            schedulerList = store.toMapList();
//            result = "200";
//            result_msg = "SUCCESS";
//        } catch (Exception e) {
//            logger.error("Failed to list up schedulers :: ", e);
//            cause = e.getMessage();
//        }
//
//        response.put("items", schedulerList);
//        response.put("result", result);
//        response.put("result_msg", result_msg);
//        response.put("cause", cause);
//        return String.valueOf(response);
//    }
//
//
//    @RequestMapping(value = "stop/{schedulerId}", produces = {"application/json"})
//    public @ResponseBody String stopScheduler (@PathVariable String schedulerId) {
//        JSONObject response = new JSONObject();
//        String result = "500", result_msg = "ERROR", cause = "";
//        boolean rs = false;
//        try{
//            TimerTask t = store.getTask(schedulerId);
//            if(t == null) throw new Exception("Scheduler [ " + schedulerId + " ] does not exists");
//            rs = t.cancel();
//            result = "200"; result_msg = "SUCCESS";
//        } catch (Exception e) {
//            cause = e.getMessage();
//        }
//
//        response.put("stop_rs", rs);
//        response.put("scheduler_id", schedulerId);
//        response.put("result", result);
//        response.put("result_msg", result_msg);
//        response.put("cause", cause);
//        return String.valueOf(response);
//    }
//
//
//    @RequestMapping(value = "start/{schedulerId}", produces = {"application/json"})
//    public @ResponseBody String startScheduler (@PathVariable String schedulerId) {
//        JSONObject response = new JSONObject();
//        String result = "500", result_msg = "ERROR", cause = "";
//
//        try{
//            TimerTask t = store.getTask(schedulerId);
//            if(t == null) throw new Exception("Scheduler [ " + schedulerId + " ] does not exists");
//            t.run();
//            result = "200";
//            result_msg = "SUCCESS";
//        } catch (Exception e) {
//            cause = e.getMessage();
//        }
//        response.put("scheduler_id", schedulerId);
//        response.put("result", result);
//        response.put("result_msg", result_msg);
//        response.put("cause", cause);
//        return String.valueOf(response);
//    }
//}