package net.ion.ice.core.api;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class ApiUtils {
    public static final String GET = "GET" ;
    public static final String POST = "POST";
    public static final String UTF_8 = "UTF-8";
    private static final String LINE_FEED = "\r\n";

    public static HttpURLConnection getUrlConnection(String apiUrl, Map<String, Object> data, int connectTimeout, int readTimeout, String method) throws IOException {
        if (!apiUrl.startsWith("http"))
            apiUrl = "http://" + apiUrl;
        URL url = new URL(apiUrl);

        HttpURLConnection conn;
        conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(readTimeout);
        conn.setConnectTimeout(connectTimeout);

        boolean isMultipart = false ;

        if(StringUtils.isEmpty(method)){
//            method = StringUtils.isEmpty(queryString) ? GET : POST ;
            method = POST ;
        }else if(method.equalsIgnoreCase("multipart")){
            isMultipart = true ;
            method = POST ;
        }

        if(isMultipart){
            String boundary = "----WebKitFormBoundaryIce" + System.currentTimeMillis() ;
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod(method.toUpperCase());
            conn.setRequestProperty("accept-encoding", "gzip");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream os = null;
            BufferedWriter writer = null;
            try{
                os = conn.getOutputStream();
                writer = new BufferedWriter(new OutputStreamWriter(os, UTF_8));
//				writer = new StringWriter();

                for(String key : data.keySet()){
                    Object value = data.get(key) ;
                    if(value instanceof MultipartFile){
                        String fileName = ((MultipartFile)value).getOriginalFilename();
                        writer.append("--" + boundary).append(LINE_FEED);
                        writer.append(
                                "Content-Disposition: form-data; name=\"" + key  + "\"; filename=\"" + fileName + "\"")
                                .append(LINE_FEED);

                        String contentType = URLConnection.guessContentTypeFromName(fileName);

                        writer.append("Content-Type: " + (StringUtils.isEmpty(contentType) ? ContentType.TEXT_PLAIN.getMimeType() : contentType))
                                .append(LINE_FEED);
//				        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
                        writer.append(LINE_FEED);
                        writer.flush();

                        InputStream inputStream = ((MultipartFile)value).getInputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead = -1;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        os.flush();
                        inputStream.close();

                        writer.append(LINE_FEED);
                        writer.flush();
                    }else{
                        writer.append("--" + boundary).append(LINE_FEED);
                        writer.append("Content-Disposition: form-data; name=\"" + key + "\"")
                                .append(LINE_FEED);
//				        writer.append("Content-Type: text/plain; charset=utf-8").append(LINE_FEED);
                        writer.append(LINE_FEED);
                        writer.append(value.toString()).append(LINE_FEED);
                        writer.flush();
                    }
                }
                writer.append(LINE_FEED).flush();
                writer.append("--" + boundary + "--").append(LINE_FEED);
            }finally{
                if(writer != null) writer.close();
                if(os!= null) os.close();
            }
        }else{
            String paramQuery = getQueryString(data) ;
            if(paramQuery == null){
                paramQuery = "";
            }

            if(!method.equalsIgnoreCase(GET)){
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod(method.toUpperCase());
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("accept-encoding", "gzip");
                OutputStream os = null;
                BufferedWriter writer = null;
                try{
                    os = conn.getOutputStream();
                    writer = new BufferedWriter(new OutputStreamWriter(os, UTF_8));
                    writer.write(paramQuery);
                }finally{
                    if(writer != null) writer.close();
                    if(os!= null) os.close();
                }
            }else{
                conn.setRequestMethod(method.toUpperCase());
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("accept-encoding", "gzip");
            }
            conn.connect();
        }
        return conn;
    }


    public static String callApiMethod(String apiUrl, Map<String, Object> data, int connectTimeout, int readTimeout, String method) throws IOException {
        InputStream in = null;
        HttpURLConnection conn = null;
        try {
            conn = getUrlConnection(apiUrl, data, connectTimeout, readTimeout, method);
            if(conn.getResponseCode() > 299){
//                logger.info("RESPONSE : " + conn.getResponseCode()) ;

                if ("gzip".equals(conn.getContentEncoding())) {
                    in = new GZIPInputStream(conn.getErrorStream());
                    throw new ApiException(conn.getResponseCode() + "", IOUtils.toString(in, UTF_8)) ;
                } else {
                    in = conn.getErrorStream();
                    throw new ApiException(conn.getResponseCode() + "", IOUtils.toString(in, UTF_8)) ;
                }
            }else{
                if ("gzip".equals(conn.getContentEncoding())) {
                    in = new GZIPInputStream(conn.getInputStream());
                    return IOUtils.toString(in, UTF_8);
                } else {
                    in = conn.getInputStream();
                    return IOUtils.toString(in, UTF_8);
                }
            }
        } finally {
            if (in != null)
                in.close();
            if (conn != null)
                conn.disconnect();
        }
    }


    public static String getQueryString(Map<String, Object> data) throws UnsupportedEncodingException {
        if(data == null || data.size() == 0) return "" ;
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (String key : data.keySet()) {
            Object value = data.get(key) ;
            if(value == null) continue;
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), UTF_8));
        }

        return result.toString();
    }
}
