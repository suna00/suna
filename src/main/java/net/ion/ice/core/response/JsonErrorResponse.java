package net.ion.ice.core.response;

import net.ion.ice.core.api.ApiException;

/**
 * Created by jaeho on 2017. 5. 18..
 */
public class JsonErrorResponse extends JsonResponse {

    public JsonErrorResponse(String resultCode, Exception e) {
        this.result = resultCode ;
        this.resultMessage = e.getMessage() ;

    }

    public JsonErrorResponse(Exception e) {
        if(e instanceof ApiException){
             this.result = ((ApiException) e).getResultCode() ;
        }else{
            this.result = "500" ;
        }
        this.resultMessage = e.getMessage() ;
    }
}
