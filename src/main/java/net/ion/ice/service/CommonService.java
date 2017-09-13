package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;

import java.util.HashMap;
import java.util.Map;

public class CommonService {

    public static final Map<String, Object> resultCodeMap;
    static
    {
        resultCodeMap = new HashMap<String, Object>();
        resultCodeMap.put("S0001", "required param. ");

        resultCodeMap.put("V0001", "존재하지 않는 쿠폰유형입니다.");
        resultCodeMap.put("V0002", "발급가능한 쿠폰 수량이 모두 소진되었습니다.");
    }

    public static boolean requiredParams(ExecuteContext context, Map<String, Object> data, String[] params) {
        for(String str : params){
            if(data.get(str) == null){
                setErrorMessage(context, "S0001", str);
                return true;
            }
        }
        return false;
    }

    public static void setErrorMessage(ExecuteContext context,  String code) {
        Map<String, Object> object = new HashMap<>();
        object.put("errorCode", code);
        object.put("errorMessage", resultCodeMap.get(code));
        context.setResult(object);
    }

    public static void setErrorMessage(ExecuteContext context, String code, String message) {
        Map<String, Object> object = new HashMap<>();
        object.put("errorCode", code);
        object.put("errorMessage", resultCodeMap.get(code) + " " + message);
        context.setResult(object);
    }

}
