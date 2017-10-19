package net.ion.ice.cjmwave.mobile.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/**
 * Created by juneyoungoh on 2017. 9. 29..
 */
@Service("FCMPushService")
public class FCMPushService implements MobilePushService<String, Map, String, Ice2PushException> {

    public static final String PUSH_MSG_SEND_INFO = "pushMsgSendInfo";
    public static final String MSG_SEQ = "msgSeq";
    public static final String THUM_IMG_PATH = "thumImgPath";
    private Logger logger = Logger.getLogger(FCMPushService.class);
    private String fcmApiUrl;
    private String fcmApiKey;
    private JdbcTemplate jdbcTemplate;

    Environment env = ApplicationContextManager.getContext().getEnvironment();

    @PostConstruct
    public void init() {
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
        try {

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

            if (messageObject.containsKey("custom")) {                           // 푸시 메세지에 추가적으로 들어갈 정보. 가령 이벤트 푸시에 이벤트 정보를 넣어서 모바일에서 해당 정보를 참조하게 하는 용도로 사용
                if (messageObject.get("custom") != null) {
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
            try {
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(jsonParamString);
                writer.flush();
                writer.close();
                os.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                if (os != null) os.close();
            }

            // 회신 데이터
            int responseCode = conn.getResponseCode();
            logger.info("####FCM push responseCode:" + responseCode + ",responseMsg:" + conn.getResponseMessage());
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                response = "" + responseCode;
                //String line;
                //BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                //while ((line = br.readLine()) != null) {
                //    response += line;
                //}
            }
        } catch (Exception e) {
            if (e instanceof MalformedURLException) {
                throw new Ice2PushException(e.getClass().getName(), e);
            }
        }
        return response;
    }

    public void sendPushMsg(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        if (data.get(MSG_SEQ) == null || StringUtils.isEmpty(data.get(MSG_SEQ).toString())) {
            throw new IceRuntimeException("Required Parameter : msgSeq");
        }

        String msgSeq = data.get(MSG_SEQ).toString();
        Node infoNode = NodeUtils.getNodeService().read(PUSH_MSG_SEND_INFO, msgSeq);
        if (infoNode == null || infoNode.isEmpty()) {
            throw new IceRuntimeException("Node not found");
        }

        String title = infoNode.getStringValue("pushTitle");
        String description = infoNode.getStringValue("pushSbst");
        String fileUrlFormat = ApplicationContextManager.getContext().getEnvironment().getProperty("image.s3PrefixUrl");
        String fullImgUrl = "";
        if (infoNode.get(THUM_IMG_PATH) != null) {
            fullImgUrl = fileUrlFormat + ((FileValue) infoNode.get(THUM_IMG_PATH)).getStorePath();
        }

        if (jdbcTemplate == null) {
            jdbcTemplate = NodeUtils.getNodeBindingService().getNodeBindingInfo(PUSH_MSG_SEND_INFO).getJdbcTemplate();
        }

        String targetSql = "select appId from mbrAppIdInfo";//mbrAppIdInfo 테이블에서 데이터를 가져와야 함..
        List<Map<String, Object>> list = jdbcTemplate.queryForList(targetSql);
        String targets = "";
        for (Map<String, Object> result : list) {
            if ("".equals(targets)) {
                targets += result.get("appId").toString();
            } else {
                targets += "," + result.get("appId").toString();
            }
        }
        Map<String, Object> sendData = new LinkedHashMap<>();
        Map<String, Object> customData = new LinkedHashMap<>();
        customData.put(MSG_SEQ, msgSeq);
        try {
            sendData.put("title", title);
            sendData.put("description", description);
            //sendData.put("image", fullImgUrl);
            sendData.put("custom", customData);
            String resultCode = sendMessageToTargets(targets, sendData);

            Map<String, Object> saveData = new LinkedHashMap<>();
            saveData.put(MSG_SEQ, msgSeq);
            if ("200".equals(resultCode)) {
                saveData.put("pushYn", "true");
            }else{
                saveData.put("pushYn", "false");
            }
            Node updateNode = (Node)NodeUtils.getNodeService().executeNode(saveData, PUSH_MSG_SEND_INFO, EventService.SAVE);
            context.setResult(updateNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
