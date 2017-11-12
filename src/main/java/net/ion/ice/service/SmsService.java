package net.ion.ice.service;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service("SmsService")
public class SmsService {
    private static Logger logger = LoggerFactory.getLogger(SmsService.class);

    public static void sendSms(String cellphone, String message) {
        System.out.println("cellphone: " + cellphone);
        System.out.println("message: " + message);
    }

    public static String getSmsTemplate(String name){
        String message = "";
        List<Node> node = NodeUtils.getNodeList("smsTemplate", "name_equals="+name);

        if(0 < node.size()){
            message = node.get(0).get("contents").toString();
        }

        return message;
    }

    public static void sendCertCode(String cellphone, String certCode) throws IOException {
        String message = getSmsTemplate("인증번호");
        message = message.replaceAll("[certCode]", "["+certCode+"]");

        sendSms(cellphone, message);
    }

}
