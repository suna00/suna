package net.ion.ice.service;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

@Service("EmailService")
public class EmailService {
    private static Logger logger = LoggerFactory.getLogger(EmailService.class);

    public static final String callBackUrl = ApplicationContextManager.getContext().getEnvironment().getProperty("cluster.front-prefix");
    public static final String apiUrl = ApplicationContextManager.getContext().getEnvironment().getProperty("cluster.api-prefix");


    public static void sendEmailDirect(String recipients, String subject, String body) throws IOException {
        // returnURL이 없는 경우 사용하는 URL
        String url = "https://directsend.co.kr/index.php/api/v2/mail";
        // returnURL이 있을 경우 사용하는 URL
        // String url = "https://directsend.co.kr/index.php/api/result_v2/mail";

        java.net.URL obj;
        obj = new java.net.URL(url);
        javax.net.ssl.HttpsURLConnection con;
        con = (javax.net.ssl.HttpsURLConnection) obj.openConnection();
        con.setRequestMethod("POST");

        /*
         * subject  : 발송 메일 제목
         * body  : 받을 메일 내용
         * username : directsend 발급 ID
         * recipients : 발송 할 고객 이메일 , 로 구분함. ex) aaa@naver.com,bbb@nate.com (',' 사이에 공백을 제거해주세요!!)
         * key : directsend 발급 api key
	 * return_url : 실제 발송결과를 return 받을 URL
	 *
         * 각 번호가 유효하지 않을 경우에는 발송이 되지 않습니다.
        */


        /* 여기서부터 수정해주시기 바랍니다. */

//        String subject = "[가입인사] 고객님 환영합니다.";
//        String body = "고객님 환경합니다.";
        String sender = "ytn@ytn.co.kr";
        String username = "YGOON";
//        String recipients = "test1@directsend.co.kr,test2@directsend.co.kr";
        String key = "IHezrjsCYUmXDnF";

        // 실제 발송성공실패 여부를 받기 원하실 경우 주석을 해제하신 후 결과를 받을 URL을 입력해 주시기 바랍니다.
        String returnURL = "http://domain";
        int unique_id = 123; // unique_id 를 넣지 않을 경우 Response로 넘어오는 Mail ID로 결과를 받을 수 있습니다.

        int open = 1;		// open 결과를 받으려면 주석을 해제 해주시기 바랍니다.
        int click = 1;		// click 결과를 받으려면 주석을 해제 해주시기 바랍니다.
        int check_period = 3;	// 트래킹 기간을 지정하며 3 / 7 / 15 / 30 일을 기준으로 지정하여 발송해 주시기 바랍니다. (단, 지정을 하지 않을 경우 결과를 받을 수 없습니다.)
        // 아래와 같이 http://domain 일 경우 http://domain?type=[click | open | reject]&mail_id=[MailID]&email=[Email] 과 같은 형식으로 request를 보내드립니다.
        String option_return_url = "http://domain/";

        // 예약발송 파라미터 추가
        String mail_type = "NORMAL"; // NORMAL - 즉시발송 / ONETIME - 1회예약 / WEEKLY - 매주정기예약 / MONTHLY - 매월정기예약 / YEARLY - 매년정기예약
        String start_reserve_time = "2017-05-17 10:00:00"; //  발송하고자 하는 시간
        String end_reserve_time = "2017-05-17 10:00:00"; //  발송이 끝나는 시간 1회 예약일 경우 $start_reserve_time = $end_reserve_time
        // WEEKLY | MONTHLY | YEARLY 일 경우에 시작 시간부터 끝나는 시간까지 발송되는 횟수 Ex) type = WEEKLY, start_reserve_time = '2017-05-17 13:00:00', end_reserve_time = '2017-05-24 13:00:00' 이면 remained_count = 2 로 되어야 합니다.
        int remained_count = 1;

        String agreement_text = "본메일은 [$NOW_DATE] 기준, 회원님의 수신동의 여부를 확인한 결과 회원님께서 수신동의를 하셨기에 발송되었습니다.";
        String deny_text = "메일 수신을 원치 않으시면 [$DENY_LINK]를 클릭하세요.\nIf you don't want this type of information or e-mail, please click the [$EN_DENY_LINK]";
        String sender_info_text = "사업자 등록번호:-- 소재지:ㅇㅇ시(도) ㅇㅇ구(군) ㅇㅇ동 ㅇㅇㅇ번지 TEL:--\nEmail: <a href='mailto:test@directsend.co.kr'>test@directsend.co.kr</a>";
        String logo_path = "http://logoimage.com/image.png";
        int logo_state = 1; // logo 사용시 1 / 사용안할 시 0

        // 첨부파일의 URL을 보내면 DirectSend에서 파일을 download 받아 발송처리를 진행합니다. 파일은 개당5MB 이하로 발송을 해야 하며, 파일의 구분자는 '|(shift+\)'로 사용하며 5개까지만 첨부가 가능합니다.
        String file_url = "http://domain/test.png|http://domain/test1.png";
        // 첨부파일의 이름을 지정할 수 있도록 합니다. 첨부파일의 이름은 순차적(http://domain/test.png - image.png, http://domain/test1.png - image2.png) 와 같이 적용이 되며, file_name을 지정하지 않은 경우 마지막의 파일의 이름이로 메일에 보여집니다.
        String file_name = "image.png|image2.png";

        /* 여기까지만 수정해주시기 바랍니다. */

        /** 수정하지 마시기 바랍니다.
         *  아래의 URL에서 apache commons-comdec jar 파일을 다운로드 한 후에 함께 컴파일 해주십시오.
         *  http://commons.apache.org/proper/commons-codec/download_codec.cgi
         * **/
        String urlParameters = "subject=" + java.net.URLEncoder.encode(org.apache.commons.codec.binary.Base64.encodeBase64String(subject.getBytes("euc-kr")), "EUC_KR")
                + "&body=" + java.net.URLEncoder.encode(org.apache.commons.codec.binary.Base64.encodeBase64String(body.getBytes("euc-kr")), "EUC_KR")
                + "&sender=" + java.net.URLEncoder.encode(sender, "EUC_KR")
                + "&username=" + java.net.URLEncoder.encode(username, "EUC_KR")
                + "&recipients=" + java.net.URLEncoder.encode(recipients, "EUC_KR")

                // 예약 관련 파라미터 쥬석 해제
                //+ "&mail_type=" + java.net.URLEncoder.encode(mail_type, "EUC_KR")
                //+ "&start_reserve_time=" + java.net.URLEncoder.encode(start_reserve_time, "EUC_KR")
                //+ "&end_reserve_time=" + java.net.URLEncoder.encode(end_reserve_time, "EUC_KR")
                //+ "&remained_count=" + java.net.URLEncoder.encode(remained_count, "EUC_KR")

                // 필수 안내문구 관련 파라미터 주석 해제
                //+ "&agreement_text=" + java.net.URLEncoder.encode(org.apache.commons.codec.binary.Base64.encodeBase64String(agreement_text.getBytes("utf-8")), "EUC_KR")
                //+ "&deny_text=" + java.net.URLEncoder.encode(org.apache.commons.codec.binary.Base64.encodeBase64String(deny_text.getBytes("utf-8")), "EUC_KR")
                //+ "&sender_info_text=" + java.net.URLEncoder.encode(org.apache.commons.codec.binary.Base64.encodeBase64String(sender_info_text.getBytes("utg-8")), "EUC_KR")
                //+ "&logo_path=" + java.net.URLEncoder.encode(logo_path, "EUC_KR")
                //+ "&logo_state=" + java.net.URLEncoder.encode(logo_state, "EUC_KR")

                // returnURL이 있는 경우 주석해제
                //+ "&return_url=" + java.net.URLEncoder.encode(returnURL, "EUC_KR")
                //+ "&unique_id=" + java.net.URLEncoder.encode(unique_id, "EUC_KR")

                // 첨부 파일이 있는 경우 주석 해제
                //+ "&file_url=" + java.net.URLEncoder.encode(file_url, "EUC_KR")
                //+ "&file_name=" + java.net.URLEncoder.encode(file_name, "EUC_KR")

                // 발송 결과 측정 항목을 사용할 경우 주석 해제
                //+ "&open=" + java.net.URLEncoder.encode(open, "EUC_KR")
                //+ "&click=" + java.net.URLEncoder.encode(click, "EUC_KR")
                //+ "&check_period=" + java.net.URLEncoder.encode(check_period, "EUC_KR")
                //+ "&option_return_url=" + java.net.URLEncoder.encode(option_return_url, "EUC_KR")

                + "&key=" + java.net.URLEncoder.encode(key, "EUC_KR");

        /** 수정하지 마시기 바랍니다. **/

        System.setProperty("jsse.enableSNIExtension", "false");
        con.setDoOutput(true);
        java.io.DataOutputStream wr = new java.io.DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println(responseCode);

        /*
         * responseCode 가 200 이 아니면 내부에서 문제가 발생한 케이스입니다.
         * directsend 관리자에게 문의해주시기 바랍니다.
         * */

        java.io.BufferedReader in = new java.io.BufferedReader(
                new java.io.InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        System.out.println(response.toString());

	/*
         * response의 실패
         *  {"status":101}
         */

        /*
         * response 성공
         * {"status":0}
         *  성공 코드번호.
         */

        /** status code
         0   : 정상발송
         100 : POST validation 실패
         101 : sender 유효한 번호가 아님
         102 : recipient 유효한 번호가 아님
         103 : message 값 존재X
         104 : recipient count = 0
         105 : message length = 0
         106 : message validation 실패
         107 : 본문에 허용할 수 없는 URL이 포함되어 있습니다.
         109 : returnURL이 없습니다.
         110 : 첨부파일이 없습니다.
         111 : 첨부파일의 개수가 5개를 초과합니다.
         112 : 파일의 Size가 5 MB를 넘어갑니다.
         113 : 첨부파일이 다운로드 되지 않았습니다.
         114 : 중복된 unique_id가 있습니다.
         205 : 잔액부족
         999 : Internal Error.
         **
         */
    }


    public static void sendEmailHtmlold(String email, String title, String html) {
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

    public static void setHtmlMemberJoin(Node node, String email, Map<String, String> emailTemplate) throws IOException {
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();
        String siteId = node.getBindingValue("siteId").toString();
        String siteType = node.getBindingValue("siteType").toString();
        String company = node.getStringValue("company");

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

        sendEmailDirect(email, title, contents);
    }

    public static void setHtmlMemberInfoChange(Node node, String email, Map<String, String> emailTemplate) throws IOException {
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();
        String siteId = node.getBindingValue("siteId").toString();
        String siteType = node.getBindingValue("siteType").toString();
        String company = node.getStringValue("company");

        String header = getHeader(siteType, company);
        String footer = getFooter(siteType, company);

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("src=\"../assets/images/", "src=" + apiUrl + "/image");
        contents = contents.replaceAll("agreeType", ""); // 이메일 , SMS
        contents = contents.replaceAll("agreeYn", ""); // 동의
        contents = contents.replaceAll("date", ""); // 2017-01-01
        contents = contents.replaceAll("href=\"#\"", "href="+callBackUrl+siteId+"/serviceCenter");
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailDirect(email, title, contents);
    }
}