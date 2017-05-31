package net.ion.ice.core.response;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.QueryResult;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class JsonResponse implements Serializable{
    protected String result ;
    protected String resultMessage ;

    protected Node item ;

    public static JsonResponse create(Node node){
        return new JsonObjectResponse(node) ;
    }

    public static JsonResponse create(Collection<Map<String, Object>> list){
        return new JsonArrayResponse(list);
    }

    public static JsonResponse create(QueryResult queryResult){
        if(queryResult.isPaging()){
            return new JsonArrayPagingResponse(queryResult);
        }else{
            return new JsonArrayResponse(queryResult);
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
}
