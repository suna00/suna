package net.ion.ice.core.file;

import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.PropertyType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.stagemonitor.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 6. 28..
 */

@Service
public class FileService {

    @Autowired
    private InfinispanRepositoryService infinispanRepositoryService ;

    private Map<String, FileRepository> repositoryMap = new ConcurrentHashMap<>() ;

    public void registerRepository(String repositoryKey, FileRepository repository) {
        repositoryMap.put(repositoryKey, repository) ;
    }


    public FileValue saveMultipartFile(PropertyType pt, String id, MultipartFile multipartFile) {
        FileRepository repository = getFileRepository(pt.getFileHandler()) ;
        String saveFilePath = repository.saveMutipartFile(pt, id, multipartFile) ;
        return new FileValue(pt, id, multipartFile, saveFilePath);
    }

    private FileRepository getFileRepository(String fileHandler) {
        if(StringUtils.isEmpty(fileHandler)){
            fileHandler = "default" ;
        }

        return repositoryMap.get(fileHandler) ;
    }
}
