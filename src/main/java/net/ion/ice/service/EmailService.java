package net.ion.ice.service;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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

    public static Map<String, String> getEmailTemplate(String name){
        Map<String, String> setHtmlMap = new HashMap<>();
        String title = "";
        String contents = "";

        List<Node> emailTemplateList = NodeUtils.getNodeList("emailTemplate", "name_matching="+name);

        if (0 < emailTemplateList.size()) {
            title = emailTemplateList.get(0).get("title").toString();
            contents = emailTemplateList.get(0).get("contents").toString();
        }

        setHtmlMap.put("title", title);
        setHtmlMap.put("contents", contents);

        return setHtmlMap;
    }

    public static Map<String, String> getAffiliate(String siteId){
        Map<String, String> setSiteInfo = new HashMap<>();
        String siteType = "";
        String name = "";

        List<Node> affiliateList = NodeUtils.getNodeList("affiliate", "siteId_matching="+siteId);
        if(0 < affiliateList.size()){
            siteType = affiliateList.get(0).get("siteType").toString();
            name = affiliateList.get(0).get("name").toString();
        }

        setSiteInfo.put("siteType", siteType);
        setSiteInfo.put("name", name);

        return setSiteInfo;
    }

    public static String getHeaderNew(String siteId){
        String header;
        // header-대학 : <img src="http://localhost/assets/images/email/logo_store.png" alt="YGOON 교육할인스토어" style="border:0;">
        // header-기업 : <img src="http://localhost/assets/images/email/logo_small.png" alt="YGOON 교육할인스토어" style="border:0;"><p style="font-size:18px;font-weight:bold;color:#fff;letter-spacing:-2px;">해당 기업명 출력영역</p>
        // header-비회원 : <img src="http://localhost/assets/images/email/logo_ygoon.png" alt="YGOON 교육할인스토어" style="border:0;">

        Map<String, String> siteInfo = getAffiliate(siteId);

        if("company".equals(siteInfo.get("siteType"))){
            header = "<img src=\""+callBackUrl+"image/email/logo_small.png\" alt=\"YGOON 기업스토어\" style=\"border:0;\"><p style=\"font-size:18px;font-weight:bold;color:#fff;letter-spacing:-2px;\">"+siteInfo.get("name")+"</p>";
        } else if("university".equals(siteInfo.get("siteType"))) {
            header = "<img src=\""+callBackUrl+"image/email/logo_store.png\" alt=\"YGOON 교육할인스토어\" style=\"border:0;\">";
        } else {
            header = "<img src=\""+callBackUrl+"image/email/logo_ygoon.png\" alt=\"YGOON 특별할인스토어\" style=\"border:0;\">";
        }
        return header;
    }

    public static String getFooterNew(String siteId){
        String footer;
        // footer-대학 : <img src="http://localhost/assets/images/email/logo_store.png" alt="YGOON 교육할인스토어" width="111" style="border:0;">
        // footer-기업 : <img src="http://localhost/assets/images/email/logo_small.png" alt="YGOON" width="49" style="border:0;"><span style="display:block;text-align:center;font-size:16px;font-weight:600;color:#fff;letter-spacing:-2px;">해당 기업명 출력영역</span>
        // footer-비회원 : <img src="http://localhost/assets/images/email/logo_ygoon.png" alt="YGOON 특별할인스토어" width="112" style="border:0;">

        Map<String, String> siteInfo = getAffiliate(siteId);

        if("company".equals(siteInfo.get("siteType"))){
            footer = "<img src=\""+callBackUrl+"image/email/logo_small.png\" alt=\"YGOON\" width=\"49\" style=\"border:0;\"><span style=\"display:block;text-align:center;font-size:16px;font-weight:600;color:#fff;letter-spacing:-2px;\">"+siteInfo.get("name")+"</span>";
        } else if("university".equals(siteInfo.get("siteType"))) {
            footer = "<img src=\""+callBackUrl+"image/email/logo_store.png\" alt=\"YGOON 교육할인스토어\" width=\"111\" style=\"border:0;\">";
        } else {
            footer = "<img src=\""+callBackUrl+"image/email/logo_ygoon.png\" alt=\"YGOON 특별할인스토어\" width=\"112\" style=\"border:0;\">";
        }
        return footer;
    }

    public static String getHeader(String siteType, String company){
        String header;
        // header-대학 : <img src="http://localhost/assets/images/email/logo_store.png" alt="YGOON 교육할인스토어" style="border:0;">
        // header-기업 : <img src="http://localhost/assets/images/email/logo_small.png" alt="YGOON 교육할인스토어" style="border:0;"><p style="font-size:18px;font-weight:bold;color:#fff;letter-spacing:-2px;">해당 기업명 출력영역</p>
        // header-비회원 : <img src="http://localhost/assets/images/email/logo_ygoon.png" alt="YGOON 교육할인스토어" style="border:0;">

        if("company".equals(siteType)){
            header = "<img src=\""+callBackUrl+"image/email/logo_small.png\" alt=\"YGOON 기업스토어\" style=\"border:0;\"><p style=\"font-size:18px;font-weight:bold;color:#fff;letter-spacing:-2px;\">"+company+"</p>";
        } else if("university".equals(siteType)) {
            header = "<img src=\""+callBackUrl+"image/email/logo_store.png\" alt=\"YGOON 교육할인스토어\" style=\"border:0;\">";
        } else {
            header = "<img src=\""+callBackUrl+"image/email/logo_ygoon.png\" alt=\"YGOON 특별할인스토어\" style=\"border:0;\">";
        }
        return header;
    }

    public static String getFooter(String siteType, String company){
        String footer;
        // footer-대학 : <img src="http://localhost/assets/images/email/logo_store.png" alt="YGOON 교육할인스토어" width="111" style="border:0;">
        // footer-기업 : <img src="http://localhost/assets/images/email/logo_small.png" alt="YGOON" width="49" style="border:0;"><span style="display:block;text-align:center;font-size:16px;font-weight:600;color:#fff;letter-spacing:-2px;">해당 기업명 출력영역</span>
        // footer-비회원 : <img src="http://localhost/assets/images/email/logo_ygoon.png" alt="YGOON 특별할인스토어" width="112" style="border:0;">

        if("company".equals(siteType)){
            footer = "<img src=\""+callBackUrl+"image/email/logo_small.png\" alt=\"YGOON\" width=\"49\" style=\"border:0;\"><span style=\"display:block;text-align:center;font-size:16px;font-weight:600;color:#fff;letter-spacing:-2px;\">"+company+"</span>";
        } else if("university".equals(siteType)) {
            footer = "<img src=\""+callBackUrl+"image/email/logo_store.png\" alt=\"YGOON 교육할인스토어\" width=\"111\" style=\"border:0;\">";
        } else {
            footer = "<img src=\""+callBackUrl+"image/email/logo_ygoon.png\" alt=\"YGOON 특별할인스토어\" width=\"112\" style=\"border:0;\">";
        }
        return footer;
    }

    public static String getMenu(){
        String menu;
        menu = "<tr>" +
                "<td valign=\"middle\" align=\"center\">\n" +
                "   <a href=\""+callBackUrl+"newProduct/list?listType=1&pageSize=30&categoryId=\" style=\"display:block;font-family:'Malgun Gothic', 'Apple SD Gothic Neo', NanumGothic, dotum, gulim, sans_serif;color:#fff;font-size:15px;font-weight:bold;text-align:center;text-decoration:none;line-height:1.5;padding:9px 0;\">신상품</a>\n" +
                "</td>\n" +
                "<td valign=\"middle\" align=\"center\" style=\"border-left:1px solid #363636\">\n" +
                "   <a href=\""+callBackUrl+"bestProduct/list?themeType=best1d\" style=\"display:block;font-family:'Malgun Gothic', 'Apple SD Gothic Neo', NanumGothic, dotum, gulim, sans_serif;color:#fff;font-size:15px;font-weight:bold;text-align:center;text-decoration:none;line-height:1.5;padding:9px 0;\">BEST</a>\n" +
                "</td>\n" +
                "<td valign=\"middle\" align=\"center\" style=\"border-left:1px solid #363636\">\n" +
                "   <a href=\""+callBackUrl+"specialExhibition/list?specialExhibitionSortType=all&categoryId=&searchText=\" style=\"display:block;font-family:'Malgun Gothic', 'Apple SD Gothic Neo', NanumGothic, dotum, gulim, sans_serif;color:#fff;font-size:15px;font-weight:bold;text-align:center;text-decoration:none;line-height:1.5;padding:9px 0;\">기획전</a>\n" +
                "</td>\n" +
                "<td valign=\"middle\" align=\"center\" style=\"border-left:1px solid #363636\">\n" +
                "   <a href=\""+callBackUrl+"shopEvent/list?page=1&shopEventSortType=all&searchText=\" style=\"display:block;font-family:'Malgun Gothic', 'Apple SD Gothic Neo', NanumGothic, dotum, gulim, sans_serif;color:#fff;font-size:15px;font-weight:bold;text-align:center;text-decoration:none;line-height:1.5;padding:9px 0;\">이벤트</a>\n" +
                "</td>\n" +
                "</tr>";
        return menu;
    }

    /** setHtml
     본인인증 : setHtmlMemberCertCode
     생일축하 : setHtmlMemberBirthday
     회원가입 : setHtmlMemberSignUp
     회원탈퇴 : setHtmlMemberLeave
     휴면해제이메일인증 : setHtmlMemberSleepCancel
     휴면전환안내 : setHtmlMemberSleepChange
     비밀번호변경 : setHtmlMemberPasswordChange
     광고성 정보수신동의 결과 : setHtmlMemberMarketingChange
     수신동의 이력 안내 : setHtmlMemberMarketingNotice
     마일리지 소멸예정 : setHtmlMemberMileage
     상품문의답변등록 : setHtmlProductQuestion
     1:1문의답변등록 : setHtmlOneToOneQuestion
     입점상담신청 : setHtmlNewStoreRequest
     제휴문의 : setHtmlAffliateRequest
     주문완료 : setHtmlOrder
     주문취소 : setHtmlOrderCancel
     상품발송 : setHtmlProduct
     배송완료 : setHtmlProductDelivery
     교환접수 : setHtmlProductChange
     반품접수 : setHtmlProductReturn
     **
     */

    public static void setHtmlMemberCertCode(String email, Map<String, Object> data) throws IOException {
        // 본인인증 ./pc_markup/DE_SL_FR_26_012.html
        getEmailTemplate("본인인증");
    }

    public static void setHtmlMemberSignUp(String email) throws IOException {
        // 회원가입 ./pc_markup/DE_SL_FR_26_008.html
        getEmailTemplate("회원가입");
    }

    public static void setHtmlMemberBirthday(String email, Map<String, Object> data) throws IOException {
        // 생일축하 ./pc_markup/DE_SL_FR_26_015.html
        // data : siteId, name, link

        String siteId = data.get("siteId").toString();
        String header = getHeaderNew(siteId);
        String menu = getMenu();
        String footer = getFooterNew(siteId);

        Map<String, String> emailTemplate = getEmailTemplate("생일축하");
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("<tr id=\"gnbMenu\"></tr>", menu);
        contents = contents.replaceAll("../assets/images", callBackUrl+"image");
        contents = contents.replaceAll("::name::", data.get("name").toString());
        contents = contents.replaceAll("::link::", callBackUrl+siteId+"/mypage/coupon");
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailDirect(email, title, contents);
    }

    public static void setHtmlMemberLeave(Map<String, Object> data) throws IOException {
        // 회원탈퇴 ./pc_markup/DE_SL_FR_26_009.html
        // data : name, userId, date, point, link

        String siteId = data.get("siteId").toString();
        String header = getHeaderNew(siteId);
        String footer = getFooterNew(siteId);

        Map<String, String> emailTemplate = getEmailTemplate("회원탈퇴");
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("../assets/images", callBackUrl+"image");
        contents = contents.replaceAll("::name::", data.get("name").toString());
        contents = contents.replaceAll("::userId::", data.get("userId").toString());
        contents = contents.replaceAll("::date::", data.get("leaveDate").toString());
        contents = contents.replaceAll("::point::", data.get("YPoint").toString());
        contents = contents.replaceAll("::link::", callBackUrl+siteId);
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailDirect(data.get("email").toString(), title, contents);
    }

    public static void setHtmlMemberSleepCancel(String email) throws IOException {
        // 휴면해제이메일인증	 ./pc_markup/DE_SL_FR_26_018.html
        getEmailTemplate("휴면해제이메일인증");
    }

    public static void setHtmlMemberSleepChange(String email, Map<String, Object> data) throws IOException {
        // 휴면전환안내 ./pc_markup/DE_SL_FR_26_011.html
        // data : name, date, link

        String siteId = data.get("siteId").toString();
        String header = getHeaderNew(siteId);
        String footer = getFooterNew(siteId);

        Map<String, String> emailTemplate = getEmailTemplate("휴면전환안내");
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("::name::", data.get("name").toString());
        contents = contents.replaceAll("::date::", data.get("date").toString());
        contents = contents.replaceAll("::link::", callBackUrl+siteId);
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailDirect(email, title, contents);
    }

    public static void setHtmlMemberPasswordChange(String email) throws IOException {
        // 비밀번호변경 ./pc_markup/DE_SL_FR_26_017.html
        getEmailTemplate("비밀번호변경");
    }

    public static void setHtmlMemberMarketingChange(Map<String, String> data) throws IOException {
        // 광고성 정보수신동의 결과 ./pc_markup/DE_SL_FR_26_010.html
        // agreeType, agreeYn, date, link

        String siteId = data.get("siteId");
        String header = getHeaderNew(siteId);
        String footer = getFooterNew(siteId);

        Map<String, String> emailTemplate = getEmailTemplate("광고성 정보수신동의 결과");
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("::agreeType::", data.get("agreeType"));
        contents = contents.replaceAll("::agreeYn::", data.get("agreeYn"));
        contents = contents.replaceAll("::date::", data.get("date"));
        contents = contents.replaceAll("::link::", callBackUrl+siteId+"/serviceCenter");
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailDirect(data.get("email"), title, contents);
    }

    public static void setHtmlMemberMarketingNotice(Node node) throws IOException {
        // 수신동의 이력 안내 ./pc_markup/DE_SL_FR_26_016.html
        // receiveMarketingSMSAgreeYn, receiveMarketingSMSAgreeDate, receiveMarketingEmailAgreeYn, receiveMarketingEmailAgreeDate, link

        String siteId = node.getBindingValue("siteId").toString();
        String header = getHeaderNew(siteId);
        String footer = getFooterNew(siteId);

        Map<String, String> emailTemplate = getEmailTemplate("수신동의 이력 안내");
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("::receiveMarketingSMSAgreeYn::", node.get("receiveMarketingSMSAgreeYn").toString());
        contents = contents.replaceAll("::receiveMarketingSMSAgreeDate::", node.get("receiveMarketingSMSAgreeDate").toString());
        contents = contents.replaceAll("::receiveMarketingEmailAgreeYn::", node.get("receiveMarketingEmailAgreeYn").toString());
        contents = contents.replaceAll("::receiveMarketingEmailAgreeDate::", node.get("receiveMarketingEmailAgreeDate").toString());
        contents = contents.replaceAll("::link::", callBackUrl+siteId);
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailDirect(node.get("email").toString(), title, contents);
    }

    public static void setHtmlMemberMileage(Node node) throws IOException {
        // 마일리지 소멸예정 ./pc_markup/DE_SL_FR_26_013.html
        // point, expiringYPoint, date, link

        String siteId = node.getBindingValue("siteId").toString();
        String header = getHeaderNew(siteId);
        String footer = getFooterNew(siteId);

        Map<String, String> emailTemplate = getEmailTemplate("마일리지 소멸예정");
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("::point::", node.get("YPoint").toString());
        contents = contents.replaceAll("::expiringYPoint::", node.get("YPoint").toString());
        contents = contents.replaceAll("::date::", "");
        contents = contents.replaceAll("::link::", callBackUrl+siteId+"/mypage/mileage");
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailDirect(node.get("email").toString(), title, contents);
    }

    public static void setHtmlProductQuestion(Node node) throws IOException {
        // 상품문의답변등록 ./pc_markup/DE_SL_FR_26_006.html
        // title, date, product, contents - productQuestion

        String siteId = node.getBindingValue("siteId").toString();
        String header = getHeaderNew(siteId);
        String menu = getMenu();
        String footer = getFooterNew(siteId);

        Map<String, String> emailTemplate = getEmailTemplate("상품문의답변등록");
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("<tr id=\"gnbMenu\"></tr>", menu);
        contents = contents.replaceAll("::title::", node.get("title").toString());
        contents = contents.replaceAll("::date::", node.get("created").toString());
        contents = contents.replaceAll("::product::", node.get("productId").toString());
        contents = contents.replaceAll("::contents::", node.get("contents").toString());
        contents = contents.replaceAll("::link::", callBackUrl+siteId+"/mypage/productQuestion");
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailDirect(node.get("email").toString(), title, contents);
    }

    public static void setHtmlOneToOneQuestion(Node node) throws IOException {
        // 1:1문의답변등록 ./pc_markup/DE_SL_FR_26_007.html
        // title, date, product, contents - oneToOneQuestion

        String siteId = node.getBindingValue("siteId").toString();
        String header = getHeaderNew(siteId);
        String menu = getMenu();
        String footer = getFooterNew(siteId);

        Map<String, String> emailTemplate = getEmailTemplate("1:1문의답변등록");
        String title = emailTemplate.get("title").toString();
        String contents = emailTemplate.get("contents").toString();

        contents = contents.replaceAll("<img src=\"header\">", header);
        contents = contents.replaceAll("<tr id=\"gnbMenu\"></tr>", menu);
        contents = contents.replaceAll("::title::", node.get("title").toString());
        contents = contents.replaceAll("::date::", node.get("created").toString());
        contents = contents.replaceAll("::product::", node.get("productId").toString());
        contents = contents.replaceAll("::contents::", node.get("contents").toString());
        contents = contents.replaceAll("::link::", callBackUrl+siteId+"/mypage/oneToOne");
        contents = contents.replaceAll("<img src=\"footer\">", footer);

        sendEmailDirect(node.get("email").toString(), title, contents);
    }

    public static void setHtmlNewStoreRequest(String email) throws IOException {
        // 입점상담신청	 ./pc_markup/DE_SL_FR_26_019.html
        getEmailTemplate("입점상담신청");
    }

    public static void setHtmlAffliateRequest(String email) throws IOException {
        // 제휴문의 ./pc_markup/DE_SL_FR_26_020.html
        getEmailTemplate("제휴문의");
    }

    public static void setHtmlOrder(String email) throws IOException {
        // 주문완료 ./pc_markup/DE_SL_FR_26_001.html
        getEmailTemplate("주문완료");
    }

    public static void setHtmlOrderCancel(String email) throws IOException {
        // 주문취소 ./pc_markup/DE_SL_FR_26_003.html
        getEmailTemplate("주문취소");
    }

    public static void setHtmlProduct(String email) throws IOException {
        // 상품발송 ./pc_markup/DE_SL_FR_26_002.html
        getEmailTemplate("상품발송");
    }

    public static void setHtmlProductDelivery(String email) throws IOException {
        // 배송완료 ./pc_markup/DE_SL_FR_26_014.html
        getEmailTemplate("배송완료");
    }

    public static void setHtmlProductChange(String email) throws IOException {
        // 교환접수 (신청) ./pc_markup/DE_SL_FR_26_004.html
        getEmailTemplate("교환접수");
    }

    public static void setHtmlProductReturn(String email) throws IOException {
        // 반품접수 (신청) ./pc_markup/DE_SL_FR_26_005.html
        getEmailTemplate("반품접수");
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
}