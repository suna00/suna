package net.ion.ice.service;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.*;
import net.ion.ice.core.query.QueryTerm;
import net.ion.ice.core.query.QueryUtils;
import net.ion.ice.core.session.SessionService;
import net.ion.ice.plugin.excel.ExcelService;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service("memberService")
public class MemberService {
    private Logger logger = LoggerFactory.getLogger(MemberService.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NodeBindingService nodeBindingService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ExcelService excelService;


    private CommonService commonService;
    private EmailService emailService;


    public ExecuteContext signIn(ExecuteContext context) {
        String siteId = context.getDataStringValue("siteId");
        String userId = context.getDataStringValue("userId");
        String password = context.getDataStringValue("password");

        if (StringUtils.isEmpty(siteId)) {
            siteId = "default";
        }
        //NodeType memberType = nodeService.getNodeType("member");
        //List<QueryTerm> queryTerms = new ArrayList<>();
        //queryTerms.add(QueryUtils.makePropertyQueryTerm(memberType, "siteId", null, siteId));
        //queryTerms.add(QueryUtils.makePropertyQueryTerm(memberType, "userId", null, userId));

        //List<Node> nodes = nodeService.getNodeList(memberType, queryTerms);
        List<Node> nodes = nodeService.getNodeList("member", "sorting=memberNo desc&siteId_equals="+siteId+"&userId_equals="+userId);

        if (nodes == null || nodes.size() == 0) {
            throw new ApiException("400", "Not Found User");
        }

        Node member = nodes.get(0);
        Integer failedCount = (member.getValue("failedCount") == null ? 0 : Integer.parseInt(member.getValue("failedCount").toString()));

        // 탈퇴회원 체크
        if("leave".equals(member.getBindingValue("memberStatus")) || "leaveRequest".equals(member.getBindingValue("memberStatus"))){
            // 휴면지속자동탈퇴 체크
//            Node leaveMemberNode = nodeService.getNode("requestToleaveMember", member.get("memberNo").toString());
//            if("leave001".equals(leaveMemberNode.getBindingValue("leaveType"))){
//                throw new ApiException("400", "U0013");
//            }

            throw new ApiException("400", "Not Found User");
        }

        // 패스워드 5회 이상 실패 체크
        if (!member.getStringValue("password").equals(password)) {
            failedCount+=1;
            member.put("failedCount", failedCount);
            member.put("lastFailedDate", new Date());
            nodeService.updateNode(member, "member");

            if (5 <= failedCount) {
                throw new ApiException("400", "M0008");
            }
            throw new ApiException("400", "The password is incorrect");
        } else {
            if (0 < failedCount) {
                member.put("failedCount", null);
                member.put("lastFailedDate", null);
                nodeService.updateNode(member, "member");
            }
        }

        Map<String, Object> session = new HashMap<>();
        session.put("member", member);
        session.put("role", "customer");
        try {
            sessionService.putSession(context.getHttpRequest(), context.getHttpResponse(), session);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 비밀번호 교체 안내 체크
        String date = (member.get("lastPasswordChangeDate") == null ? member.get("joinDate").toString() : member.get("lastPasswordChangeDate").toString());
        LocalDateTime limitChangeDate = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).plusMonths(3);
        LocalDateTime now = LocalDateTime.now();
        if(limitChangeDate.isBefore(now)){
            context.setResult(CommonService.getResult("U0007"));
            return context;
        }

        // 휴면회원 체크
        if("sleep".equals(member.getBindingValue("memberStatus"))){
            context.setResult(CommonService.getResult("U0012"));
        } else {
            member.put("lastLoginDate", new Date());
            nodeService.updateNode(member, "member");

            Map<String, Object> item = new HashMap<>();
            item.put("memberNo", member.get("memberNo"));
            context.setResult(CommonService.getResult("U0008", item));
        }

        return context;
    }

    public ExecuteContext me(ExecuteContext context) {
        Map<String, Object> session = null;
        try {
            session = sessionService.getSession(context.getHttpRequest());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (session != null) {
            Node memberNode = (Node) session.get("member");
            Map<String, Object> extraData = new HashMap<>();

            if (null == memberNode) {
                context.setResult(CommonService.getResult("U0011"));    //로그인을 하지않은 사용자
            } else {
                try {
                    sessionService.refreshSession(context.getHttpRequest(), context.getHttpResponse());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                extraData.put("siteId", memberNode.get("siteId"));
                extraData.put("siteType", memberNode.get("siteType"));
                extraData.put("memberNo", memberNode.get("memberNo"));
                extraData.put("userId", memberNode.get("userId"));
                extraData.put("name", memberNode.get("name"));
                extraData.put("cellphone", memberNode.get("cellphone"));
                context.setResult(CommonService.getResult("U0010", extraData));    //로그인을 한 사용자

            }
        }
        return context;
    }

    public ExecuteContext saveMemberInfo(ExecuteContext context) {
        Map<String, Object> contextData = new LinkedHashMap<>(context.getData());
        Map<String, Object> resultObject = new HashMap<>();
        Map<String, Object> item = new HashMap<>();

        Node node;
        try {
            if (contextData.get("memberNo") == null) {
                contextData.put("barcode", setBarcode()); //회원바코드 생성
                node = (Node) nodeService.executeNode(contextData, "member", CommonService.CREATE);

                Map<String, String> emailTemplate = getEmailTemplate("회원가입");
                EmailService.setHtmlMemberJoin(node, node.get("email").toString(), emailTemplate);
            } else {
                if (contextData.containsKey("password")) {
                    if(contextData.containsKey("updateType")){
                        Node memberNode = nodeService.read("member", contextData.get("memberNo").toString());

                        String memberNodePassword = memberNode.getValue("password").toString();
                        Integer failedCount = (memberNode.getValue("failedCount") == null ? 0 : Integer.parseInt(memberNode.getValue("failedCount").toString()));

                        if (!memberNodePassword.equals(contextData.get("oriPassword").toString())) {
                            failedCount+=1;
                            memberNode.put("failedCount", failedCount);
                            memberNode.put("lastFailedDate", new Date());
                            nodeService.updateNode(memberNode, "member");

                            if (failedCount < 5) {
                                commonService.setErrorMessage(context, "M0007");
                                return context;
                            } else {
                                commonService.setErrorMessage(context, "M0008");
                                return context;
                            }
                        }
                    }

                    contextData.put("failedCount", null);
                    contextData.put("lastFailedDate", null);
                }

                node = (Node) nodeService.executeNode(contextData, "member", CommonService.UPDATE);

                if (contextData.containsKey("changeMarketingAgreeYn")) {
                    Map<String, String> emailTemplate = getEmailTemplate("광고성 정보수신동의 결과");
                    EmailService.setHtmlMemberInfoChange(node, node.get("email").toString(), emailTemplate);
                }
            }
        }catch (Exception e){
            throw new ApiException("500", e.getMessage()) ;
        }

        item.put("memberNo", node.get("memberNo"));
        item.put("userId", node.get("userId"));
        item.put("name", node.get("name"));
        item.put("email", node.get("email"));

        resultObject.put("item", item);
        context.setResult(resultObject);

        return context;
    }

    public ExecuteContext certPassword(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        Map<String, Object> resultObject = new HashMap<>();
        Map<String, Object> item = new HashMap<>();

        if (data.containsKey("email")) {
            authenticationCertEmail(context);
        }

        if (data.containsKey("cellphone")) {
            NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("smsCertification");
            String query = " SELECT * FROM smscertification WHERE certCode = ? AND cellphone = ? AND certStatus = 'success' ";
            List<Map<String, Object>> certList = nodeBindingInfo.getJdbcTemplate().queryForList(query, data.get("certCode").toString(), data.get("cellphone").toString());

            if (0 < certList.size()) {
                item.put("memberNo", certList.get(0).get("memberNo"));
                resultObject.put("item", item);
                context.setResult(resultObject);
            } else {
                commonService.setErrorMessage(context, "U0002");
                return context;
            }
        }

        return context;
    }

    public ExecuteContext authenticationSendEmail(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = {"emailCertificationType"};
        if (commonService.requiredParams(context, data, params)) return context;

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("member");
        String emailCertificationType = data.get("emailCertificationType").toString();

        // 회원가입 : join
        if ("join".equals(emailCertificationType)) {
            String siteId = data.get("siteId").toString();
            String email = data.get("email").toString();
            String count = nodeBindingInfo.getJdbcTemplate().queryForList(" select count(memberNo) as count from member where siteId=? and email=? ", siteId, email).get(0).get("count").toString();

            if ("0".equals(count)) {
                try {
                    sendEmail(emailCertificationType, email, data);
                } catch (IOException e) {
                    commonService.setErrorMessage(context, "U0001");
                    return context;
                }
            } else {
                commonService.setErrorMessage(context, "U0001");
                return context;
            }
        }

        // 비밀번호 : password, 휴면회원해제 : sleepMember
        if ("password".equals(emailCertificationType) || "sleepMember".equals(emailCertificationType)) {
            List<Map<String, Object>> memberList;
            if ("password".equals(emailCertificationType)) {
                memberList = nodeBindingInfo.getJdbcTemplate().queryForList(" select * from member where name=? and userId=? order by memberNo desc limit 1", data.get("name").toString(), data.get("userId").toString());
            } else {
                memberList = nodeBindingInfo.getJdbcTemplate().queryForList(" select * from member where name=? and email=? order by memberNo desc limit 1", data.get("name").toString(), data.get("email").toString());
            }

            if (0 < memberList.size()) {
                Map<String, Object> member = memberList.get(0);

                if("leave".equals(member.get("memberStatus").toString()) || "leaveRequest".equals(member.get("memberStatus").toString())){
                    commonService.setErrorMessage(context, "L0003");
                    return context;
                }

                try {
                    sendEmail(emailCertificationType, member.get("email").toString(), member);
                } catch (IOException e) {
                    commonService.setErrorMessage(context, "U0004");
                    return context;
                }

                Map<String, Object> resultObject = new HashMap<>();
                Map<String, Object> item = new HashMap<>();

                item.put("email", member.get("email"));
                resultObject.put("item", item);
                context.setResult(resultObject);
            } else {
                commonService.setErrorMessage(context, "U0004");
                return context;
            }
        }

        return context;
    }

    public ExecuteContext authenticationSendSms(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        Map<String, Object> resultObject = new HashMap<>();
        Map<String, Object> item = new HashMap<>();

        String[] params = {"smsCertificationType"};
        if (commonService.requiredParams(context, data, params)) return context;

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("member");
        String smsCertificationType = data.get("smsCertificationType").toString();
        String cellphone = data.get("cellphone").toString();

        // 아이디 : id
        if ("id".equals(smsCertificationType)) {
            List<Map<String, Object>> memberList = nodeBindingInfo.getJdbcTemplate().queryForList(" SELECT * FROM member WHERE cellphone=? ORDER BY memberNo DESC LIMIT 1", cellphone);

            if (0 < memberList.size()) {
                Map<String, Object> member = memberList.get(0);

                if("leave".equals(member.get("memberStatus").toString()) || "leaveRequest".equals(member.get("memberStatus").toString())){
                    commonService.setErrorMessage(context, "L0003");
                    return context;
                }

                sendSms(smsCertificationType, cellphone, member);

                item.put("cellphone", member.get("cellphone"));
                resultObject.put("item", item);
                context.setResult(resultObject);
            } else {
                commonService.setErrorMessage(context, "U0005");
                return context;
            }
        }

        // 패스워드 : password, 휴면회원해제 : sleepMember
        if ("password".equals(smsCertificationType) || "sleepMember".equals(smsCertificationType)) {
            List<Map<String, Object>> memberList;

            if ("password".equals(smsCertificationType)) {
                memberList = nodeBindingInfo.getJdbcTemplate().queryForList(" select * from member where name=? and userId=? and cellphone=? order by memberNo desc limit 1 ", data.get("name").toString(), data.get("userId").toString(), cellphone);
            } else {
                memberList = nodeBindingInfo.getJdbcTemplate().queryForList(" select * from member where name=? and email=? and cellphone=? order by memberNo desc limit 1", data.get("name").toString(), data.get("email").toString(), cellphone);
            }

            if (0 < memberList.size()) {
                Map<String, Object> member = memberList.get(0);

                if("leave".equals(member.get("memberStatus").toString()) || "leaveRequest".equals(member.get("memberStatus").toString())){
                    commonService.setErrorMessage(context, "L0003");
                    return context;
                }

                sendSms(smsCertificationType, cellphone, member);

                item.put("memberNo", member.get("memberNo"));
                item.put("cellphone", member.get("cellphone"));
                resultObject.put("item", item);
                context.setResult(resultObject);
            } else {
                commonService.setErrorMessage(context, "U0004");
                return context;
            }
        }

        return context;
    }

    public ExecuteContext authenticationCertEmail(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = {"certCode"};
        if (commonService.requiredParams(context, data, params)) return context;

        String certCode = data.get("certCode").toString();

        if("memberRegistBannerCert".equals(certCode)){
            return context;
        } else {
            if(data.get("email") != null){
                String email = data.get("email").toString();

                NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("emailCertification");
                String query = " SELECT emailcertificationId, email, memberNo, certCode, certStatus, certRequestDate, date_add(certRequestDate, INTERVAL +60 MINUTE) AS certExpireDate, (certStatus = 'request' AND date_add(certRequestDate, INTERVAL +60 MINUTE) > now() AND certCode = ?) AS available FROM emailcertification WHERE certCode = ? and email = ? limit 1";
                List<Map<String, Object>> list = nodeBindingInfo.getJdbcTemplate().queryForList(query, certCode, certCode, email);

                if (0 < list.size()) {
                    Map<String, Object> map = list.get(0);
                    String available = map.get("available").toString();

                    if ("1".equals(available)) {
                        Map<String, Object> emailCertificationData = new HashMap<>();
                        emailCertificationData.put("emailCertificationId", map.get("emailCertificationId"));
                        emailCertificationData.put("certStatus", "success");
                        emailCertificationData.put("certSuccessDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

                        nodeService.executeNode(emailCertificationData, "emailCertification", CommonService.UPDATE);
                        Map<String, Object> resultObject = new HashMap<>();
                        Map<String, Object> item = new HashMap<>();

                        item.put("memberNo", map.get("memberNo"));
                        item.put("email", email); // 회원가입
                        resultObject.put("item", item);

                        context.setResult(resultObject);
                    } else {
                        commonService.setErrorMessage(context, "U0003");
                    }
                } else {
                    commonService.setErrorMessage(context, "U0002");
                }
            } else {
                commonService.setErrorMessage(context, "U0002");
            }
        }

        return context;
    }

    public ExecuteContext authenticationCertSms(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = {"certCode"};
        if (commonService.requiredParams(context, data, params)) return context;

        String certCode = data.get("certCode").toString();
        String cellphone = data.get("cellphone").toString();

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("smsCertification");
        String query = " SELECT smsCertificationId, cellphone, memberNo, certCode, certStatus, certRequestDate, date_add(certRequestDate, INTERVAL +3 MINUTE) AS certExpireDate, (certStatus = 'request' AND date_add(certRequestDate, INTERVAL +3 MINUTE) > now() AND certCode = ?) AS available FROM smscertification WHERE certCode = ? AND cellphone = ? limit 1";
        List<Map<String, Object>> list = nodeBindingInfo.getJdbcTemplate().queryForList(query, certCode, certCode, cellphone);

        if (0 < list.size()) {
            Map<String, Object> map = list.get(0);
            String available = map.get("available").toString();

            if ("1".equals(available)) {
                Map<String, Object> smsCertificationData = new HashMap<>();
                smsCertificationData.put("smsCertificationId", map.get("smsCertificationId"));
                smsCertificationData.put("certStatus", "success");
                smsCertificationData.put("certSuccessDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

                nodeService.executeNode(smsCertificationData, "smsCertification", CommonService.UPDATE);

                Map<String, Object> resultObject = new HashMap<>();
                Map<String, Object> item = new HashMap<>();

                item.put("memberNo", map.get("memberNo"));
                resultObject.put("item", item);

                context.setResult(resultObject);
            } else {
                commonService.setErrorMessage(context, "U0003");
            }
        } else {
            commonService.setErrorMessage(context, "U0006");
        }

        return context;
    }

    public String setBarcode() {
        String barcode = "";

        for (int i = 0; i < 4; i++) {
            Integer randomInt = ((int) (Math.random() * 10000) + 1000);
            if (10000 <= randomInt) {
                randomInt = randomInt - 1000;
            }
            barcode += randomInt;
        }

        barcode = barcode.substring(0, barcode.length() - 1);
        return barcode;
    }

    public void sendEmail(String emailCertificationType, String email, Map<String, Object> data) throws IOException {
        Map<String, String> html = new HashMap<>();

        // 회원가입 : join
        if ("join".equals(emailCertificationType)) {
            String certCode = getEmailCertCode("이메일 인증요청", emailCertificationType, null, email, "request");
            String linkUrl = data.get("siteId") + "/signUp/stepTwo?certCode=" + certCode + "&email=" + email + "&affiliateId=" + data.get("affiliateId") + "&inflowRoute=" + data.get("inflowRoute") + "&acceptTermsYn=" + data.get("acceptTermsYn") + "&receiveMarketingEmailAgreeYn=" + data.get("receiveMarketingEmailAgreeYn") + "&receiveMarketingSMSAgreeYn=" + data.get("receiveMarketingSMSAgreeYn");
            html = setHtml("본인인증", linkUrl);
        }

        // 비밀번호 : password
        if ("password".equals(emailCertificationType)) {
            String certCode = getEmailCertCode("이메일 인증요청", emailCertificationType, data.get("memberNo").toString(), email, "request");
            String linkUrl = data.get("siteId") + "/signIn/changePassword?certCode=" + certCode + "&email=" + email;
            html = setHtml("비밀번호변경", linkUrl);
        }

        // 휴면회원해제 : sleepMember
        if ("sleepMember".equals(emailCertificationType)) {
            String certCode = getEmailCertCode("이메일 인증요청", emailCertificationType, data.get("memberNo").toString(), email, "request");
            String linkUrl = data.get("siteId") + "/signIn/changePassword?certCode=" + certCode + "&email=" + email + "&certificationType=sleepMember";
            html = setHtml("휴면해제이메일인증", linkUrl);
        }

        emailService.sendEmailDirect(email, html.get("title"), html.get("contents"));
    }

    public void sendSms(String smsCertificationType, String cellphone, Map<String, Object> data) {
        // 아이디 : id, 패스워드 : password, 휴면회원 : sleepMember

        String certCdoe = getSmsCertCode("SMS 인증요청", smsCertificationType, data.get("memberNo").toString(), cellphone, "request");
        String Message = "인증번호[" + certCdoe + "]";

        System.out.println(Message);
        // 문자 전송
    }

    public String getEmailCertCode(String name, String emailCertificationType, String memberNo, String email, String certStatus) {
        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("emailCertification");
        String certCode = nodeBindingInfo.getJdbcTemplate().queryForList(" select to_base64(concat('ygoon!@#SHOP ',now())) as certCode ").get(0).get("certCode").toString();

        Map<String, Object> emailCertificationData = new HashMap<>();
        emailCertificationData.put("name", name);
        emailCertificationData.put("emailCertificationType", emailCertificationType);
        emailCertificationData.put("memberNo", memberNo);
        emailCertificationData.put("email", email);
        emailCertificationData.put("certCode", certCode);
        emailCertificationData.put("certStatus", certStatus);
        emailCertificationData.put("certRequestDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        nodeService.executeNode(emailCertificationData, "emailCertification", CommonService.CREATE);

        return certCode;
    }

    public String getSmsCertCode(String name, String smsCertificationType, String memberNo, String cellphone, String certStatus) {
        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo("smsCertification");
        String certCode = nodeBindingInfo.getJdbcTemplate().queryForList(" select FLOOR(100000 + (RAND()*899999)) as certCode ").get(0).get("certCode").toString();

        Map<String, Object> smsCertificationData = new HashMap<>();
        smsCertificationData.put("name", name);
        smsCertificationData.put("smsCertificationType", smsCertificationType);
        smsCertificationData.put("memberNo", memberNo);
        smsCertificationData.put("cellphone", cellphone);
        smsCertificationData.put("certCode", certCode);
        smsCertificationData.put("certStatus", certStatus);
        smsCertificationData.put("certRequestDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        nodeService.executeNode(smsCertificationData, "smsCertification", CommonService.CREATE);

        return certCode;
    }

    public Map<String, String> setHtml(String templateName, String linkUrl) {
        Map<String, String> setHtmlMap = new HashMap<>();

        String contextPath = commonService.replaceUrl();
        LocalDateTime date = LocalDateTime.now().plusHours(1);

        String title = "";
        String contents = "";
        String link = ApplicationContextManager.getContext().getEnvironment().getProperty("cluster.front-prefix") + linkUrl; // http://125.131.88.206:3090

        List<Map<String, Object>> emailTemplateList = nodeBindingService.list("emailTemplate", "name_in=".concat(templateName));

        if (emailTemplateList.size() > 0) {
            title = emailTemplateList.get(0).get("title").toString();
            contents = emailTemplateList.get(0).get("contents").toString();

            contents = contents.replaceAll("src=\"http://localhost/assets/images", "src=" + contextPath + "/image");
            contents = contents.replaceAll("href=\"#\"", "href=" + link);
            contents = contents.replaceAll("yyyy-MM-dd", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))); // yyyy-MM-dd
            contents = contents.replaceAll("HH:mm:ss", date.format(DateTimeFormatter.ofPattern("HH:mm:ss"))); // HH:mm:ss
        }

        setHtmlMap.put("title", title);
        setHtmlMap.put("contents", contents);

        return setHtmlMap;
    }

    public ExecuteContext leaveMembership(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = {"memberNo", "leaveType", "reasonType"};
        if (commonService.requiredParams(context, data, params)) return context;

        List<Node> list = nodeService.getNodeList("orderProduct", "memberNo_matching=" + data.get("memberNo"));
        if (list.size() > 0) {
            for (Node node : list) {
                String orderStatus = node.getValue("orderStatus").toString();
//                배송완료,구매확정,취소완료,교환배송완료,반품완료
                if (!("order006".equals(orderStatus) || "order007".equals(orderStatus) || "order009".equals(orderStatus) || "order016".equals(orderStatus) || "order021".equals(orderStatus))) {
                    commonService.setErrorMessage(context, "L0001");
                    return context;
                }
            }
        }

        Node node = nodeService.getNode("member", data.get("memberNo").toString());
        if (node == null) {
            commonService.setErrorMessage(context, "L0002");
            return context;
        }

        Node leave = nodeService.getNode("requestToleaveMember", data.get("memberNo").toString());
        if (leave != null) {
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
    public ExecuteContext getTempPassword(ExecuteContext context) {
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("password", randomPassword());

        context.setResult(object);

        return context;
    }

    public String randomPassword() {
        char pwCollection[] = new char[]{
                '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                '!', '@', '#', '$', '%', '^', '&'};

        String ranPw = "";

        for (int i = 0; i < 10; i++) {
            int selectRandomPw = (int) (Math.random() * (pwCollection.length));//Math.rondom()은 0.0이상 1.0미만의 난수를 생성해 준다.
            ranPw += pwCollection[selectRandomPw];
        }
        return ranPw;
    }

    public Map<String, String> getEmailTemplate(String emailName) {
        Map<String, String> setHtmlMap = new HashMap<>();
        String title = "";
        String contents = "";

        List<Map<String, Object>> emailTemplateList = nodeBindingService.list("emailTemplate", "name_in=".concat(emailName));
        if (emailTemplateList.size() > 0) {
            title = emailTemplateList.get(0).get("title").toString();
            contents = emailTemplateList.get(0).get("contents").toString();
        }
        setHtmlMap.put("title", title);
        setHtmlMap.put("contents", contents);

        return setHtmlMap;
    }

    public ExecuteContext downloadExcelForm(ExecuteContext context) {
        HttpServletRequest request = context.getHttpRequest();
        HttpServletResponse response = context.getHttpResponse();

        Map<String, Object> params = context.getData();
        String siteType = params.get("siteType") == null ? "" : params.get("siteType").toString();
        String fileName = StringUtils.equals(siteType, "company") ? "기업회원정보 등록 양식" : "대학회원정보 등록 양식";

        Workbook workbook = null;
        OutputStream outputStream = null;

        try {
            workbook = excelService.getXlsxWorkbook();

            if (StringUtils.equals(siteType, "company")) {
                setCompanySheet(workbook);
            } else if (StringUtils.equals(siteType, "university")) {
                setUniversitySheet(workbook);
            }

            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+excelService.encodingFileName(request, fileName)+"\".xlsx");
            outputStream = response.getOutputStream();
            workbook.write(outputStream);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        return context;
    }

    private void setCompanySheet(Workbook workbook) {
        Sheet sheet1 = workbook.createSheet("기업회원정보");
        Row row1 = sheet1.createRow(0);

        Cell cell0 = row1.createCell(0);
        cell0.setCellValue("기업");
        cell0.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell1 = row1.createCell(1);
        cell1.setCellValue("회원등급");
        cell1.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell2 = row1.createCell(2);
        cell2.setCellValue("아이디");
        cell2.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell3 = row1.createCell(3);
        cell3.setCellValue("비밀번호");
        cell3.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell4 = row1.createCell(4);
        cell4.setCellValue("이름");
        cell4.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell5 = row1.createCell(5);
        cell5.setCellValue("휴대폰 번호");
        cell5.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell6 = row1.createCell(6);
        cell6.setCellValue("기타 연락처");
        cell6.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell7 = row1.createCell(7);
        cell7.setCellValue("우편번호");
        cell7.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell8 = row1.createCell(8);
        cell8.setCellValue("주소");
        cell8.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell9 = row1.createCell(9);
        cell9.setCellValue("상세주소");
        cell9.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell10 = row1.createCell(10);
        cell10.setCellValue("성별");
        cell10.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell11 = row1.createCell(11);
        cell11.setCellValue("관심분야");
        cell11.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell12 = row1.createCell(12);
        cell12.setCellValue("마케팅 이메일 수신동의");
        cell12.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell13 = row1.createCell(13);
        cell13.setCellValue("마케팅 SMS 수신동의");
        cell13.setCellStyle(excelService.getHeaderCellStyle(workbook));

        for (int i=0; i<14; i++) {
            sheet1.autoSizeColumn(i);
        }

        Sheet sheet2 = workbook.createSheet("기업 코드");
        setAffiliateSheet(workbook, sheet2, "company");

        Sheet sheet3 = workbook.createSheet("회원등급 코드");
        setCodeSheet(workbook, sheet3, "membershipLevel");

        Sheet sheet4 = workbook.createSheet("성별 코드");
        setCodeSheet(workbook, sheet4, "genderType");

        Sheet sheet5 = workbook.createSheet("관심분야 코드");
        setCodeSheet(workbook, sheet5, "interests");

        Sheet sheet6 = workbook.createSheet("마케팅 이메일 수신동의 코드");
        setCodeSheet(workbook, sheet6, "agreeYn");

        Sheet sheet7 = workbook.createSheet("마케팅 SMS 수신동의 코드");
        setCodeSheet(workbook, sheet7, "agreeYn");
    }

    private void setUniversitySheet(Workbook workbook) {
        Sheet sheet1 = workbook.createSheet("대학회원정보");
        Row row1 = sheet1.createRow(0);

        Cell cell0 = row1.createCell(0);
        cell0.setCellValue("대학");
        cell0.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell1 = row1.createCell(1);
        cell1.setCellValue("회원등급");
        cell1.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell2 = row1.createCell(2);
        cell2.setCellValue("아이디");
        cell2.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell3 = row1.createCell(3);
        cell3.setCellValue("비밀번호");
        cell3.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell4 = row1.createCell(4);
        cell4.setCellValue("이름");
        cell4.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell5 = row1.createCell(5);
        cell5.setCellValue("휴대폰 번호");
        cell5.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell6 = row1.createCell(6);
        cell6.setCellValue("기타 연락처");
        cell6.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell7 = row1.createCell(7);
        cell7.setCellValue("우편번호");
        cell7.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell8 = row1.createCell(8);
        cell8.setCellValue("주소");
        cell8.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell9 = row1.createCell(9);
        cell9.setCellValue("상세주소");
        cell9.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell10 = row1.createCell(10);
        cell10.setCellValue("학부/학과");
        cell10.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell11 = row1.createCell(11);
        cell11.setCellValue("교직원");
        cell11.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell12 = row1.createCell(12);
        cell12.setCellValue("입학년도");
        cell12.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell13 = row1.createCell(13);
        cell13.setCellValue("성별");
        cell13.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell14 = row1.createCell(14);
        cell14.setCellValue("관심분야");
        cell14.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell15 = row1.createCell(15);
        cell15.setCellValue("마케팅 이메일 수신동의");
        cell15.setCellStyle(excelService.getHeaderCellStyle(workbook));

        Cell cell16 = row1.createCell(16);
        cell16.setCellValue("마케팅 SMS 수신동의");
        cell16.setCellStyle(excelService.getHeaderCellStyle(workbook));

        for (int i=0; i<17; i++) {
            sheet1.autoSizeColumn(i);
        }

        Sheet sheet2 = workbook.createSheet("대학 코드");
        setAffiliateSheet(workbook, sheet2, "university");

        Sheet sheet3 = workbook.createSheet("회원등급 코드");
        setCodeSheet(workbook, sheet3, "membershipLevel");

        Sheet sheet4 = workbook.createSheet("성별 코드");
        setCodeSheet(workbook, sheet4, "genderType");

        Sheet sheet5 = workbook.createSheet("관심분야 코드");
        setCodeSheet(workbook, sheet5, "interests");

        Sheet sheet6 = workbook.createSheet("마케팅 이메일 수신동의 코드");
        setCodeSheet(workbook, sheet6, "agreeYn");

        Sheet sheet7 = workbook.createSheet("마케팅 SMS 수신동의 코드");
        setCodeSheet(workbook, sheet7, "agreeYn");
    }

    private void setAffiliateSheet(Workbook workbook, Sheet sheet, String siteType) {
        Row headerRow = sheet.createRow(0);
        Cell headerCell0 = headerRow.createCell(0);
        headerCell0.setCellValue(StringUtils.equals(siteType, "company") ? "기업 코드값" : "대학 코드값");
        headerCell0.setCellStyle(excelService.getHeaderCellStyle(workbook));
        Cell headerCell1 = headerRow.createCell(1);
        headerCell1.setCellValue(StringUtils.equals(siteType, "company") ? "기업 코드명" : "대학 코드명");
        headerCell1.setCellStyle(excelService.getHeaderCellStyle(workbook));

        int rowCount = 1;
        List<Node> affiliateList = (List<Node>) NodeQuery.build("affiliate").matching("siteType", siteType).matching("affiliateStatus", "y").sorting("created desc").getList();
        for (Node affiliateNode : affiliateList) {
            Row dataRow = sheet.createRow(rowCount);
            Cell dataCell0 = dataRow.createCell(0);
            dataCell0.setCellValue(affiliateNode.getBindingValue("affiliateId").toString());
            Cell dataCell1 = dataRow.createCell(1);
            dataCell1.setCellValue(affiliateNode.getBindingValue("name").toString());

            rowCount++;
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void setCodeSheet(Workbook workbook, Sheet sheet, String upperCode) {
        Row headerRow = sheet.createRow(0);
        Cell headerCell0 = headerRow.createCell(0);
        headerCell0.setCellValue("코드값");
        headerCell0.setCellStyle(excelService.getHeaderCellStyle(workbook));
        Cell headerCell1 = headerRow.createCell(1);
        headerCell1.setCellValue("코드명");
        headerCell1.setCellStyle(excelService.getHeaderCellStyle(workbook));

        int rowCount = 1;
        List<Node> codeList = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", upperCode).matching("commonCodeStatus", "y").sorting("sortOrder asc").getList();
        for (Node codeNode : codeList) {
            Row dataRow = sheet.createRow(rowCount);
            Cell dataCell0 = dataRow.createCell(0);
            dataCell0.setCellValue(codeNode.getBindingValue("code").toString());
            Cell dataCell1 = dataRow.createCell(1);
            dataCell1.setCellValue(codeNode.getBindingValue("name").toString());

            rowCount++;
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    public ExecuteContext uploadExcel(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        MultipartFile file = data.get("excelFile") == null ? null : (MultipartFile) data.get("excelFile");
        String siteType = data.get("siteType") == null ? "" : data.get("siteType").toString();
        String fileName = file.getOriginalFilename();

        Map<String, Object> parsedResult = excelService.parsingExcelFile(file);
        Map<String, Object> saveResult = new HashMap<>();

        if (StringUtils.equals(siteType, "company")) {
            saveResult = saveCompanyMembers(parsedResult);
        } else if (StringUtils.equals(siteType, "university")) {
            saveResult = saveUniversityMembers(parsedResult);
        }

        Map<String, Object> affiliateMemberUploadData = new HashMap<>();
        affiliateMemberUploadData.put("siteType", siteType);
        affiliateMemberUploadData.put("name", fileName);
        affiliateMemberUploadData.put("file", file);
        affiliateMemberUploadData.put("successCount", saveResult.get("successCount"));
        affiliateMemberUploadData.put("failCount", saveResult.get("failCount"));
        affiliateMemberUploadData.put("failDescription", saveResult.get("failDescription"));

        Node result = (Node) nodeService.executeNode(affiliateMemberUploadData, "affiliateMemberUpload", EventService.CREATE);
        context.setResult(result);

        return context;
    }

    private Map<String, Object> saveCompanyMembers(Map<String, Object> parsedResult) {
        Iterator<String> parsedResultKeyIterator = parsedResult.keySet().iterator();
        String firstKey = parsedResultKeyIterator.next();

        StringBuffer failDescription = new StringBuffer();
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> creatableItems = new ArrayList<>();

        if (parsedResult.get(firstKey) != null) {
            List<Map<String, String>> memberDataList = (List<Map<String, String>>) parsedResult.get("기업회원정보");
            int rowIndex = 1;
            for (Map<String, String> memberData : memberDataList) {
                List<String> values = new ArrayList<>(memberData.values());

                String affiliateId = values.get(0);
                String membershipLevel = values.get(1);
                String userId = values.get(2);
                String password = values.get(3);
                String name = values.get(4);
                String cellphone = values.get(5);
                String phone = values.get(6);
                String postCode = values.get(7);
                String address = values.get(8);
                String detailedAddress = values.get(9);
                String gender = values.get(10);
                String interests = values.get(11);
                String receiveMarketingEmailAgreeYn = values.get(12);
                String receiveMarketingSMSAgreeYn = values.get(13);

                boolean validation = false;

                if (StringUtils.isEmpty(affiliateId)) {
                    failDescription.append(String.format("%d 행 1 열 대학 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(affiliateId)) {
                    Node affiliateNode = nodeService.getNode("affiliate", affiliateId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 1 열 잘못된 대학 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(membershipLevel)) {
                    failDescription.append(String.format("%d 행 2 열 회원등급 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(membershipLevel)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "membershipLevel").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, membershipLevel)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 2 열 잘못된 회원등급 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(userId)) {
                    failDescription.append(String.format("%d 행 3 열 아이디 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(userId)) {
                    Node memberNode = nodeService.getNode("member", userId);
                    if (memberNode != null) {
                        failDescription.append(String.format("%d 행 3 열 이미 등록된 아이디 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(password)) {
                    failDescription.append(String.format("%d 행 4 열 비밀번호 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(name)) {
                    failDescription.append(String.format("%d 행 5 열 이름 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(gender)) {
                    failDescription.append(String.format("%d 행 11 열 성별 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(gender)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "genderType").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, gender)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 11 열 잘못된 성별 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (!StringUtils.isEmpty(interests)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "interests").matching("commonCodeStatus", "y").getList();
                    List<String> interestsValues = Arrays.asList(StringUtils.split(interests, ","));
                    boolean codeValidation = true;
                    for (String interestsValue : interestsValues) {
                        boolean codeSubValidation = false;
                        for (Node codeNode : codeNodes) {
                            String code = codeNode.getBindingValue("code").toString();
                            if (StringUtils.equals(code, interestsValue)) { codeSubValidation = true; }
                        }
                        if (!codeSubValidation) {
                            codeValidation = false;
                            break;
                        }
                    }
                    if (!codeValidation) {
                        failDescription.append(String.format("%d 행 12 열 잘못된 관심분야 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(receiveMarketingEmailAgreeYn)) {
                    failDescription.append(String.format("%d 행 13 열 마케팅 이메일 수신동의 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(receiveMarketingEmailAgreeYn)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "agreeYn").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, receiveMarketingEmailAgreeYn)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 13 열 잘못된 마케팅 이메일 수신동의 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(receiveMarketingSMSAgreeYn)) {
                    failDescription.append(String.format("%d 행 14 열 마케팅 SMS 수신동의 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(receiveMarketingSMSAgreeYn)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "agreeYn").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, receiveMarketingSMSAgreeYn)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 14 열 잘못된 마케팅 SMS 수신동의 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }

                if (validation) {
                    failCount++;
                } else {
                    Map<String, Object> createMemberData = new HashMap<>();
                    createMemberData.put("affiliateId", affiliateId);
                    createMemberData.put("membershipLevel", membershipLevel);
                    createMemberData.put("userId", userId);
                    createMemberData.put("password", password);
                    createMemberData.put("name", name);
                    createMemberData.put("cellphone", cellphone);
                    createMemberData.put("phone", phone);
                    createMemberData.put("postCode", postCode);
                    createMemberData.put("address", address);
                    createMemberData.put("detailedAddress", detailedAddress);
                    createMemberData.put("gender", gender);
                    createMemberData.put("interests", interests);
                    createMemberData.put("receiveMarketingEmailAgreeYn", receiveMarketingEmailAgreeYn);
                    createMemberData.put("receiveMarketingSMSAgreeYn", receiveMarketingSMSAgreeYn);
                    createMemberData.put("siteType", "company");
                    createMemberData.put("memberStatus", "join");

                    creatableItems.add(createMemberData);

                    successCount++;
                }

                rowIndex++;
            }
        }

        if (failCount == 0) {
            for (Map<String, Object> creatableItem : creatableItems) {
                nodeService.executeNode(creatableItem, "member", EventService.CREATE);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failDescription", failDescription.toString());

        return result;
    }

    private Map<String, Object> saveUniversityMembers(Map<String, Object> parsedResult) {
        Iterator<String> parsedResultKeyIterator = parsedResult.keySet().iterator();
        String firstKey = parsedResultKeyIterator.next();

        StringBuffer failDescription = new StringBuffer();
        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> creatableItems = new ArrayList<>();

        if (parsedResult.get(firstKey) != null) {
            List<Map<String, String>> memberDataList = (List<Map<String, String>>) parsedResult.get("대학회원정보");
            int rowIndex = 1;
            for (Map<String, String> memberData : memberDataList) {
                List<String> values = new ArrayList<>(memberData.values());

                String affiliateId = values.get(0);
                String membershipLevel = values.get(1);
                String userId = values.get(2);
                String password = values.get(3);
                String name = values.get(4);
                String cellphone = values.get(5);
                String phone = values.get(6);
                String postCode = values.get(7);
                String address = values.get(8);
                String detailedAddress = values.get(9);
                String major = values.get(10);
                String uniEmployee = values.get(11);
                String entranceYear = values.get(12);
                String gender = values.get(13);
                String interests = values.get(14);
                String receiveMarketingEmailAgreeYn = values.get(15);
                String receiveMarketingSMSAgreeYn = values.get(16);

                boolean validation = false;

                if (StringUtils.isEmpty(affiliateId)) {
                    failDescription.append(String.format("%d 행 1 열 기업 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(affiliateId)) {
                    Node affiliateNode = nodeService.getNode("affiliate", affiliateId);
                    if (affiliateNode == null) {
                        failDescription.append(String.format("%d 행 1 열 잘못된 기업 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(membershipLevel)) {
                    failDescription.append(String.format("%d 행 2 열 회원등급 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(membershipLevel)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "membershipLevel").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, membershipLevel)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 2 열 잘못된 회원등급 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(userId)) {
                    failDescription.append(String.format("%d 행 3 열 아이디 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(userId)) {
                    Node memberNode = nodeService.getNode("member", userId);
                    if (memberNode != null) {
                        failDescription.append(String.format("%d 행 3 열 이미 등록된 아이디 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(password)) {
                    failDescription.append(String.format("%d 행 4 열 비밀번호 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(name)) {
                    failDescription.append(String.format("%d 행 5 열 이름 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (StringUtils.isEmpty(gender)) {
                    failDescription.append(String.format("%d 행 11 열 성별 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(gender)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "genderType").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, gender)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 11 열 잘못된 성별 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (!StringUtils.isEmpty(interests)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "interests").matching("commonCodeStatus", "y").getList();
                    List<String> interestsValues = Arrays.asList(StringUtils.split(interests, ","));
                    boolean codeValidation = true;
                    for (String interestsValue : interestsValues) {
                        boolean codeSubValidation = false;
                        for (Node codeNode : codeNodes) {
                            String code = codeNode.getBindingValue("code").toString();
                            if (StringUtils.equals(code, interestsValue)) { codeSubValidation = true; }
                        }
                        if (!codeSubValidation) {
                            codeValidation = false;
                            break;
                        }
                    }
                    if (!codeValidation) {
                        failDescription.append(String.format("%d 행 12 열 잘못된 관심분야 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(receiveMarketingEmailAgreeYn)) {
                    failDescription.append(String.format("%d 행 13 열 마케팅 이메일 수신동의 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(receiveMarketingEmailAgreeYn)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "agreeYn").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, receiveMarketingEmailAgreeYn)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 13 열 잘못된 마케팅 이메일 수신동의 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }
                if (StringUtils.isEmpty(receiveMarketingSMSAgreeYn)) {
                    failDescription.append(String.format("%d 행 14 열 마케팅 SMS 수신동의 값이 비어있습니다.\r\n", rowIndex));
                    validation = true;
                }
                if (!StringUtils.isEmpty(receiveMarketingSMSAgreeYn)) {
                    List<Node> codeNodes = (List<Node>) NodeQuery.build("commonCode").matching("upperCode", "agreeYn").matching("commonCodeStatus", "y").getList();
                    boolean codeValidation = true;
                    for (Node codeNode : codeNodes) {
                        String code = codeNode.getBindingValue("code").toString();
                        if (StringUtils.equals(code, receiveMarketingSMSAgreeYn)) codeValidation = false;
                    }
                    if (codeValidation) {
                        failDescription.append(String.format("%d 행 14 열 잘못된 마케팅 SMS 수신동의 값입니다.\r\n", rowIndex));
                        validation = true;
                    }
                }

                if (validation) {
                    failCount++;
                } else {
                    Map<String, Object> createMemberData = new HashMap<>();
                    createMemberData.put("affiliateId", affiliateId);
                    createMemberData.put("membershipLevel", membershipLevel);
                    createMemberData.put("userId", userId);
                    createMemberData.put("password", password);
                    createMemberData.put("name", name);
                    createMemberData.put("cellphone", cellphone);
                    createMemberData.put("phone", phone);
                    createMemberData.put("postCode", postCode);
                    createMemberData.put("address", address);
                    createMemberData.put("detailedAddress", detailedAddress);
                    createMemberData.put("major", major);
                    createMemberData.put("uniEmployee", uniEmployee);
                    createMemberData.put("entranceYear", entranceYear);
                    createMemberData.put("gender", gender);
                    createMemberData.put("interests", interests);
                    createMemberData.put("receiveMarketingEmailAgreeYn", receiveMarketingEmailAgreeYn);
                    createMemberData.put("receiveMarketingSMSAgreeYn", receiveMarketingSMSAgreeYn);
                    createMemberData.put("siteType", "university");
                    createMemberData.put("memberStatus", "join");

                    creatableItems.add(createMemberData);

                    successCount++;
                }

                rowIndex++;
            }
        }

        if (failCount == 0) {
            for (Map<String, Object> creatableItem : creatableItems) {
                nodeService.executeNode(creatableItem, "member", EventService.CREATE);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failDescription", failDescription.toString());

        return result;
    }
}
