package net.ion.ice.cjmwave.external.utils;

import java.io.File;
import java.net.URL;
import java.util.UUID;

/**
 * Created by juneyoungoh on 2017. 9. 18..
 */
public class FileUtils {

    /*
    * URL 로 파일을 요청하고 파일객체 반환
    * */
    public static File retrieveRemoteFile (String basicPath, String url) {
        File file = null;
        UUID uuid = UUID.randomUUID();
        try{
            file = new File(basicPath + "/" + uuid.toString() + ".jpg");
            URL requestUrl = new URL(url);
            int connectionTimeout = 3000, readTimeout = 60000;
            org.apache.commons.io.FileUtils.copyURLToFile(requestUrl, file, connectionTimeout, readTimeout);
        } catch (Exception e) {
            // file retrieve 실패 null 을 반환함
            file = null;
        }
        return file;
    }

    /*
    * 이 부분의 로직은 고객이 전달한 부분임
    * */
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
};