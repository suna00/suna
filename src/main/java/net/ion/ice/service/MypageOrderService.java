package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.context.ReadContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.*;
import net.ion.ice.core.session.SessionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.SocketException;
import java.security.Security;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("mypageOrderService")
public class MypageOrderService {

    private static Logger logger = LoggerFactory.getLogger(MypageOrderService.class);

    public static final String orderSheet_TID = "orderSheet";
    public static final String orderProduct_TID = "orderProduct";
    public static final String commonResource_TID = "commonResource";

    @Autowired
    private SessionService sessionService;
    @Autowired
    private NodeBindingService nodeBindingService;
    @Autowired
    private NodeService nodeService;


    public ExecuteContext accountNumberValidation(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String strNm = JsonUtils.getStringValue(data, "name");
        String strAccountNo = JsonUtils.getStringValue(data, "accountNumber");
        String strBankCode = JsonUtils.getStringValue(data, "bankCode");

        String svcGbn = "2";                // 업무구분
        String service = "2";               // 계좌소유주 성명확인 서비스 구분
        String strResId = "";               // 생년월일(사업자 번호,법인번호)
        String niceUid = "NID101893";       // 나이스평가정보에서 고객사에 부여한 구분 id
        String svcPwd = "101893";           // 나이스평가정보에서 고객사에 부여한 서비스 이용 패스워드
        String inq_rsn = "20";              // 조회사유 - 10:회원가입 20:기존회원가입 30:성인인증 40:비회원확인 90:기타사유
        String svc_cls = "";                 // 내-외국인구분
        String strGbn = "1";                // 1 : 개인, 2: 사업자
        String strOrderNo = sdf.format(new Date()) + (Math.round(Math.random() * 10000000000L) + "");

        String result = start(niceUid, svcPwd, service, strGbn, strResId, strNm, strBankCode, strAccountNo, svcGbn, strOrderNo, svc_cls, inq_rsn);

        String[] results = result.split("\\|");
        String resultOrderNo = results[0];
        String resultCode = results[1];
        String resultMsg = results[2];

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("resultOrderNo", resultOrderNo);
        resultMap.put("resultCode", resultCode);
        resultMap.put("resultMsg", resultMsg);
        context.setResult(resultMap);

        return context;
    }

