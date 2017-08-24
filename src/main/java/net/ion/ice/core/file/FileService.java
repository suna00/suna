package net.ion.ice.core.file;

import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jaeho on 2017. 6. 28..
 */

@Service
public class FileService {

    @Autowired
    private InfinispanRepositoryService infinispanRepositoryService ;

    @Autowired
    private NodeService nodeService ;


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

    public Resource loadAsResource(String tid, String pid, String path) {
        PropertyType pt = nodeService.getNodeType(tid).getPropertyType(pid) ;

        FileRepository repository = getFileRepository(pt.getFileHandler()) ;
        return repository.loadAsResource(path) ;
    }

    public FileValue getFileInfo(PropertyType pt, String id, String value) {
        FileRepository repository = getFileRepository(pt.getFileHandler()) ;

        if(StringUtils.contains((String) value, "classpath:")){
            FileValue fileValue = repository.getClasspathFileInfo(pt, id, value) ;
            return fileValue ;
        }
        return null ;
    }
}
