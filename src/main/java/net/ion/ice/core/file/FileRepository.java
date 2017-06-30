package net.ion.ice.core.file;

import net.ion.ice.core.node.PropertyType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Created by jaeho on 2017. 6. 28..
 */
public interface FileRepository {

    String saveMutipartFile(PropertyType pt, String id, MultipartFile multipartFile);
}
