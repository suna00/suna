package net.ion.ice.core.response;

import net.ion.ice.core.node.QueryResult;

import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonResponse {
    protected String result ;
    protected String resultMessage ;


    public static JsonResponse create(Map<String, Object> date){
        return new JsonObjectResponse() ;
    }

    public static JsonResponse create(Collection<Map<String, Object>> list){
        return new JsonArrayResponse(list);
    }

    public static JsonResponse create(QueryResult queryResult){
        return new JsonArrayResponse(queryResult);
    }

    public static JsonResponse error(Exception e) {
        return new JsonErrorResponse(e) ;
    }
}
