package net.ion.ice.core.api;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

public class RequestParameter implements Serializable{
    private String paramName ;
    private String paramValue ;

    private Boolean required ;
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public String getParamName() {
        return paramName;
    }

    public String getParamValue() {
        return paramValue;
    }
}
