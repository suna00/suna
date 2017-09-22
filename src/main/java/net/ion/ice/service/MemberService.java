package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service("memberService")
public class MemberService {
    public static final String CREATE = "create";
    public static final String UPDATE = "update";

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService ;

    private CommonService commonService;
    private EmailService emailService;

    public ExecuteContext authenticationSendEmail(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "email","siteType","acceptTermsYn","receiveMarketingEmailAgreeYn","receiveMarketingSMSAgreeYn" };
        if (commonService.requiredParams(context, data, params)) return context;

        String email = data.get("email").toString();
        String siteType = data.get("siteType").toString();
        String acceptTermsYn = data.get("acceptTermsYn").toString();
        String receiveMarketingEmailAgreeYn = data.get("receiveMarketingEmailAgreeYn").toString();
        String receiveMarketingSMSAgreeYn = data.get("receiveMarketingSMSAgreeYn").toString();

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("member");
        String count = nodeBindingInfo.getJdbcTemplate().queryForList(" select count(memberNo) as count from member where email=? ", email).get(0).get("count").toString();

        if("0".equals(count)){
            String certCode = getCertCode("이메일 인증요청", "join", null, email, "request");
            String linkUrl = "/signUp/stepTwo?certCode="+certCode+"&siteType="+siteType+"&acceptTermsYn="+acceptTermsYn+"&receiveMarketingEmailAgreeYn="+receiveMarketingEmailAgreeYn+"&receiveMarketingSMSAgreeYn="+receiveMarketingSMSAgreeYn;
            Map<String, String> html = memberHtml("본인인증", linkUrl);

            emailService.sendEmailHtml(email, html.get("title"), html.get("contents"));
        } else {
            commonService.setErrorMessage(context, "U0001");
        }

