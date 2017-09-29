package net.ion.ice.cjmwave.mobile.push;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 29..
 */
public class PushUtils {

    /*
    * 대상이 복수라면 targets 에 comma String 이 와야 함
    * */
    public static Map<String, Object> getFcmMessage(String targets
            , String title, String body, String image, Map<String, Object> customInfo){
        Map<String, Object> organizedMessage = new HashMap<String, Object>();
        Map<String, Object> dataMap = new HashMap<String, Object> ();

        dataMap.put("title", title);
        dataMap.put("body", body);
        dataMap.put("image", image);
        if(customInfo != null) dataMap.put("custom", customInfo);
//        organizedMessage.put("registration_ids", targets);
        organizedMessage.put("to", targets);
        organizedMessage.put("data", dataMap);
        return organizedMessage;
    }
}
