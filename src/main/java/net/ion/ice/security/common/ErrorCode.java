package net.ion.ice.security.common;

/**
 * Created by seonwoong on 2017. 6. 15..
 */
public enum ErrorCode {

    AUTHENTICATION(100), JWT_TOKEN_EXPIRED(200);

    private int errorCode;

    private ErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}
