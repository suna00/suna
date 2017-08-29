package net.ion.ice.schedule.timertask;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by juneyoungoh on 2017. 8. 21..
 * static 클래스
 * yml 을 읽어서 TimerTask 를 생성함
 * 해당 클래스는 bean  이 아닌 static 클래스기 때문에
 * context 정보는 caller 에서 넘겨준다
 */
public class TimerTaskFactory {

    private Logger logger;

    private TimerTaskFactory (){
        logger = Logger.getLogger(TimerTaskFactory.class);
    }
    private static class LazyHolder {
        private static final TimerTaskFactory INSTANCE = new TimerTaskFactory();
    }
    public static TimerTaskFactory getFactory(){
        return LazyHolder.INSTANCE;
    }

    /*
    * profile 이 제공되면 해당 프로파일명.yml 혹은 yaml 을 찾아 로드
    * */
    public Map<String, Timer> loadTasks(List taskInfoList){
        Map<String, Timer> found = new HashMap<String, Timer>();
        try{
            taskInfoList.forEach( single -> {
                Map<String, Object> singleMap = (Map<String, Object>) single;
                String schedulerId = String.valueOf(singleMap.get("schedule-id"));
                String cron = String.valueOf(singleMap.get("cron-expression"));
                TimerTask task = instantiateTimerTask(singleMap);
                if(!"null".equals(schedulerId) && task != null){
                    Timer timer = new Timer(schedulerId);
                    executeTimerWithCronExpression(timer, task, cron, schedulerId);
                    found.put(schedulerId, timer);
                }
            });

        } catch (Exception e) {
            logger.error(e);
        }
        return found;
    }


    private TimerTask instantiateTimerTask (Map<String, Object> taskInfo) {

        // 생성해서 돌리기 전에 대상 클래스가 Bean 인지 아닌지 확인할 필요가 있을까..
        TimerTask t = null;
        try{
            Class clz = Class.forName(String.valueOf(taskInfo.get("execute-class")));
            if(clz == null) return null;
            String executeMethod = String.valueOf(taskInfo.get("execute-method"));

            Method method = clz.getDeclaredMethod(executeMethod);
            if(method == null) return null;

            t = new ManagedTimerTask() {
                @Override
                public void run() {
                    super.run();
                    try{
                        method.invoke(clz.newInstance());
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.cancel();
                    }
                }
            };

        } catch (Exception e) {
            logger.error("Failed to load TimerTask by reflection :: " + String.valueOf(taskInfo), e);
        }

        return t;
    }

    /*
    * FROM ICE1
    * JJD
    * */
    private void executeTimerWithCronExpression(Timer timer, TimerTask task, String cronExpression, String name) {

        try{
            String values[] = StringUtils.split(cronExpression, ',');
            Calendar current = Calendar.getInstance();

            switch (values.length) {
                case 1: {
                    Integer delay = new Integer(values[0].trim());
                    timer.schedule(task, new Date(), delay) ;
                    break;
                }
                case 2: {
                    Integer delay = new Integer(values[0].trim());
                    Integer period = new Integer(values[1].trim());
                    timer.schedule(task, delay, period);
                    break;
                }
                case 3: {
                    Calendar firstTime = Calendar.getInstance();
                    firstTime.set(Calendar.HOUR_OF_DAY, new Integer(values[0].trim()));
                    firstTime.set(Calendar.MINUTE, new Integer(values[1].trim()));
                    if (firstTime.before(current)) {
                        firstTime.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    Integer period = new Integer(values[2].trim());
                    timer.schedule(task, firstTime.getTime(), period);
                    break;
                }
                case 4: {
                    Calendar firstTime = Calendar.getInstance();
                    firstTime.set(Calendar.DAY_OF_WEEK, new Integer(values[0].trim()));
                    firstTime.set(Calendar.HOUR_OF_DAY, new Integer(values[1].trim()));
                    firstTime.set(Calendar.MINUTE, new Integer(values[2].trim()));
                    if (firstTime.before(current)) {
                        firstTime.add(Calendar.WEEK_OF_MONTH, 1);
                    }
                    Integer period = new Integer(values[3].trim());
                    timer.schedule(task, firstTime.getTime(), period);
                    break;
                }
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("ERROR. Scheduler [" + name + "] has filed to register\n");
            e.printStackTrace();
        }
    }
}


