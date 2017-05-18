package net.ion.ice.core.response;

/**
 * Created by jaeho on 2017. 5. 18..
 */
public class JsonErrorResponse extends JsonResponse {

    public JsonErrorResponse(String resultCode, Exception e) {
        this.result = resultCode ;
        this.resultMessage = e.getMessage() ;

    }

    public JsonErrorResponse(Exception e) {
        this("500", e) ;
    }
}
