package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("memberService")
public class MemberService {
    public static final String CREATE = "create";
    public static final String UPDATE = "update";

    @Autowired
    private NodeService nodeService;
    private CommonService common;


    public ExecuteContext authenticationSendEmail(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());
        String emailCertificationType = data.get("emailCertificationType").toString();
        String certStatus = data.get("certStatus").toString();
        String email = data.get("email").toString();
        String siteType = data.get("siteType").toString();
        String date = data.get("date").toString();
        String time = data.get("time").toString();

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("member");
        String query = " select count(memberNo) as count from member where email=? ";
        String count = nodeBindingInfo.getJdbcTemplate().queryForList(query, email).get(0).get("count").toString();

        if("0".equals(count)){
            String contextPath = replaceUrl();
            String certCode = getCertCode(email, emailCertificationType, certStatus);
            Map<String, String> html = html(contextPath, certCode, email, siteType, date, time);

            sendEmailHtml(email, html.get("title"), html.get("contents"));
        } else {
            common.setErrorMessageAlert(context,"U0001", "중복된 인증 메일입니다.");
        }

        return context;
    }

    public ExecuteContext authenticationCertEmail(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());
        String certCode = data.get("certCode").toString();

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("emailCertification");
        String query = " SELECT emailcertificationId, certCode, certStatus, certRequestDate, date_add(certRequestDate, INTERVAL +60 MINUTE) AS certExpireDate, (certStatus = 'request' AND date_add(certRequestDate, INTERVAL +60 MINUTE) > now() AND certCode = ?) AS available FROM emailcertification WHERE certCode = ?  limit 1";
        List<Map<String, Object>> list = nodeBindingInfo.getJdbcTemplate().queryForList(query, certCode, certCode);

        if(0 < list.size()){
            String available = list.get(0).get("available").toString();
            Date certExpireDate = (Date) list.get(0).get("certExpireDate");
            Integer result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()).compareTo(String.valueOf(certExpireDate));

            if("1".equals(available) && result < 1){
                Map<String, Object> emailCertificationData = new HashMap<>();
                emailCertificationData.put("emailCertificationId", list.get(0).get("emailCertificationId"));
                emailCertificationData.put("certStatus", "success");
                emailCertificationData.put("certSuccessDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

                nodeService.executeNode(emailCertificationData, "emailCertification", UPDATE);
            } else{
                common.setErrorMessageAlert(context,"U0003", "만료된 인증코드입니다.");
            }
        } else {
            common.setErrorMessageAlert(context,"U0002", "존재하지 않는 인증코드입니다.");
        }

        return context;
    }

    public static String replaceUrl(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String[] splitRequestUrl = StringUtils.split(request.getRequestURL().toString(), "/");
        return String.format("%s//%s", splitRequestUrl[0], splitRequestUrl[1]); // "http://localhost:8080", "http://125.131.88.206:8080"
    }

    public String getCertCode(String email, String emailCertificationType, String certStatus){
        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("emailCertification");
        String query = " select to_base64(concat('ygoon!@#SHOP ',now())) as certCode ";
        String certCode = nodeBindingInfo.getJdbcTemplate().queryForList(query).get(0).get("certCode").toString();

        Map<String, Object> emailCertificationData = new HashMap<>();
        emailCertificationData.put("name", "이메일 인증요청");
        emailCertificationData.put("emailCertificationType", emailCertificationType);
        emailCertificationData.put("email", email);
        emailCertificationData.put("certCode", certCode);
        emailCertificationData.put("certStatus", certStatus);
        emailCertificationData.put("certRequestDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        nodeService.executeNode(emailCertificationData, "emailCertification", CREATE);

        return certCode;
    }

    public Map<String, String> html(String contextPath, String certCode, String email, String siteType, String date, String time){
        Map<String, String> html = new HashMap<>();

        String title = "";
        String contents = "";
        String link = "http://localhost:3090/signUp/stepTwo?certCode="+certCode+"&siteType="+siteType+"&email="+email;

        List<Node> list = nodeService.getNodeList("emailTemplate", "name_matching=본인인증");
        if(list.size() > 0){
            title = list.get(0).get("title").toString();
            contents = list.get(0).get("contents").toString();

            contents = contents.replaceAll("src=\"http://localhost/assets/images","src="+contextPath+"/image");
            contents = contents.replaceAll("href=\"#\"", "href="+link);
            contents = contents.replaceAll("2017-07-20",date);
            contents = contents.replaceAll("04:20:20",time);
        }

        html.put("title", title);
        html.put("contents", contents);

        return html;
    }

    public void sendEmailHtml(String email, String title, String html) {
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

    public ExecuteContext leaveMembership(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "memberNo" };
        if (common.requiredParams(context, data, params)) return context;

        List<Node> list = nodeService.getNodeList("orderProduct", "memberNo_matching="+data.get("memberNo"));
        if(list.size() > 0){
            for(Node node : list){
                String orderStatus = node.getValue("orderStatus").toString();
//                배송완료,구매확정,취소완료,교환배송완료,반품완료
                if( !("order006".equals(orderStatus) || "order007".equals(orderStatus) || "order009".equals(orderStatus) || "order016".equals(orderStatus) || "order021".equals(orderStatus))){
                    common.setErrorMessage(context, "L0001");
                    return context;
                }
            }
        }

        Node node = nodeService.getNode("member", data.get("memberNo").toString());
        if(node == null){
            common.setErrorMessage(context, "L0002");
            return context;
        }

        node.put("memberStatus", "leave");
        nodeService.executeNode(node, "member", common.UPDATE);

        data.putAll(node);
        data.put("leaveDate", new Date());
        nodeService.executeNode(data, "requestToleaveMember", common.CREATE);

        return context;
    }
}
