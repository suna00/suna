package net.ion.ice.core.file;

/**
 * Created by jaeho on 2017. 6. 28..
 */

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * A conditional configuration that potentially adds the bean definitions in
 * this class to the Spring application context, depending on whether theㅁㅁ
 * {@code @ConditionalOnExpression} is true or not.
 *
 */
@Configuration
@ConfigurationProperties(prefix = "file.default")
public class DefaultFileRepository implements FileRepository{
    private String path ;
    private File fileRoot ;

    @PostConstruct
    public void register(){
        fileRoot = new File(path) ;
        if(!fileRoot.exists()){
            fileRoot.mkdirs() ;
        }

        ApplicationContextManager.getBean(FileService.class).registerRepository("default", this) ;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String saveMutipartFile(PropertyType pt, String id, MultipartFile multipartFile ) {
        String savePath = pt.getTid() + "/" +  pt.getPid() + "/" + DateFormatUtils.format(new Date(), "yyyyMM/dd/") + UUID.randomUUID() + "." + StringUtils.substringAfterLast(multipartFile.getOriginalFilename(), ".");
        File saveFile = new File(fileRoot, savePath) ;
        try {
            if(!saveFile.getParentFile().exists()){
                saveFile.getParentFile().mkdirs() ;
            }
            multipartFile.transferTo(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("FILE SAVE ERROR : " + e.getMessage()) ;
        }

        return savePath ;
    }

    @Override
    public Resource loadAsResource(String path) {
        File file = new File(fileRoot, path) ;
        return new FileSystemResource(file);
    }
}
