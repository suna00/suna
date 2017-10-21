package net.ion.ice.core.file;

public class FileUtils {

    public static String getContentType(String name){
        int i = name.lastIndexOf('.');

        if (i < 0)
            return "application/octet-stream";
        else if (name.endsWith(".txt"))
            return "text/plain";
        else if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        else if (name.endsWith(".gif"))
            return "image/gif";
        else if (name.endsWith(".tif") || name.endsWith(".tiff"))
            return "image/tiff";
        else if (name.endsWith(".png"))
            return "image/png";
        else if (name.endsWith(".htm") || name.endsWith(".html"))
            return "text/html";
        else if (name.endsWith(".xml"))
            return "text/xml";
        else
            return "application/octet-stream";
    }

    public static boolean isAttach(String name){
        String contentType = getContentType(name) ;
        switch(contentType){
            case "application/octet-stream" :
                return true ;
            default:
                return false ;
        }
    }

}
