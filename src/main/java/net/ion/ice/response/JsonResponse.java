package net.ion.ice.response;

import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonResponse {

    public static JsonResponse create(Map<String, Object> date){
        return new JsonObjectResponse() ;
    }

    public static JsonResponse create(Collection<Map<String, Object>> list){
        return new JsonArrayResponse(list);
    }
}
