package net.ion.ice.security.common;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.util.Date;

@Data
public class ErrorResponse {
    private final HttpStatus status;
    private final String message;
    private final Date timestamp;
    private final ErrorCode code;

    protected ErrorResponse(final String message, HttpStatus status, ErrorCode code) {
        this.message = message;
        this.status = status;
        this.timestamp = new Date();
        this.code = code;
    }

    public static ErrorResponse of(final String message,  HttpStatus status, ErrorCode code) {
        return new ErrorResponse(message, status, code);    }

}