    public ExecuteContext getMypageOrderList(ExecuteContext context) {
        Map<String, Object> session = null;
        try {
            session = sessionService.getSession(context.getHttpRequest());
            String memberNo = JsonUtils.getStringValue(session, "member.memberNo");
            getOrderSheetList(context, memberNo);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return context;
    }

    public ExecuteContext getAdminOrderList(ExecuteContext context) {
        getOrderSheetList(context, "");

        return context;
    }

    public void getOrderSheetList(ExecuteContext context, String memberNo) {
        Map<String, Object> data = context.getData();
        int pageSize = (JsonUtils.getIntValue(data, "pageSize") == 0 ? 10 : JsonUtils.getIntValue(data, "pageSize"));
        int page = JsonUtils.getIntValue(data, "page");
        int currentPage = (page == 0 ? 1 : page);
        String createdFromto = JsonUtils.getStringValue(data, "createdFromto");
        String orderSheetId = JsonUtils.getStringValue(data, "orderSheetId");
        String productId = JsonUtils.getStringValue(data, "productId");
        String orderStatus = JsonUtils.getStringValue(data, "orderStatus");

        String existsQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct where IF(@{productId} = '' ,'1',productId) = IF(@{productId} = '' ,'1',@{productId}) and IF(@{orderStatus} = '' ,'1',orderStatus) = IF(@{orderStatus} = '' ,'1',@{orderStatus})";

        List<String> search = new ArrayList<>();
        search.add("pageSize="+pageSize);
        search.add("page="+currentPage);
        search.add("sorting=created desc");
        search.add("referenceView=memberNo");
        if (!StringUtils.isEmpty(orderSheetId)) search.add("orderSheetId_equals="+orderSheetId);
        if (!StringUtils.isEmpty(orderStatus)) search.add("orderSheetId_exists="+existsQuery);
        if (!StringUtils.isEmpty(createdFromto)) search.add("created_fromto="+createdFromto);

        String searchText = StringUtils.join(search, "&");

//        String searchText = "pageSize=" + pageSize +
//                "&page=" + currentPage +
//                "&sorting=created desc" +
//                (orderSheetId != "" ? "&orderSheetId_equals=" + orderSheetId : "") +
//                (productId != "" || orderStatus != "" ? "&orderSheetId_exists=" + existsQuery : "") +
//                (createdFromto != "" ? "&created_fromto=" + createdFromto : "") +
//                (memberNo != "" ? "&memberNo_equals=" + memberNo : "");

        List<Map<String, Object>> sheetTotalList = nodeBindingService.list(orderSheet_TID, "");


        NodeType nodeType = NodeUtils.getNodeType(orderSheet_TID);
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType, null);
        queryContext.setData(context.getData());
        NodeBindingInfo nodeBindingInfo = nodeBindingService.getNodeBindingInfo(orderSheet_TID);
        List<Map<String, Object>> sheetList = nodeBindingInfo.list(queryContext);

        ReadContext readContext = new ReadContext();

        for (Map<String, Object> sheet : sheetList) {
            Timestamp created = sheet.get("created") == null ? null : (Timestamp) sheet.get("created");
            if (created != null) sheet.put("created", FastDateFormat.getInstance("yyyyMMddHHmmss").format(created.getTime()));
            Long memberNoValue = sheet.get("memberNo") == null ? null : (Long) sheet.get("memberNo");
            if (memberNoValue == null) {
                sheet.put("memberItem", new HashMap<>());
            } else {
                Node memberNode = nodeService.getNode("member", memberNoValue.toString());
                if (memberNode == null) {
                    sheet.put("memberItem", new HashMap<>());
                } else {
                    Map<String, Object> memberItem = new HashMap<>();
                    NodeType memberNodeType = NodeUtils.getNodeType("member");
                    List<PropertyType> memberPropertyList = new ArrayList<>(memberNodeType.getPropertyTypes());
                    for (PropertyType memberProperty : memberPropertyList) {
                        if (!StringUtils.equals(memberProperty.getPid(), "password")) {
                            memberItem.put(memberProperty.getPid(), NodeUtils.getResultValue(readContext, memberProperty, memberNode));
                        }
                    }
                    sheet.put("memberItem", memberItem);
                }
            }

            List<Map<String, Object>> opList = nodeBindingService.list(orderProduct_TID, "orderSheetId_equals=" + JsonUtils.getStringValue(sheet, "orderSheetId"));
            for (Map<String, Object> op : opList) {
                Node product = NodeUtils.getNode("product", JsonUtils.getStringValue(op, "productId"));
                List<Map<String, Object>> mainImages = nodeBindingService.list(commonResource_TID, "contentsId_matching=" + product.getId() + "&tid_matching=product&name_matching=main");
                product.put("referencedMainImage", mainImages);

                Map<String, Object> orderDeliveryPrice = getOrderDeliveryPrice(JsonUtils.getStringValue(op, "orderSheetId"), JsonUtils.getStringValue(op, "orderProductId"));
                op.put("referencedOrderDeliveryPrice", orderDeliveryPrice);
                op.put("functionBtn", getFunctionBtn(orderDeliveryPrice, op, product));
                putReferenceValue("orderProduct", context, op);
            }
            sheet.put("referencedOrderProduct", opList);
            putReferenceValue("orderSheet", context, sheet);
        }

        int pageCount = (int) Math.ceil((double) sheetTotalList.size() / (double) pageSize);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("totalCount", sheetTotalList.size());
        item.put("resultCount", sheetList.size());
        item.put("pageSize", pageSize);
        item.put("pageCount", pageCount);
        item.put("currentPage", currentPage);

        item.put("items", sheetList);
        context.setResult(item);
    }

    public void putReferenceValue(String nodeTypeId, ExecuteContext context, Map<String, Object> op) {
        NodeType nodeType = NodeUtils.getNodeType(nodeTypeId);
        for (PropertyType pt : nodeType.getPropertyTypes()) {
            if ("REFERENCE".equals(pt.getValueType().toString()) && op.get(pt.getPid()) != null) {
                op.put(pt.getPid(), NodeUtils.getReferenceValue(context, op.get(pt.getPid()), pt));
            }
        }
    }

    public Map<String, Object> getFunctionBtn(Map<String, Object> orderDeliveryPrice, Map<String, Object> orderProduct, Map<String, Object> product) {
        Map<String, Object> map = new LinkedHashMap<>();
        String orderStatus = JsonUtils.getStringValue(orderProduct, "orderStatus");
        String contentsType = JsonUtils.getStringValue(product, "contentsType");
        String deliveryMethod = JsonUtils.getStringValue(product, "deliveryMethod");
        boolean writableReviewYn = (JsonUtils.getIntValue(orderDeliveryPrice, "writableReviewYn") == 0 ? false : true);

        boolean cancelBtn = false;
        boolean exchangeBtn = false;
        boolean returnBtn = false;
        boolean reviewBtn = false;
        boolean trackingViewBtn = false;
        boolean addressChangeBtn = false;

        if ("order002".equals(orderStatus) || "order003".equals(orderStatus) || "order004".equals(orderStatus)) {
            cancelBtn = true;
        }
        if ("goods".equals(contentsType) && ("order005".equals(orderStatus) || "order006".equals(orderStatus))) {
            exchangeBtn = true;
        }
        if ("goods".equals(contentsType) && ("order005".equals(orderStatus) || "order006".equals(orderStatus))) {
            returnBtn = true;
        }
        if ("goods".equals(contentsType) && writableReviewYn && ("order005".equals(orderStatus) || "order006".equals(orderStatus) || "order007".equals(orderStatus))) {
            reviewBtn = true;
        }
        if ("goods".equals(contentsType) && "delivery".equals(deliveryMethod) && ("order005".equals(orderStatus) || "order006".equals(orderStatus) || "order015".equals(orderStatus) || "order016".equals(orderStatus))) {
            trackingViewBtn = true;
        }
        if ("goods".equals(contentsType) && ("order002".equals(orderStatus) || "order003".equals(orderStatus))) {
            addressChangeBtn = true;
        }

        map.put("cancelBtn", cancelBtn);
        map.put("exchangeBtn", exchangeBtn);
        map.put("returnBtn", returnBtn);
        map.put("reviewBtn", reviewBtn);
        map.put("trackingViewBtn", trackingViewBtn);
        map.put("addressChangeBtn", addressChangeBtn);

        return map;
    }

