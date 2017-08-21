//package net.ion.ice.schedule;
//
//import org.apache.log4j.Logger;
//import org.infinispan.factories.scopes.Scopes;
//import org.springframework.stereotype.Component;
//
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Created by juneyoungoh on 2017. 8. 21..
// * 이 빈에서 타이머 태스크를 ID 맵핑으로 가지고 있겠다
// * 싱글톤으로 관리되어야 하지
// */
//@Component
//public class SchedulerStore {
//    private Logger logger = Logger.getLogger(SchedulerStore.class);
//    ConcurrentHashMap<String, TimerTask> schedulerMap = new ConcurrentHashMap<>();
//
//    SchedulerStore () {
//        try{
//            // yaml 읽어서 맵에 key/set 으로 담는다
//            // 이건 Factory 에서 처리할 거
//
//        } catch (Exception e) {
//            logger.error("Failed to load Scheduler Information :: ", e);
//        }
//    }
//
//    public TimerTask getTask (String schedulerId) {
//        return schedulerMap.get(schedulerId);
//    }
//
//    public void addTask(String schedulerId, TimerTask task) {
//        schedulerMap.put(schedulerId, task);
//    }
//
//    public List<Map<String, Object>> toMapList () {
//        List<Map<String, Object>> scheduleList = new ArrayList<>();
//        Iterator<String> iter = schedulerMap.keySet().iterator();
//        while(iter.hasNext()) {
//            String k = iter.next();
//            TimerTask t = schedulerMap.get(k);
//            Map<String, Object> scheduleInfo = new HashMap<>();
//            scheduleInfo.put("id", k);
//            scheduleInfo.put("scheduled_time", t.scheduledExecutionTime());
//            scheduleList.add(scheduleInfo);
//        }
//        return scheduleList;
//    }
//}
