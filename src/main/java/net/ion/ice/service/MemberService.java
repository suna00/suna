package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Map;
import java.util.Properties;

@Service("memberService")
public class MemberService {
    @Autowired
    private NodeService nodeService ;

    public void sendEmail(ExecuteContext context) throws MessagingException {
        //certCode=11111&siteType=company&email=yeon0153@i-on.net
        Map<String, Object> data = context.getData();
        String certCode = data.get("certCode").toString();
        String siteType = data.get("siteType").toString();
        String email = data.get("email").toString();

        String testData = certCode + "/" + siteType + "/" + email;

        //node/emailTemplate/list.json

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.host", "smtp.gmail.com");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.quitwait", "false");

        Authenticator auth = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("kimjiyeon0526@gmail.com", "14wldusdl");
            }
        };

        Session session = Session.getInstance(props, auth);
        MimeMessage message = new MimeMessage(session);

        message.setSender(new InternetAddress("YTN"));
        message.setSubject("YGOON 복지몰 인증 메일입니다.");
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));


        Multipart mp = new MimeMultipart();
        MimeBodyPart mbp1 = new MimeBodyPart();
        mbp1.setText("testData : "+ testData);
        mp.addBodyPart(mbp1);

        message.setContent(mp);
        Transport.send(message);
    }
}
