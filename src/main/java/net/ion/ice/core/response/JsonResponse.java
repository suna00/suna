package net.ion.ice.core.response;

import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.query.SimpleQueryResult;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonResponse implements Serializable{
    protected String result ;
    protected String resultMessage;

    public static JsonResponse create(){
        return new JsonBasicResponse() ;
    }
    public static JsonResponse create(Node node){
        return new JsonObjectResponse(node) ;
    }

    public static JsonResponse create(Map<String, Object> map){
        return new JsonMapResponse(map) ;
    }

    public static JsonResponse create(Collection<Map<String, Object>> list){
        return new JsonArrayResponse(list);
    }

    public static JsonResponse create(SimpleQueryResult simpleQueryResult){
        if(simpleQueryResult.isPaging()) {
            return new JsonArrayPagingResponse(simpleQueryResult);
        }else if(simpleQueryResult.isTree()){
            return new JsonTreeResponse(simpleQueryResult) ;
        }else{
            return new JsonArrayResponse(simpleQueryResult);
        }
    }

    public static JsonResponse error(Exception e) {
        return new JsonErrorResponse(e) ;
    }

    public String getResult(){
        return result ;
    }

    public void setResult(String result){
        this.result = result ;
    }

    public String getResultMessage(){
        return resultMessage ;
    }

    public static JsonResponse createValueResponse(Object value) {
        return new JsonValueResponse(value) ;
    }
}
