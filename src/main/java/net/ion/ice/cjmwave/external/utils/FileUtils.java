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
    public static File retrieveRemoteFile (String url) throws Exception {
        URL requestUrl = new URL(url);
        File file = null;
        int connectionTimeout = 3000, readTimeout = 60000;
        org.apache.commons.io.FileUtils.copyURLToFile(requestUrl, file, connectionTimeout, readTimeout);
        return file;
    }


    public static String getMnetFileUrl (String mediaId, String mediaType, String sizeType) {
        String imgUrl = "http://cmsimg.global.mnet.com/clipimage";
        String strClipName = "";

        String upperCased = mediaType.toUpperCase();
        if("ALBUM".equals(upperCased) || "ARTIST".equals(upperCased)) {
            mediaType = upperCased.toLowerCase();
        } else if ("VOD".equals(upperCased) || "PROGRAM".equals(upperCased)) {
            mediaType = "vod";
        }

        strClipName = "0000000000" + mediaId;
        int sLen = strClipName.length() - 9;
        strClipName = strClipName.substring(sLen);
        return imgUrl + "/" + mediaType
                + "/" + sizeType
                + "/" + strClipName.substring(0, 3)
                + "/" + strClipName.substring(3, 6)
                + "/" + mediaId + ".jpg";
    }

}
