package net.ion.ice.cjmwave.external.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by juneyoungoh on 2017. 9. 5..
 */
public class JSONNetworkUtils {

    private static Logger logger = Logger.getLogger(JSONNetworkUtils.class);

    public static List fetchJSON (String requestUrl) throws Exception {
        return fetchJSON(requestUrl, null);
    }

    public static List fetchJSON (String requestUrl, String paramStr) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List rsList = null;

        URL url = new URL((paramStr == null) ? requestUrl : requestUrl + "?" + paramStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));


        String aLine = "";
        StringBuilder sb = new StringBuilder();
        while((aLine = reader.readLine()) != null) {
            sb.append(aLine);
        }

        reader.close();
        logger.info("------------------------------- " + sb.toString());
        rsList = mapper.readValue(sb.toString(), List.class);
        return rsList;
    }
};