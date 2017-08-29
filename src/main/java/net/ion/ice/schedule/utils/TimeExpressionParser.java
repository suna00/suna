package net.ion.ice.schedule.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by juneyoungoh on 2017. 8. 29..
 * - scheduleAtFixedRate() 프로세스 종료여부와 관계없이 반복
 *  - scheduleWithFixedDelay() 해당 프로세스가 종료된 이후 반복
 */
public class TimeExpressionParser {

    /*
    * 반환 키 정보
    * methodName
    * long initialDelay = 0;
    * long period = 0;
    * long delay = 0;
    * */
    public static Map<String, Object> parseForScheduledExecutor(String timeExpression) {
        String values [] = timeExpression.split(",");
        Map<String, Object> infoMap = new HashMap<String, Object>();
        String methodName = "";
        long delay = 0;
        long period = 0;
        long initialDelay = 0;
        TimeUnit unit = TimeUnit.MILLISECONDS;

        switch (values.length) {
            case 1: {
                delay = Long.parseLong(values[0].trim());
                methodName = "schedule";
                break;
            }
            case 2: {
                methodName = "scheduleAtFixedRate";
                initialDelay = Long.parseLong(values[0].trim());
                period = Long.parseLong(values[1].trim());
                break;
            }
            default:
                break;
        }

        infoMap.put("method", methodName);
        infoMap.put("delay", delay);
        infoMap.put("period", period);
        infoMap.put("initialDelay", initialDelay);
        infoMap.put("unit", unit);
        return infoMap;
    }


    public static Map<String, Object> parseForTimerTask(String timeExpression) {
        Map<String, Object> infoMap = new HashMap<String, Object>();
        String values[] = StringUtils.split(timeExpression, ',');
        Date firstTime = new Date();
        long delay = 0;
        long period = 0;

        switch (values.length) {
            case 1: {
                delay = Long.parseLong(values[0].trim());
                break;
            }
            case 2: {
                delay = Long.parseLong(values[0].trim());
                period = Long.parseLong(values[1].trim());
                break;
            }
            case 3: {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, new Integer(values[0].trim()));
                cal.set(Calendar.MINUTE, new Integer(values[1].trim()));
                if (cal.before(Calendar.getInstance())) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                period = Long.parseLong(values[2].trim());
                break;
            }
            case 4: {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, new Integer(values[0].trim()));
                cal.set(Calendar.HOUR_OF_DAY, new Integer(values[1].trim()));
                cal.set(Calendar.MINUTE, new Integer(values[2].trim()));
                if (cal.before(Calendar.getInstance())) {
                    cal.add(Calendar.WEEK_OF_MONTH, 1);
                }
                period = Long.parseLong(values[3].trim());
                break;
            }
            default:
                break;
        }

        infoMap.put("firstTime", firstTime);
        infoMap.put("period", period);
        infoMap.put("delay", delay);
        infoMap.put("case", values.length);
        return infoMap;
    }
}
