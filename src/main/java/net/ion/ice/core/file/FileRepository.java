package net.ion.ice.core.file;

import net.ion.ice.core.node.PropertyType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * Created by jaeho on 2017. 6. 28..
 */
public interface FileRepository {

    FileValue saveMutipartFile(PropertyType pt, String id, MultipartFile multipartFile);

    FileValue saveFile(PropertyType pt, String id, File file, String fileName, String contentType);

    Resource loadAsResource(String path);

    FileValue saveResourceFile(PropertyType pt, String id, String path);
}
