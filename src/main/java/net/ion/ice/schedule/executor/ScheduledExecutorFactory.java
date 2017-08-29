package net.ion.ice.schedule.executor;

import net.ion.ice.schedule.utils.TimeExpressionParser;
import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by juneyoungoh on 2017. 8. 29..
 * 이건 매주 화요일 이런건 안됨.
 */
public class ScheduledExecutorFactory {
    private Logger logger = Logger.getLogger(ScheduledExecutorFactory.class);
    private ScheduledExecutorFactory () {
        logger.info("Initializing ScheduledExecutor Service...");
    }

    // private 생성자로 new 를 막고, Factory 자체는 하나만 존재하여야 함
    private static final ScheduledExecutorFactory INSTANCE = new ScheduledExecutorFactory();
    public static ScheduledExecutorFactory getFactory(){
        return INSTANCE;
    }

    // 일부 메소드가 Runnable 만 지원함
    public ScheduledFuture getScheduledExecutor(Runnable action, String scheduleString) throws Exception {
        return getScheduledExecutor(action, scheduleString, null);
    }
    public ScheduledFuture getScheduledExecutor(Runnable action, String scheduleString, String schedulerName) throws Exception {
        int default_core_pool = 10;
        ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(default_core_pool);
        Map<String, Object> scheduleInfoMap = parseScheduledString(scheduleString);

        String methodName = String.valueOf(scheduleInfoMap.get("method"));

        long initialDelay = (long) scheduleInfoMap.get("initialDelay");
        long period = (long) scheduleInfoMap.get("period");
        long delay = (long) scheduleInfoMap.get("delay");
        TimeUnit timeUnit = (TimeUnit) scheduleInfoMap.get("unit");
        String executorClz = ScheduledExecutorService.class.getName();

        ScheduledFuture future = null;
        logger.info("Will execute [ " + executorClz + "." + methodName +  " ] with [ " + schedulerName + " ]");
        switch (methodName) {
            case "schedule" :
                future = (ScheduledFuture) executeReflection(executorClz, methodName, action, delay, timeUnit);
                break;
            case "scheduleAtFixedRate" :
                future = (ScheduledFuture) executeReflection(executorClz, methodName, action, initialDelay, period, timeUnit);
                break;
            case "scheduleWithFixedDelay" :
                future = (ScheduledFuture) executeReflection(executorClz, methodName, action, initialDelay, delay, timeUnit);
                break;
            default:
                logger.info("Could not find scheduled method");
                break;
        }
        return future;
    }


    private Object executeReflection(String className, String methodName, Object ... params) throws Exception {
        Class clz = Class.forName(className);
        Class[] paramTypes = new Class[params.length];
        for(int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        Method executable = clz.getDeclaredMethod(methodName, paramTypes);
        return executable.invoke(clz, params);
    }

    // 스케쥴러문자열 파싱은 별도로 유틸리티로 뺌
    private Map<String, Object> parseScheduledString(String str){
        Map<String, Object> infoMap = TimeExpressionParser.parseForScheduledExecutor(str);
        infoMap.put("method", infoMap.get("method"));
        infoMap.put("initialDelay", infoMap.get("initialDelay"));
        infoMap.put("period", infoMap.get("period"));
        infoMap.put("delay", infoMap.get("delay"));
        infoMap.put("unit", infoMap.get("unit"));
        return infoMap;
    }
};