package net.ion.ice.core.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * Created by jaeho on 2017. 6. 29..
 */
public class FileValue implements Serializable{
    private String contentType;
    private String storePath  ;
    private String fileName  ;
    private Long fileSize ;

    public FileValue(MultipartFile multipartFile, String saveFilePath) {
        this.storePath = saveFilePath ;
        this.fileName = multipartFile.getOriginalFilename() ;
        this.contentType = multipartFile.getContentType() ;
        this.fileSize = multipartFile.getSize() ;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStorePath() {
        return storePath;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }
}