        return context;
    }

    public ExecuteContext searchPassword(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "searchType","name","userId" };
        if (commonService.requiredParams(context, data, params)) return context;

        String searchType = data.get("searchType").toString();
        String name = data.get("name").toString();
        String userId = data.get("userId").toString();

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("member");
        List<Map<String,Object>> memberList = nodeBindingInfo.getJdbcTemplate().queryForList(" select * from member where name=? and userId=? ", name, userId);

        if(0 < memberList.size()){
            Map<String, Object> member = memberList.get(0);
            String memberNo = member.get("memberNo").toString();
            String email = member.get("email").toString();
            String cellPhone = member.get("cellPhone").toString();

            if("email".equals(searchType)){
                String certCode = getCertCode("이메일 인증요청", "password", memberNo, email, "request");
                String linkUrl = "/signIn/changePassword?certCode="+certCode;
                Map<String, String> html = memberHtml("비밀번호변경", linkUrl);

                emailService.sendEmailHtml(member.get("email").toString(), html.get("title"), html.get("contents"));

                Map<String, Object> resultObject = new HashMap<>();
                resultObject.put("email", member.get("email").toString());
                context.setResult(resultObject);
            } else {
                String cellPhoneData = data.get("cellPhoneData").toString();
            }
        } else {
            commonService.setErrorMessage(context, "U0004");
        }

        return context;
    }

    public ExecuteContext authenticationCertEmail(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "certCode" };
        if (commonService.requiredParams(context, data, params)) return context;

        String certCode = data.get("certCode").toString();

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("emailCertification");
        String query = " SELECT emailcertificationId, email, memberNo, certCode, certStatus, certRequestDate, date_add(certRequestDate, INTERVAL +60 MINUTE) AS certExpireDate, (certStatus = 'request' AND date_add(certRequestDate, INTERVAL +60 MINUTE) > now() AND certCode = ?) AS available FROM emailcertification WHERE certCode = ?  limit 1";
        List<Map<String, Object>> list = nodeBindingInfo.getJdbcTemplate().queryForList(query, certCode, certCode); // new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())

        if(0 < list.size()){
            Map<String,Object> map = list.get(0);
            String available = map.get("available").toString();

            if("1".equals(available)){
                Map<String, Object> emailCertificationData = new HashMap<>();
                emailCertificationData.put("emailCertificationId", map.get("emailCertificationId"));
                emailCertificationData.put("certStatus", "success");
                emailCertificationData.put("certSuccessDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

                nodeService.executeNode(emailCertificationData, "emailCertification", UPDATE);

                Map<String, Object> resultObject = new HashMap<>();
                Map<String, Object> item = new HashMap<>();

                item.put("memberNo", map.get("memberNo"));
                item.put("email", map.get("email"));
                resultObject.put("data", item);

                context.setResult(resultObject);
            } else{
                commonService.setErrorMessage(context, "U0003");
            }
        } else {
            commonService.setErrorMessage(context, "U0002");
        }

        return context;
    }

    public ExecuteContext leaveMembership(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "memberNo","leaveType","reasonType" };
        if (commonService.requiredParams(context, data, params)) return context;

        List<Node> list = nodeService.getNodeList("orderProduct", "memberNo_matching="+data.get("memberNo"));
        if(list.size() > 0){
            for(Node node : list){
                String orderStatus = node.getValue("orderStatus").toString();
//                배송완료,구매확정,취소완료,교환배송완료,반품완료
                if( !("order006".equals(orderStatus) || "order007".equals(orderStatus) || "order009".equals(orderStatus) || "order016".equals(orderStatus) || "order021".equals(orderStatus))){
                    commonService.setErrorMessage(context, "L0001");
                    return context;
                }
            }
        }

        Node node = nodeService.getNode("member", data.get("memberNo").toString());
        if(node == null){
            commonService.setErrorMessage(context, "L0002");
            return context;
        }

        Node leave = nodeService.getNode("requestToleaveMember", data.get("memberNo").toString());
        if(leave != null){
            commonService.setErrorMessage(context, "L0003");
            return context;
        }

        data.putAll(node);
        data.put("leaveDate", new Date());
        nodeService.executeNode(data, "requestToleaveMember", commonService.CREATE);

        node.put("memberStatus", "leave");
        nodeService.executeNode(node, "member", commonService.UPDATE);


        return context;
    }

    //    임시 비밀번호 생성
    public ExecuteContext getTempPassword(ExecuteContext context){
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("password", randomPassword());

        context.setResult(object);

        return context;
    }

    public String randomPassword(){
        char pwCollection[] = new char[] {
                '1','2','3','4','5','6','7','8','9','0',
                'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
                'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
                '!','@','#','$','%','^','&'};

        String ranPw = "";

        for (int i = 0; i < 10; i++) {
            int selectRandomPw = (int)(Math.random()*(pwCollection.length));//Math.rondom()은 0.0이상 1.0미만의 난수를 생성해 준다.
            ranPw += pwCollection[selectRandomPw];
        }
        return ranPw;
    }

    public String getCertCode(String name, String emailCertificationType, String memberNo, String email, String certStatus){
        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("emailCertification");
        String certCode = nodeBindingInfo.getJdbcTemplate().queryForList(" select to_base64(concat('ygoon!@#SHOP ',now())) as certCode ").get(0).get("certCode").toString();

        Map<String, Object> emailCertificationData = new HashMap<>();
        emailCertificationData.put("name",name);
        emailCertificationData.put("emailCertificationType", emailCertificationType);
        emailCertificationData.put("memberNo", memberNo);
        emailCertificationData.put("email", email);
        emailCertificationData.put("certCode", certCode);
        emailCertificationData.put("certStatus", certStatus);
        emailCertificationData.put("certRequestDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        nodeService.executeNode(emailCertificationData, "emailCertification", CREATE);

        return certCode;
    }

    public Map<String, String> memberHtml(String templateName, String linkUrl){
        Map<String, String> html = new HashMap<>();

        String contextPath = commonService.replaceUrl();
        LocalDateTime date = LocalDateTime.now().plusHours(1);

        String title = "";
        String contents = "";
        String link = "http://localhost:3090"+linkUrl;

        List<Map<String, Object>> emailTemplateList = nodeBindingService.list("emailTemplate", "name_in=".concat(templateName));

        if(emailTemplateList.size() > 0){
            title = emailTemplateList.get(0).get("title").toString();
            contents = emailTemplateList.get(0).get("contents").toString();

            contents = contents.replaceAll("src=\"http://localhost/assets/images","src="+contextPath+"/image");
            contents = contents.replaceAll("href=\"#\"", "href="+link);
            contents = contents.replaceAll("yyyy-MM-dd", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // yyyy-MM-dd
            contents = contents.replaceAll("HH:mm:ss", date.format(DateTimeFormatter.ofPattern("HH:mm:ss"))); // HH:mm:ss
        }

        html.put("title", title);
        html.put("contents", contents);

        return html;
    }
}
