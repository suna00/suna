package net.ion.ice.cjmwave.external.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 5..
 */
public class CommonNetworkUtils {

    public static String MapToString(Map paramMap) {
        String paramStr = "";
        Iterator iter = paramMap.keySet().iterator();
        while(iter.hasNext()) {
            String k = String.valueOf(iter.next());
            String v = String.valueOf(paramMap.get(k));
            paramStr = paramStr + k + "=" + v + "&";
        }
        return paramStr;
    }

    public static Map<String, String> RequestMapToMap(HttpServletRequest request) {
        Map<String, String[]> requestMap = request.getParameterMap();
        Map<String, String> rtn = new HashMap<>();
        Iterator<String> iter = requestMap.keySet().iterator();
        while(iter.hasNext()) {
            String k = iter.next();
            String v = String.valueOf(requestMap.get(k)[0]);
            rtn.put(k, v);
        }
        return rtn;
    }

    public static String RequestMapToString(HttpServletRequest request) {
        Map<String, String[]> requestMap = request.getParameterMap();
        String paramStr = "";
        Iterator<String> iter = requestMap.keySet().iterator();
        while(iter.hasNext()) {
            String k = iter.next();
            String v = String.valueOf(requestMap.get(k)[0]);
            paramStr = paramStr + k + "=" + v + "&";
        }
        return paramStr;
    }
}
