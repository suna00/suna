package net.ion.ice.schedule;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.utils.YMLHelper;
import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;


import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by juneyoungoh on 2017. 8. 21..
 * 이 빈에서 타이머 태스크를 ID 맵핑으로 가지고 있겠다
 * 싱글톤으로 관리되어야 하지
 */

@Component
public class SchedulerStore {
    private Logger logger = Logger.getLogger(SchedulerStore.class);
    private ConcurrentHashMap<String, Timer> schedulerMap = new ConcurrentHashMap<>();
    private HashMap<String, Object> scheduleExtraInfoMap = new HashMap<String, Object>();

    @Autowired
    ApplicationContextManager contextManager;

    @Value("${spring.profiles}")
    String profiles;

    @PostConstruct
    private void loadTimer (){
        init();
    }


    public Timer getTimer (String schedulerId) {
        return schedulerMap.get(schedulerId);
    }
    public Object getInfo (String schedulerId) { return scheduleExtraInfoMap.get(schedulerId); }
    public void addTask(String schedulerId, Timer task) {
        schedulerMap.put(schedulerId, task);
    }

    public List<Map<String, Object>> toMapList () {
        List<Map<String, Object>> scheduleList = new ArrayList<>();
        Iterator<String> iter = schedulerMap.keySet().iterator();
        while(iter.hasNext()) {
            String k = iter.next();
            Timer t = schedulerMap.get(k);
            Map<String, Object> scheduleInfo = new HashMap<>();
            scheduleInfo.put("schedulerId", k);
            scheduleList.add(scheduleInfo);
        }
        return scheduleList;
    }

    public void init () {
        try{
            Resource r = contextManager.getResource("classpath:schedule/" + profiles + ".yml");
            File yml = r.getFile();

            YMLHelper.loadYML(yml.getCanonicalPath(), yml.getName());
            Map<String, Object> map = YMLHelper.getYML(yml.getName());

            List taskInfoList = (List) map.get("schedulers");
            taskInfoList.forEach( single -> {
                Map<String, Object> singleMap = (Map<String, Object>) single;
                Map<String, Object> subMap = new HashMap<String, Object>();
                String schedulerId = String.valueOf(singleMap.get("schedule-id"));
                String cron = String.valueOf(singleMap.get("cron-expression"));
                String className = String.valueOf(singleMap.get("execute-class"));
                String methodName = String.valueOf(singleMap.get("execute-method"));
                subMap.put("cron", cron);
                subMap.put("class", className);
                subMap.put("method", methodName);
                scheduleExtraInfoMap.put(schedulerId, subMap);
            });
            Map<String, Timer> taskMap = TimerTaskFactory.getFactory().loadTasks(taskInfoList);
            schedulerMap.putAll(taskMap);
        } catch (Exception e) {
            logger.error("Failed to load Scheduler Information :: ", e);
        }
    }
}
