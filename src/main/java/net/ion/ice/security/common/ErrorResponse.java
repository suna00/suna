package net.ion.ice.security.common;

import org.springframework.http.HttpStatus;

import java.util.Date;

public class ErrorResponse {
    private final HttpStatus status;

    private final String message;

    private final Date timestamp;

    protected ErrorResponse(final String message, HttpStatus status) {
        this.message = message;
        this.status = status;
        this.timestamp = new Date();
    }

    public static ErrorResponse of(final String message,  HttpStatus status) {
        return new ErrorResponse(message, status);
    }

    public Integer getStatus() {
        return status.value();
    }

    public String getMessage() {
        return message;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
