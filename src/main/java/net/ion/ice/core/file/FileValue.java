package net.ion.ice.core.file;

import net.ion.ice.core.node.PropertyType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by jaeho on 2017. 6. 29..
 */
public class FileValue implements Serializable{
    private String handler ;
    private String contentType;
    private String storePath  ;
    private String fileName  ;
    private Long fileSize ;

    public FileValue(PropertyType pt, String id, MultipartFile multipartFile, String saveFilePath) {
        this.handler = pt.getFileHandler() ;
        this.storePath = saveFilePath ;
        this.fileName = multipartFile.getOriginalFilename() ;
        this.contentType = multipartFile.getContentType() ;
        this.fileSize = multipartFile.getSize() ;
    }

    public FileValue(PropertyType pt, Resource res, String path) {
        this.handler = pt.getFileHandler() ;
        this.storePath = path ;
        this.fileName = res.getFilename() ;
        try {
            this.fileSize = res.contentLength() ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.contentType = FileUtils.getContentType(res.getFilename()) ;
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

    public String toString(){
        return this.storePath ;
    }
}
