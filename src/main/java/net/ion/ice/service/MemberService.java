package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
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

    public ExecuteContext saveMemberInfo(ExecuteContext context){
        Map<String, Object> contextData = new LinkedHashMap<>(context.getData());
        Map<String, Object> resultObject = new HashMap<>();
        Map<String, Object> item = new HashMap<>();

        Node node;

        if(contextData.get("memberNo") == null){
            if(contextData.containsKey("password")){
                String password = SHA256(contextData.get("password").toString());
                contextData.put("password", password);
            }
            node = (Node)nodeService.executeNode(contextData, "member", CREATE);

        } else {
            if(contextData.containsKey("password")){
                String password = SHA256(contextData.get("password").toString());
                contextData.put("password", password);
            }
            node = (Node)nodeService.executeNode(contextData, "member", UPDATE);
        }

        item.put("memberNo", node.get("memberNo"));
        item.put("email", node.get("email"));
        item.put("name", node.get("name"));

        resultObject.put("item", item);
        context.setResult(resultObject);

        return context;
    }

    public ExecuteContext certPassword(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        Map<String, Object> resultObject = new HashMap<>();
        Map<String, Object> item = new HashMap<>();

        if(data.containsKey("email")){
            authenticationCertEmail(context);
        }

        if(data.containsKey("cellphone")){
            NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("smsCertification");
            String query = " SELECT * FROM smscertification WHERE certCode = ? AND cellphone = ? AND certStatus = 'success' ";
            List<Map<String,Object>> certList = nodeBindingInfo.getJdbcTemplate().queryForList(query, data.get("certCode"), data.get("cellphone"));

            if(0 < certList.size()){
                item.put("memberNo", certList.get(0).get("memberNo"));
                resultObject.put("item", item);
                context.setResult(resultObject);
            } else {
                commonService.setErrorMessage(context, "U0002"); return context;
            }
        }

        return context;
    }

    public ExecuteContext authenticationSendEmail(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "emailCertificationType" };
        if (commonService.requiredParams(context, data, params)) return context;

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("member");
        String emailCertificationType = data.get("emailCertificationType").toString();

        // 회원가입 : join
        if("join".equals(emailCertificationType)){
            String email = data.get("email").toString();
            String count = nodeBindingInfo.getJdbcTemplate().queryForList(" select count(memberNo) as count from member where email=? ", email).get(0).get("count").toString();

            if("0".equals(count)){
                sendEmail(emailCertificationType, email, data);
            } else {
                commonService.setErrorMessage(context, "U0001"); return context;
            }
        }

        // 비밀번호 : password, 휴면회원해제:sleep
        if("password".equals(emailCertificationType) || "sleep".equals(emailCertificationType)){
            List<Map<String,Object>> memberList = nodeBindingInfo.getJdbcTemplate().queryForList(" select * from member where name=? and userId=? ", data.get("name").toString(), data.get("userId").toString());

            if(0 < memberList.size()){
                Map<String, Object> member = memberList.get(0);
                sendEmail(emailCertificationType, member.get("email").toString(), member);

                Map<String, Object> resultObject = new HashMap<>();
                Map<String, Object> item = new HashMap<>();

                item.put("email", member.get("email"));
                resultObject.put("item", item);
                context.setResult(resultObject);
            } else {
                commonService.setErrorMessage(context, "U0004"); return context;
            }
        }

        return context;
    }

    public ExecuteContext authenticationSendSms(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        Map<String, Object> resultObject = new HashMap<>();
        Map<String, Object> item = new HashMap<>();

        String[] params = { "smsCertificationType" };
        if (commonService.requiredParams(context, data, params)) return context;

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("member");
        String smsCertificationType = data.get("smsCertificationType").toString();
        String cellphone = data.get("cellphone").toString();

        if("id".equals(smsCertificationType)){
            List<Map<String,Object>> memberList = nodeBindingInfo.getJdbcTemplate().queryForList(" SELECT * FROM member WHERE cellphone=? ORDER BY memberNo DESC ", cellphone);

            if(0 < memberList.size()){
                Map<String, Object> member = memberList.get(0);
                sendSms(smsCertificationType, cellphone, member);

                item.put("cellphone", member.get("cellphone"));
                resultObject.put("item", item);
                context.setResult(resultObject);
            } else {
                commonService.setErrorMessage(context, "U0005"); return context;
            }
        }

        if("password".equals(smsCertificationType) || "sleepMember".equals(smsCertificationType)){
            List<Map<String,Object>> memberList = nodeBindingInfo.getJdbcTemplate().queryForList(" select * from member where name=? and userId=? and cellphone=?", data.get("name").toString(), data.get("userId").toString(), cellphone);

            if(0 < memberList.size()){
                Map<String, Object> member = memberList.get(0);
                sendSms(smsCertificationType, cellphone, member);

                item.put("memberNo", member.get("memberNo"));
                item.put("cellphone", member.get("cellphone"));
                resultObject.put("item", item);
                context.setResult(resultObject);
            } else {
                commonService.setErrorMessage(context, "U0004"); return context;
            }
        }

        return context;
    }

    public ExecuteContext authenticationCertEmail(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "certCode" };
        if (commonService.requiredParams(context, data, params)) return context;

        String certCode = data.get("certCode").toString();
        String email = data.get("email").toString();

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("emailCertification");
        String query = " SELECT emailcertificationId, email, memberNo, certCode, certStatus, certRequestDate, date_add(certRequestDate, INTERVAL +60 MINUTE) AS certExpireDate, (certStatus = 'request' AND date_add(certRequestDate, INTERVAL +60 MINUTE) > now() AND certCode = ?) AS available FROM emailcertification WHERE certCode = ? and email = ? limit 1";
        List<Map<String, Object>> list = nodeBindingInfo.getJdbcTemplate().queryForList(query, certCode, certCode, email);

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
                item.put("email", email); // 회원가입
                resultObject.put("item", item);

                context.setResult(resultObject);
            } else{
                commonService.setErrorMessage(context, "U0003");
            }
        } else {
            commonService.setErrorMessage(context, "U0002");
        }

        return context;
    }

    public ExecuteContext authenticationCertSms(ExecuteContext context){
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = { "certCode" };
        if (commonService.requiredParams(context, data, params)) return context;

        String certCode = data.get("certCode").toString();
        String cellphone = data.get("cellphone").toString();

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("smsCertification");
        String query = " SELECT smsCertificationId, cellphone, memberNo, certCode, certStatus, certRequestDate, date_add(certRequestDate, INTERVAL +3 MINUTE) AS certExpireDate, (certStatus = 'request' AND date_add(certRequestDate, INTERVAL +3 MINUTE) > now() AND certCode = ?) AS available FROM smscertification WHERE certCode = ? AND cellphone = ? limit 1";
        List<Map<String, Object>> list = nodeBindingInfo.getJdbcTemplate().queryForList(query, certCode, certCode, cellphone);

        if(0 < list.size()){
            Map<String,Object> map = list.get(0);
            String available = map.get("available").toString();

            if("1".equals(available)){
                Map<String, Object> smsCertificationData = new HashMap<>();
                smsCertificationData.put("smsCertificationId", map.get("smsCertificationId"));
                smsCertificationData.put("certStatus", "success");
                smsCertificationData.put("certSuccessDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

                nodeService.executeNode(smsCertificationData, "smsCertification", UPDATE);

                Map<String, Object> resultObject = new HashMap<>();
                Map<String, Object> item = new HashMap<>();

                item.put("memberNo", map.get("memberNo"));
                resultObject.put("item", item);

                context.setResult(resultObject);
            } else{
                commonService.setErrorMessage(context, "U0003");
            }
        } else {
            commonService.setErrorMessage(context, "U0006");
        }

        return context;
    }

    public String SHA256(String password){
        String encodingPassword;

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            encodingPassword = hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }

        return  encodingPassword;
    }

    public void sendEmail(String emailCertificationType, String email, Map<String, Object> data){
        Map<String, String> html = new HashMap<>();

        // 회원가입 : join
        if("join".equals(emailCertificationType)){
            String certCode = getEmailCertCode("이메일 인증요청", emailCertificationType, null, email, "request");
            String linkUrl = "/signUp/stepTwo?certCode="+certCode+"&email="+email+"&siteType="+data.get("siteType")+"&acceptTermsYn="+data.get("acceptTermsYn")+"&receiveMarketingEmailAgreeYn="+data.get("receiveMarketingEmailAgreeYn")+"&receiveMarketingSMSAgreeYn="+data.get("receiveMarketingSMSAgreeYn");
            html = setHtml("본인인증", linkUrl);
        }

        // 비밀번호 : password
        if("password".equals(emailCertificationType)){
            String certCode = getEmailCertCode("이메일 인증요청", emailCertificationType, data.get("memberNo").toString(), email, "request");
            String linkUrl = "/signIn/changePassword?certCode="+certCode+"&email="+email;
            html = setHtml("비밀번호변경", linkUrl);
        }

        // 휴면회원:sleep
        if("sleep".equals(emailCertificationType)){
            String certCode = getEmailCertCode("이메일 인증요청", emailCertificationType, data.get("memberNo").toString(), email, "request");
            String linkUrl = "/signIn/changePassword?certCode="+certCode+"&email="+email;
            html = setHtml("휴면해제이메일인증", linkUrl);
        }

        emailService.sendEmailHtml(email, html.get("title"), html.get("contents"));
    }

    public void sendSms(String smsCertificationType, String cellphone, Map<String, Object> data){
        // 아이디 : id, 패스워드 : password, 휴면회원 : sleepMember

        String certCdoe = getSmsCertCode("SMS 인증요청", smsCertificationType, data.get("memberNo").toString(), cellphone, "request");
        String Message = "인증번호["+certCdoe+"]";

        System.out.println(Message);
        // 문자 전송
    }

    public String getEmailCertCode(String name, String emailCertificationType, String memberNo, String email, String certStatus){
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

    public String getSmsCertCode(String name, String smsCertificationType, String memberNo, String cellphone, String certStatus){
        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("smsCertification");
        String certCode = nodeBindingInfo.getJdbcTemplate().queryForList(" select FLOOR(100000 + (RAND()*899999)) as certCode ").get(0).get("certCode").toString();

        Map<String, Object> smsCertificationData = new HashMap<>();
        smsCertificationData.put("name",name);
        smsCertificationData.put("smsCertificationType", smsCertificationType);
        smsCertificationData.put("memberNo", memberNo);
        smsCertificationData.put("cellphone", cellphone);
        smsCertificationData.put("certCode", certCode);
        smsCertificationData.put("certStatus", certStatus);
        smsCertificationData.put("certRequestDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        nodeService.executeNode(smsCertificationData, "smsCertification", CREATE);

        return certCode;
    }

    public Map<String, String> setHtml(String templateName, String linkUrl){
        Map<String, String> setHtmlMap = new HashMap<>();

        String contextPath = commonService.replaceUrl();
        LocalDateTime date = LocalDateTime.now().plusHours(1);

        String title = "";
        String contents = "";
        String link = "http://localhost:3090"+linkUrl; // http://125.131.88.206:3090

        List<Map<String, Object>> emailTemplateList = nodeBindingService.list("emailTemplate", "name_in=".concat(templateName));

        if(emailTemplateList.size() > 0){
            title = emailTemplateList.get(0).get("title").toString();
            contents = emailTemplateList.get(0).get("contents").toString();

            contents = contents.replaceAll("src=\"http://localhost/assets/images","src="+contextPath+"/image");
            contents = contents.replaceAll("href=\"#\"", "href="+link);
            contents = contents.replaceAll("yyyy-MM-dd", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // yyyy-MM-dd
            contents = contents.replaceAll("HH:mm:ss", date.format(DateTimeFormatter.ofPattern("HH:mm:ss"))); // HH:mm:ss
        }

        setHtmlMap.put("title", title);
        setHtmlMap.put("contents", contents);

        return setHtmlMap;
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
}
