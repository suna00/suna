package net.ion.ice.core.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 6. 28..
 */

@Service("fileService")
public class FileService {
    private static Logger logger = LoggerFactory.getLogger(FileService.class);


    @Autowired
    private NodeService nodeService ;

    private Map<String, FileRepository> repositoryMap = new ConcurrentHashMap<>() ;

    private ObjectMapper objectMapper = new ObjectMapper();

    public void registerRepository(String repositoryKey, FileRepository repository) {
        repositoryMap.put(repositoryKey, repository) ;
    }


    public FileValue saveMultipartFile(PropertyType pt, String id, MultipartFile multipartFile) {
        logger.info("Save Multipart File : property = {}, id = {}, param = {}, file = {} ", pt.getPid(), id, multipartFile.getName(), multipartFile.getOriginalFilename());
        try {
            FileRepository repository = getFileRepository(pt.getFileHandler());
            return repository.saveMutipartFile(pt, id, multipartFile);
        }catch(Exception e){
            logger.error("Multipart File save error : property = {}, param = {}, file = {}, error : {}", pt.getPid(), multipartFile.getName(), multipartFile.getOriginalFilename(), e.getMessage());
            throw e ;
        }
    }

    public FileValue saveFile(PropertyType pt, String id, File file, String fileName, String contentType) {
        FileRepository repository = getFileRepository(pt.getFileHandler()) ;
        return repository.saveFile(pt, id, file, fileName, contentType) ;
    }

    public FileValue fileValueMapper(String value) {
        FileValue fileValue = null;
        try {
            fileValue = objectMapper.readValue(value, FileValue.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileValue;
    }

    private FileRepository getFileRepository(String fileHandler) {
        if(StringUtils.isEmpty(fileHandler)){
            fileHandler = "default" ;
        }

        FileRepository repository = repositoryMap.get(fileHandler) ;
        if(repository == null && fileHandler.equals("default")){
            repository = ApplicationContextManager.getBean(DefaultFileRepository.class) ;
        }
        if(repository == null){
            repository = ApplicationContextManager.getBean(DefaultFileRepository.class) ;
        }
        return repository ;
    }

    public Resource loadAsResource(String tid, String pid, String path) {
        PropertyType pt = nodeService.getNodeType(tid).getPropertyType(pid) ;

        FileRepository repository = getFileRepository(pt.getFileHandler()) ;
        return repository.loadAsResource(path) ;
    }


    public FileValue saveResourceFile(PropertyType pt, String id, String path) {
        FileRepository repository = getFileRepository(pt.getFileHandler()) ;
        return repository.saveResourceFile(pt, id, path) ;
    }
}
