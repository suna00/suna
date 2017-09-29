package net.ion.ice.cjmwave.mobile.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 29..
 */
@Service
public class FCMPushService implements MobilePushService <String, Map, String, Ice2PushException>  {

    private Logger logger = Logger.getLogger(FCMPushService.class);
    private String fcmApiUrl;
    private String fcmApiKey;

    @Autowired
    Environment env;

    @PostConstruct
    public void init(){
        try {
            fcmApiKey = env.getProperty("mobile.push.fcmApiKey");
            fcmApiUrl = env.getProperty("mobile.push.fcmApiUrl");
        } catch (Exception e) {
            logger.error("Failed to initialize FCM Service. Main cause ::" + e.getClass().getName());
        }
    }

    /*
    * target - 일반적으로는 사용자 단말 ID 를 comma string 으로 전송함
    * messageObject - Map 을 넣도록 설계. title
    * return 되는 String 은 JSON 문자열임
    * */
    @Override
    public String sendMessageToTargets(String target, Map messageObject) throws Ice2PushException {
        String response = "";
        try{

            URL fcmHttpUrl = new URL(fcmApiUrl);
            HttpURLConnection conn = (HttpURLConnection) fcmHttpUrl.openConnection();
            conn.setRequestMethod("POST");

            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestProperty("Authorization", "key=" + fcmApiKey);
            conn.setRequestProperty("Content-Type", "application/json");

            String title = String.valueOf(messageObject.get("title"));              // 푸시 메세지의 제목
            String description = String.valueOf(messageObject.get("description"));  // 푸시 메세지의 내용
            String image = String.valueOf(messageObject.get("image"));              // 푸시에 포함될 이미지 URL.
            Map customInfo = null;

            if(messageObject.containsKey("custom")) {                           // 푸시 메세지에 추가적으로 들어갈 정보. 가령 이벤트 푸시에 이벤트 정보를 넣어서 모바일에서 해당 정보를 참조하게 하는 용도로 사용
                if(messageObject.get("custom") != null) {
                    customInfo = (Map) messageObject.get("custom");
                }
            }


            ObjectMapper mapper = new ObjectMapper();

            // 실질적인 데이터 구성
            String jsonParamString = mapper.writeValueAsString(
                    PushUtils.getFcmMessage(target, title, description, image, customInfo)
            );

            // 전송
            OutputStream os = conn.getOutputStream();
            try{
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(jsonParamString);
                writer.flush();
                writer.close();
                os.close();
            } catch (IOException ioe){
                ioe.printStackTrace();
                if(os != null) os.close();
            }

            // 회신 데이터
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            }
        } catch (Exception e) {
            if(e instanceof MalformedURLException) {
                throw new Ice2PushException(e.getClass().getName(), e);
            }
        }
        return response;
    }
}
