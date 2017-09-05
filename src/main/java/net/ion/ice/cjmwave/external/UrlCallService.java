package net.ion.ice.cjmwave.external;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


/**
 * Created by juneyoungoh on 2017. 9. 4..
 * 이걸 상속 받아서 {연동대상명}UrlCallService 를 만듬
 * ex > SKBUrlCallService
 *
 * nono => 구현 먼저 가고 고도화 가자
 * 일단 프로그램 / 영상 먼저 가자
 */

@Service
public class UrlCallService {
    Logger logger = Logger.getLogger(UrlCallService.class);

    public List fetchJSON (String requestUrl) throws Exception {
        return fetchJSON(requestUrl, null);
    }

    public List fetchJSON (String requestUrl, String paramStr) throws Exception {
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

}
