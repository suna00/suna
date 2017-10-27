package net.ion.ice.cjmwave.external.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CommonDateUtils {

    /**
     * 현재 날짜 월요일, 일요일
     * @param dayOfWeek (월요일:Monday, 일요일:Sunday)
     * @param voteDate (YYYYMMDD)
     * @return
     */
    public static String getCurMondayOrSunday(String dayOfWeek, String voteDate){
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();

        SimpleDateFormat transForm = new SimpleDateFormat("yyyyMMdd");
        Date vD = null;

        if(!voteDate.equals("") && voteDate != null) {
            try {
                vD = transForm.parse(voteDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            c.setTime(vD);
        }

        if(dayOfWeek.equals("Monday")) {
            c.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
        } else if(dayOfWeek.equals("Sunday")){
            c.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);
            c.add(c.DATE,7);
        }

        return formatter.format(c.getTime());
    }
}
