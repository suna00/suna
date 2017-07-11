package net.ion.ice.security.common;

import org.springframework.http.HttpStatus;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ErrorResponse {
    private final HttpStatus status;
    private final String message;
    private final String timestamp;
    private final ErrorCode code;

    protected ErrorResponse(final String message, HttpStatus status, ErrorCode code) {
        this.message = message;
        this.status = status;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.code = code;
    }

    public static ErrorResponse of(final String message,  HttpStatus status, ErrorCode code) {
        return new ErrorResponse(message, status, code);    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public ErrorCode getCode() {
        return code;
    }

}
