package net.ion.ice.cjmwave.external.utils;

import java.io.File;
import java.net.URL;

/**
 * Created by juneyoungoh on 2017. 9. 18..
 */
public class FileUtils {

    /*
    * URL 로 파일을 요청하고 파일객체 반환
    * */
    public File retrieveRemoteFile (String url) throws Exception {
        URL requestUrl = new URL(url);
        File file = null;
        org.apache.commons.io.FileUtils.copyURLToFile(requestUrl, file);
        return file;
    }
}
