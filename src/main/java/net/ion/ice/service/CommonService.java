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
        resultCodeMap.put("V0002", "[발급수제한] 쿠폰 수량이 모두 소진되었습니다.");
        resultCodeMap.put("V0003", "[동일인재발급제한] 받을 수 있는 쿠폰이 없습니다.");
        resultCodeMap.put("V0004", "발급가능한 기간이 만료되었습니다.");
        resultCodeMap.put("V0005", "이미 발급된 쿠폰입니다.");

        /*Order (O0000)*/

        /*Cart (C0000)*/
        resultCodeMap.put("C0001", "상품 삭제 성공");
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
        Map<String, Object> object2 = new HashMap<>();
        object2.put("code", code);
        object2.put("message", resultCodeMap.get(code));
        object.put("validate", object2);
        context.setResult(object);
    }

    public static void setErrorMessage(ExecuteContext context, String code, String message) {
        Map<String, Object> object = new HashMap<>();
        Map<String, Object> object2 = new HashMap<>();
        object2.put("code", code);
        object2.put("message", resultCodeMap.get(code) + " " + message);
        object.put("validate", object2);
        context.setResult(object);
    }

    public static void setErrorMessageAlert(ExecuteContext context, String code, String message) {
        Map<String, Object> object = new HashMap<>();
        Map<String, Object> object2 = new HashMap<>();
        object2.put("code", code);
        object2.put("message", message);
        object.put("validate", object2);
        context.setResult(object);
    }

}
