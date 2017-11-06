package net.ion.ice.plugin.openApi;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@ConfigurationProperties(prefix = "openApiNaver")
@Service("naverService")
public class NaverService {
    private Logger logger = LoggerFactory.getLogger(NaverService.class);

    private String clientId;
    private String clientSecret;

    public String getClientId() { return clientId; }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() { return clientSecret; }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }


    public String requestApi(String method, String apiUrl, String params) {
        String result = "";

        if (StringUtils.contains(apiUrl, "?")) apiUrl = StringUtils.substringBefore(apiUrl, "?");

        try {
            List<String> encodeParamList = new ArrayList<>();
            List<String> paramList = Arrays.asList(StringUtils.split(params, "&"));
            for (String param : paramList) {
                String[] splitParam = StringUtils.split(param, "=");
                String paramName = splitParam.length >= 1 ? splitParam[0] : "";
                String paramValue = splitParam.length >= 2 ? splitParam[1] : "";

                if (!StringUtils.isEmpty(paramName)) {
                    String encodeParamValue = StringUtils.isEmpty(paramValue) ? "" : URLEncoder.encode(paramValue, "UTF-8");
                    encodeParamList.add(paramName+"="+encodeParamValue);
                }
            }

            URL url;
            HttpURLConnection con;

            if (StringUtils.equalsIgnoreCase(method, "post")) {
                url = new URL(apiUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(con.getOutputStream());
                outputStreamWriter.write(params.toString());
                outputStreamWriter.flush();
            } else {
                apiUrl = apiUrl+'?'+StringUtils.join(encodeParamList, "&");
                url = new URL(apiUrl);
                con = (HttpURLConnection) url.openConnection();
            }

            con.setRequestMethod(method);
            con.setRequestProperty("X-Naver-Client-Id", getClientId());
            con.setRequestProperty("X-Naver-Client-Secret", getClientSecret());
            con.getResponseCode();

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) { response.append(inputLine); }
            br.close();

            result = response.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return result;
    }
}