    public Map<String, Object> getOrderDeliveryPrice(String orderSheetId, String orderProductId) {
        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("review").getJdbcTemplate();
        String query = "select a.writableReviewYn, b.*\n" +
                "from (\n" +
                "  select\n" +
                "    IFNULL(MAX(orderDeliveryPriceId), 0) as orderDeliveryPriceId, IFNULL(MAX(if((sendingDate + INTERVAL 30 DAY) >= now(), TRUE, FALSE)), FALSE) as writableReviewYn\n" +
                "  from orderdeliveryprice\n" +
                "  WHERE orderSheetId = ? and find_in_set(?, orderProductIds) > 0\n" +
                ") a LEFT JOIN orderdeliveryprice b\n" +
                "ON a.orderDeliveryPriceId = b.orderDeliveryPriceId ";
        return jdbcTemplate.queryForMap(query, orderSheetId, orderProductId);
    }

    public ExecuteContext getOrderChangeList(ExecuteContext context) {

        return context;
    }

    private String start(String niceUid, String svcPwd, String service, String strGbn, String strResId, String strNm, String strBankCode, String strAccountNo, String svcGbn, String strOrderNo, String svc_cls, String inq_rsn) {

        String result = "";

        BufferedReader in = null;
        PrintWriter out = null;

        try {
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket soc = (SSLSocket) factory.createSocket("secure.nuguya.com", 443);

            // 타임아웃  +++++++++++++++++++++++++++++++++++++++++++++++++++++
            soc.setSoTimeout(10 * 1000);    // 타임아웃 10초
            soc.setSoLinger(true, 10);
            soc.setKeepAlive(true);
            // 타임아웃  +++++++++++++++++++++++++++++++++++++++++++++++++++++

            out = new PrintWriter(soc.getOutputStream());
            in = new BufferedReader(new InputStreamReader(soc.getInputStream()), 8 * 1024);

            result = rlnmCheck(out, in, niceUid, svcPwd, service, strGbn, strResId, strNm, strBankCode, strAccountNo, svcGbn, strOrderNo, svc_cls, inq_rsn);

        } catch (SocketException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }

        return result;

    }

    private String rlnmCheck(PrintWriter out, BufferedReader in, String niceUid, String svcPwd, String service, String strGbn, String strResId, String strNm, String strBankCode, String strAccountNo, String svcGbn, String strOrderNo, String svc_cls, String inq_rsn) throws IOException {
        StringBuffer sbResult = new StringBuffer();

        String contents = "niceUid=" + niceUid + "&svcPwd=" + svcPwd + "&service=" + service + "&strGbn=" + strGbn + "&strResId=" + strResId + "&strNm=" + strNm + "&strBankCode=" + strBankCode + "&strAccountNo=" + strAccountNo + "&svcGbn=" + svcGbn + "&strOrderNo=" + strOrderNo + "&svc_cls=" + svc_cls + "&inq_rsn=" + inq_rsn + "&seq=0000001";

        out.println("POST https://secure.nuguya.com/nuguya2/service/realname/sprealnameactconfirm.do HTTP/1.1"); //UTF-8 URL
//        out.println("POST https://secure.nuguya.com/nuguya/service/realname/sprealnameactconfirm.do HTTP/1.1");
        out.println("Host: secure.nuguya.com");
        out.println("Connection: Keep-Alive");
        out.println("Content-Type: application/x-www-form-urlencoded");
        out.println("Content-Length: " + contents.length());
        out.println();
        out.println(contents);
        out.flush();

        String line = null;
        int i = 0;
        boolean notYet = true;
        try {
            while ((line = in.readLine()) != null) {
                i++;
                if (notYet && line.indexOf("HTTP/1.") == -1) {
                    continue;
                }
                if (notYet && line.indexOf("HTTP/1.") > -1) {
                    notYet = false;
                }

                if (line.indexOf("HTTP/1.") > -1) {
                    notYet = false;
                }
                if (line.startsWith("0")) {
                    break;
                }
                if (line == null) {
                    break;
                }

                if (i == 9) sbResult.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(sbResult.toString());
        return sbResult.toString();
    }


}
