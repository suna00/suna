package net.ion.ice.cjmwave.editor;

import net.ion.ice.core.file.amazon.S3FileRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by juneyoungoh on 2017. 11. 10..
 */
@Service
public class FrontFileService {
    private Logger logger = Logger.getLogger(FrontFileService.class);

    @Autowired
    S3FileRepository s3Repo;

    public List<Map<String, Object>> uploadFiles(MultipartHttpServletRequest request){
        List<Map<String, Object>> s3FileInfos = new ArrayList<>();
        Iterator<String> itr = request.getFileNames();
        while (itr.hasNext()) {
            Map<String, Object> s3MultiFileInfo = new HashMap();
            MultipartFile mpf = request.getFile(itr.next());
            String s3Path = s3Repo.saveFrontMultiPartFile(mpf);
            s3MultiFileInfo.put("url", s3Path);
            s3FileInfos.add(s3MultiFileInfo);
        }
        return s3FileInfos;
    }
}
