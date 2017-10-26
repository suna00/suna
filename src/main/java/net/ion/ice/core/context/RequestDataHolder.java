package net.ion.ice.core.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestDataHolder {

    private static final ThreadLocal<Map<String, Object>> requests = new ThreadLocal<>() ;


    public static void initRequestData(Map<String, Object> data){
        requests.set(data);
    }

    public static Map<String, Object> getRequestData(){
        return requests.get() ;
    }


    public static Object getRequestDataValue(String key){
        return requests.get() == null ? null : requests.get().get(key) ;
    }

    public static void setRequestDataValue(String key, Object value){
        if(requests.get() == null){
            requests.set(new ConcurrentHashMap<>());
        }
        if(value == null) return  ;

        requests.get().put(key, value) ;
    }

    public static void clearRequestData(){
        requests.set(null);
    }
}
