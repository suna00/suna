package net.ion.ice.json;

import net.ion.ice.CoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Created by jaehocho on 2017. 2. 9..
 */
@Service("jsonRepositoryService")
public class JsonRepositoryService {
    private static Logger logger = LoggerFactory.getLogger(JsonRepositoryService.class);

    @Autowired
    private CoreConfig config ;
    private File rootDir  ;

    public JsonRepositoryService(){
        initRepository() ;
    }

    private void initRepository() {
        rootDir = new File((String) config.getConfigValue("json-path")) ;
        if(!rootDir.exists()){
            rootDir.mkdirs() ;
        }
    }

}
