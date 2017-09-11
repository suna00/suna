package net.ion.ice.core.file;

import net.ion.ice.core.node.PropertyType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by jaeho on 2017. 6. 28..
 */
public interface FileRepository {

    FileValue saveMutipartFile(PropertyType pt, String id, MultipartFile multipartFile);

    Resource loadAsResource(String path);

    FileValue saveResourceFile(PropertyType pt, String id, String path);
}
