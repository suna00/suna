package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.NodeService;
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
import java.util.Map;
import java.util.Properties;

@Service("memberService")
public class MemberService {
    @Autowired
    private NodeService nodeService;

    public void authenticationSendEmail(ExecuteContext context) throws MessagingException {
        // http://localhost:8080/node/member/event/sendEmail.json?certCode=11111&siteType=company&email=yeon0153@i-on.net&date=2017-09-13&time=21:02:46

        String contextPath = replaceUrl();
        Map<String, Object> data = context.getData();
        String email = data.get("email").toString();
        String link = "http://localhost:3090/main?email="+email+"&certCode="+data.get("certCode").toString()+"&siteType="+data.get("siteType").toString();

        // dataUri
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Document</title>\n" +
                "    <style>\n" +
                "        * {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" height=\"100%\" bgcolor=\"#e5e5e5\">\n" +
                "    <tr>\n" +
                "        <td valign=\"top\" align=\"center\">\n" +
                "\n" +
                "            <table cellpadding=\"0\" cellspacing=\"0\" width=\"680\" height=\"60\">\n" +
                "                <tr>\n" +
                "                    <td bgcolor=\"#0086e3\" style=\"padding-left:20px;\" valign=\"middle\">\n" +
                "                        <img src=\""+contextPath+"/image/email/logo_store.png\" alt=\"YGOON 교육할인스토어\" style=\"border:0;\">\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "            </table>\n" +
                "\n" +
                "        </td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td valign=\"top\" align=\"center\">\n" +
                "\n" +
                "            <table cellpadding=\"0\" cellspacing=\"0\" width=\"680\" height=\"200\" bgcolor=\"#f5f5f5\" style=\"table-layout:fixed;\">\n" +
                "                <tr>\n" +
                "                    <td valign=\"middle\" align=\"center\" style=\"padding-left:30px;padding-right:30px;\">\n" +
                "                        <p style=\"font-family:'Malgun Gothic', 'Apple SD Gothic Neo', NanumGothic, dotum, gulim, sans_serif;color:#000;font-size:28px;font-weight:bold;text-align:left;line-height:1.5;margin:0;letter-spacing:-2px;\">\n" +
                "                            YGOON 교육할인 스토어 인증 메일입니다.</p>\n" +
                "                        <p style=\"font-family:'Malgun Gothic', 'Apple SD Gothic Neo', NanumGothic, dotum, gulim, sans_serif;color:#000;font-size:18px;text-align:left;line-height:1.5;margin:7px 0 0;letter-spacing:-2px;\">\n" +
                "                            YGOON 교육할인 스토어에 가입할 회원임을 메일로 확인하고 있습니다.<br>인증을 원하시면 인증하기 버튼을 클릭해주세요. </p>\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "            </table>\n" +
                "\n" +
                "        </td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td valign=\"top\" align=\"center\">\n" +
                "            <table cellpadding=\"0\" cellspacing=\"0\" width=\"680\" height=\"60\">\n" +
                "                <tr>\n" +
                "                    <td bgcolor=\"#ffffff\" valign=\"top\" style=\"padding:40px 30px 40px;\">\n" +
                "\n" +
                "                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                            <tr>\n" +
                "                                <td style=\"font-family:'Malgun Gothic', 'Apple SD Gothic Neo', NanumGothic, dotum, gulim, sans_serif;color:#000;font-size:16px;line-height:1.5;letter-spacing:-1px;\">\n" +
                "                                    <p>안녕하세요.<br>소중한 개인정보 보호를 위해 이메일 인증 확인을 하고 있습니다.</p>\n" +
                "                                    <p style=\"padding-top:16px;\">인증을 하기 위해서는 아래 인증하기 버튼을 클릭해주시면 인증 받을 수 있습니다.<br>인증 유효 시간은 메일 수신 시간으로부터 60분입니다. 시간이 초과하면 다시 인증 받으셔야<br>하니 유의해주시기 바랍니다.</p>\n" +
                "                                    <p style=\"padding-top:16px;color:#ff0000;\"><strong>인증 유효시간</strong> : "+data.get("date").toString()+" / "+data.get("time").toString()+"</p>\n" +
                "                                </td>\n" +
                "                            </tr>\n" +
                "                            <tr>\n" +
                "                                <td style=\"padding-top:60px;\">\n" +
                "                                    <a href=\""+link+"\"\n" +
                "                                       target=\"_blank\"\n" +
                "                                       style=\"display:block;font-family:'Malgun Gothic', 'Apple SD Gothic Neo', NanumGothic, dotum, gulim, sans_serif;background:#222222;color:#fff;font-size:16px;text-align:center;text-decoration:none;line-height:1.5;padding:16px 0;letter-spacing:-1px;\">YGOON\n" +
                "                                        복지몰 가기</a>\n" +
                "                                </td>\n" +
                "                            </tr>\n" +
                "                        </table>\n" +
                "\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "            </table>\n" +
                "        </td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "        <td valign=\"top\" align=\"center\">\n" +
                "            <table cellpadding=\"0\" cellspacing=\"0\" width=\"680\" height=\"250\">\n" +
                "                <tr>\n" +
                "                    <td bgcolor=\"#0086e3\" valign=\"middle\">\n" +
                "                        <p style=\"font-family:dotum, gulim, sans_serif;color:#fff;font-size:11px;text-align:center;line-height:1.5;\">사이트 이용 시 필요한 정보성 메일은 수신 여부와 관계없이 발송되니 이점 참고해주시기 바랍니다.<br>이 메일은 발신 전용 메일로 회신을\n" +
                "                            받을 수 없습니다.<br>추가 문의사항이나 다른 문의사항은 고객센터를 이용해주시기 바랍니다.</p>\n" +
                "                        <p style=\"text-align:center;padding:25px 0 12px;\">\n" +
                "                            <img src=\""+contextPath+"/image/email/logo_store.png\" alt=\"YGOON 교육할인스토어\" width=\"111\" style=\"border:0;\">\n" +
                "                        </p>\n" +
                "                        <p style=\"font-family:dotum, gulim, sans_serif;color:#fff;font-size:11px;text-align:center;line-height:1.5;\">\n" +
                "                            ㈜와이티엔 &nbsp; 주소 (03926) 서울특별시 마포구 상암산로 76 YTN 뉴스퀘어 &nbsp; 대표이사 조준희<br>\n" +
                "                            사업자등록번호 102-81-32883 &nbsp; 통신판매업 신고번호. 마포 1507<br>\n" +
                "                            고객센터 02-398-8880 &nbsp; 팩스 02-398-8359 &nbsp; 이메일 csmaster@ytn.co.kr\n" +
                "                        </p>\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "            </table>\n" +
                "        </td>\n" +
                "    </tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>\n";

        sendMail(email, html);
    }

    public static String replaceUrl(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String[] splitRequestUrl = StringUtils.split(request.getRequestURL().toString(), "/");
        //return String.format("%s//%s/%s", splitRequestUrl[0], splitRequestUrl[1], splitRequestUrl[2]);
        return String.format("%s//%s", splitRequestUrl[0], splitRequestUrl[1]); // "localhost:8080", "http://125.131.88.206:8080"
    }

    public void sendMail(String email, String html) throws MessagingException {
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

        message.setSender(new InternetAddress("ytn@ytn.co.kr"));
        message.setSubject("YGOON 복지몰 인증 메일입니다.");
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));

        Multipart multipart = new MimeMultipart();
        MimeBodyPart mimeBodyPart = new MimeBodyPart();

        mimeBodyPart.setContent(html, "text/html; charset=MS949");
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);

        Transport.send(message);
    }
}
