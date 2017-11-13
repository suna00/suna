package net.ion.ice.service;

import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("smsService")
public class SmsService {

    @Autowired
    private DBService dbService;

    private JdbcTemplate jdbcTemplate;

    private static final String smsQuery = "INSERT INTO biz_msg ( MSG_TYPE, CMID, REQUEST_TIME, SEND_TIME, DEST_PHONE, SEND_PHONE, MSG_BODY)\n" +
            "VALUES (0, ?, NOW(), NOW(), ?, ?, ?)";

    private static final String lmsQuery = "INSERT INTO biz_msg ( MSG_TYPE, CMID, REQUEST_TIME, SEND_TIME, DEST_PHONE, SEND_PHONE, SUBJECT, MSG_BODY)\n" +
            "VALUES (5, ?, NOW(), NOW(), '01012341234', '0212341234', ‘LMS 제목', '본 메시지는 LMS 테스트 메시지 입니다.')";


    private static final String mmsQuery = "INSERT INTO biz_msg ( MSG_TYPE, CMID, REQUEST_TIME, SEND_TIME, DEST_PHONE, SEND_PHONE, SUBJECT, MSG_BODY, ATTACHED_FILE)\n" +
            "VALUES (5, ?, NOW(), NOW(), '01012341234', '0212341234', 'MMS 제목', '본 메시지는 MMS 테스트 메시지 입니다.', {첨부파일명.jpg})";


    public void sendSms(String id, String destPhone, String sendPhone, String msgBody){
        JdbcTemplate jdbcTemplate = DBService.getJdbc("ytnDevDb") ;

        jdbcTemplate.update(smsQuery, id, destPhone, sendPhone, msgBody);

    }

    public String getSmsTemplate(String name){
        String message = "";
        List<Node> node = NodeUtils.getNodeList("smsTemplate", "name_equals="+name);

        if(0 < node.size()){
            message = node.get(0).get("contents").toString();
        }

        return message;
    }

    public void sendCertCode(String cellphone, String certCode) {
        String message = getSmsTemplate("인증번호");
        message = message.replaceAll("[certCode]", "["+certCode+"]");

        sendSms(certCode, cellphone, cellphone, message);
    }
}
