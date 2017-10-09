package net.ion.ice.service;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.node.Node;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Map;
import java.util.Properties;

@Service("EmailService")
public class EmailService {
    public static final String callBackUrl = ApplicationContextManager.getContext().getEnvironment().getProperty("cluster.front-prefix");

    public static void sendEmailHtml(String email, String title, String html) {
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

        try {
            message.setSender(new InternetAddress("ytn@ytn.co.kr"));
            message.setSubject(title);
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));

            Multipart multipart = new MimeMultipart();
            MimeBodyPart mimeBodyPart = new MimeBodyPart();

            mimeBodyPart.setContent(html, "text/html; charset=MS949");
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public static String getHeader(String siteType, String company){
        String header;
        // header-대학 : <img src="http://localhost/assets/images/email/logo_store.png" alt="YGOON 교육할인스토어" style="border:0;">
        // header-기업 : <img src="http://localhost/assets/images/email/logo_small.png" alt="YGOON 교육할인스토어" style="border:0;"><p style="font-size:18px;font-weight:bold;color:#fff;letter-spacing:-2px;">해당 기업명 출력영역</p>
        // header-비회원 : <img src="http://localhost/assets/images/email/logo_ygoon.png" alt="YGOON 교육할인스토어" style="border:0;">

        if("company".equals(siteType)){
            header = "<img src="+callBackUrl+"image/email/logo_store.png\" alt=\"YGOON 교육할인스토어\" style=\"border:0;\">";
        } else if("university".equals(siteType)) {
            header = "<img src="+callBackUrl+"image/email/logo_small.png\" alt=\"YGOON 기업스토어\" style=\"border:0;\"><p style=\"font-size:18px;font-weight:bold;color:#fff;letter-spacing:-2px;\">"+company+"</p>";
        } else {
            header = "<img src="+callBackUrl+"image/email/logo_ygoon.png\" alt=\"YGOON 특별할인스토어\" style=\"border:0;\">";
        }
        return header;
    }

    public static String getFooter(String siteType, String company){
        String apiUrl = ApplicationContextManager.getContext().getEnvironment().getProperty("cluster.api-prefix");
        String footer;
        // footer-대학 : <img src="http://localhost/assets/images/email/logo_store.png" alt="YGOON 교육할인스토어" width="111" style="border:0;">
        // footer-기업 : <img src="http://localhost/assets/images/email/logo_small.png" alt="YGOON" width="49" style="border:0;"><span style="display:block;text-align:center;font-size:16px;font-weight:600;color:#fff;letter-spacing:-2px;">해당 기업명 출력영역</span>
        // footer-비회원 : <img src="http://localhost/assets/images/email/logo_ygoon.png" alt="YGOON 특별할인스토어" width="112" style="border:0;">

        if("company".equals(siteType)){
            footer = "<img src="+apiUrl+"image/email/logo_store.png\" alt=\"YGOON 교육할인스토어\" width=\"111\" style=\"border:0;\">";
        } else if("university".equals(siteType)) {
            footer = "<img src=\"+apiUrl+\"image/email/logo_small.png\" alt=\"YGOON\" width=\"49\" style=\"border:0;\"><span style=\"display:block;text-align:center;font-size:16px;font-weight:600;color:#fff;letter-spacing:-2px;\">"+company+"</span>";
        } else {
            footer = "<img src=\"+apiUrl+\"image/email/logo_ygoon.png\" alt=\"YGOON 특별할인스토어\" width=\"112\" style=\"border:0;\">";
        }
        return footer;
    }

    public static void setHtmlMemberJoin(Node node, String email, Map<String, String> emailTemplate) {
        String apiUrl = ApplicationContextManager.getContext().getEnvironment().getProperty("cluster.api-prefix");
        String callBackUrl = ApplicationContextManager.getContext().getEnvironment().getProperty("cluster.front-prefix");

        String siteId = node.getBindingValue("siteId").toString();
        String siteType = node.getBindingValue("siteType").toString();
        String company = "";
        if("company".equals(siteType)){
            company = node.getStringValue("company");
        }

        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();

        String header = getHeader(siteType, company);
        String footer = getFooter(siteType, company);

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("src=\"../assets/images/", "src=" + apiUrl + "/image");

        contents = contents.replaceAll("userId", node.getStringValue("userId")); // abc***@ytn.co.kr
        contents = contents.replaceAll("joinDate", node.getStringValue("joinDate")); // 2017-07-21
        contents = contents.replaceAll("name", node.getStringValue("name")); // 홍*동
        contents = contents.replaceAll("cellphone", node.getStringValue("cellphone")); // 010-11**-11**
        contents = contents.replaceAll("receiveMarketingEmailAgreeYn", node.getReferenceNode("receiveMarketingEmailAgreeYn").get("name").toString()); //	동의
        contents = contents.replaceAll("receiveMarketingSMSAgreeYn", node.getReferenceNode("receiveMarketingSMSAgreeYn").get("name").toString()); // 	동의

        contents = contents.replaceAll("href=\"#\"", "href="+callBackUrl+siteId+"/intro");
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailHtml(email, title, contents);
    }

    public static void setHtmlMemberInfoChange(Node node, String email, Map<String, String> emailTemplate) {
        String siteType = node.getBindingValue("siteType").toString();
        if("company".equals(siteType)){
            String company = node.getStringValue("company");
        }

        String apiUrl = ApplicationContextManager.getContext().getEnvironment().getProperty("cluster.api-prefix");
        String callBackUrl = ApplicationContextManager.getContext().getEnvironment().getProperty("cluster.front-prefix");

        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();
        sendEmailHtml(email, title, contents);
    }
}