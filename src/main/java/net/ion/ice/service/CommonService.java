package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;

import java.util.HashMap;
import java.util.Map;

public class CommonService {

    public static boolean requiredParams(ExecuteContext context, Map<String, Object> data, String[] params) {
        for(String str : params){
            if(data.get(str) == null){
                Map<String, Object> object = new HashMap<>();
                object.put("error", "required param. " + str);
                context.setResult(object);
                return true;
            }
        }
        return false;
    }

}
