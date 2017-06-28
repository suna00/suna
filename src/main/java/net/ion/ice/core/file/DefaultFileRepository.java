package net.ion.ice.core.file;

/**
 * Created by jaeho on 2017. 6. 28..
 */

import net.ion.ice.ApplicationContextManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * A conditional configuration that potentially adds the bean definitions in
 * this class to the Spring application context, depending on whether the
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
    public String saveMutipartFile(String tid, String pid, MultipartFile multipartFile ) {
        String savePath = tid + "/" ;
        File saveFile = new File(fileRoot, savePath) ;
//        multipartFile.transferTo(saveFile);

        return saveFile.getAbsolutePath();
    }
}
