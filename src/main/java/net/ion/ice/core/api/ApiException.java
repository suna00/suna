package net.ion.ice.core.api;

import net.ion.ice.IceRuntimeException;

public class ApiException extends IceRuntimeException {
    private String resultCode ;


    public ApiException(String resultCode, String message){
        super(message);
        this.resultCode = resultCode ;
    }

    public ApiException(String resultCode, String message, Exception e){
        super(message, e);
        this.resultCode = resultCode ;
    }


    public String getResultCode() {
        return resultCode;
    }
}
