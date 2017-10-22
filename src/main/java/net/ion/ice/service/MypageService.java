package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.session.SessionService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.*;


@Service("mypageService")
public class MypageService {
    private Logger logger = Logger.getLogger(ProductService.class);

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService;
    @Autowired
    private SessionService sessionService;


    //주문 상세조회 배송지 변경
    public ExecuteContext updateOrderDeliveryAddress(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String[] params = {"orderSheetId", "deliveryId", "postCode", "cellphone", "phone", "address", "recipient"};

        if (CommonService.requiredParams(context, data, params)) return context;

        List<Map<String, Object>> orderProducts = nodeBindingService.list("orderProduct", "orderSheetId_equals=" + data.get("orderSheetId"));
        for (Map<String, Object> orderProduct : orderProducts) {
            String orderStatus = orderProduct.get("orderStatus").toString();
            if (!("order001".equals(orderStatus) || "order002".equals(orderStatus))) {
                context.setResult(CommonService.getResult("M0002"));
                return context;
            }
        }
        nodeService.executeNode(data, "delivery", CommonService.UPDATE);
        context.setResult(CommonService.getResult("S0002"));
        return context;
    }


    //    배송지 등록수정 / 기본배송지 설정
    public ExecuteContext setMyDeliveryAddress(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = {"memberNo", "defaultYn"};
        if (CommonService.requiredParams(context, data, params)) return context;

        Node node = (Node) nodeService.executeNode(data, "myDeliveryAddress", CommonService.SAVE);
        if ("trueFalse>y".equals(node.getValue("defaultYn"))) {
            List<Node> nodes = NodeUtils.getNodeList("myDeliveryAddress", "memberNo_matching=" + data.get("memberNo") + "&myDeliveryAddressId_notMatching=" + node.getId() + "&defaultYn_matching=y");
            if (nodes.size() > 0) {
                for (Node address : nodes) {
                    address.put("defaultYn", "trueFalse>n");
                    nodeService.executeNode(address, "myDeliveryAddress", CommonService.UPDATE);
                }
            }
        }
        context.setResult(node);
        return context;
    }

    public void removeMyDeliveryAddress(ExecuteContext context) {
        Node node = context.getNode();
        if (!node.getValue("defaultYn").equals("n")) { // 방어체계구축
            nodeService.deleteNode(context.getNode().getTypeId(), context.getNode().getId());
            context.setResult(CommonService.getResult("M0001"));
        }
    }

    //    관심상품 리스트 -> 품절상품 삭제
    public ExecuteContext removeOutOfStockProduct(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        String[] params = {"memberNo"};
        if (CommonService.requiredParams(context, data, params)) return context;

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo(context.getNodeType().getTypeId());
        String query = " select a.*\n" +
                "from interestproduct a, product b\n" +
                "where b.productId = a.productId\n" +
                "  and a.memberNo = ? \n" +
                "  and b.stockQuantity = 0";
        List<Map<String, Object>> maps = nodeBindingInfo.getJdbcTemplate().queryForList(query, data.get("memberNo").toString());
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            nodeService.deleteNode("interestProduct", map.get("interestProductId").toString());
            list.add(map);
        }

        context.setResult(list);

        return context;

    }


    //  POST 처리 필요 :  {{protocol}}://{{hostname}}:{{port}}/node/member/event/pwAuth.json?memberNo=77777&password=1234
//    마이페이지 회원정보 수정 비밀번호 인증
    public ExecuteContext passwordAuthentication(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());
        String[] params = {"memberNo", "password"};
        if (CommonService.requiredParams(context, data, params)) return context;

        Node node = nodeService.read("member", data.get("memberNo").toString());
        String nodePw = node.getValue("password").toString();
        if (!nodePw.equals(data.get("password").toString())) {
            Integer failedCount = (node.getValue("failedCount") == null ? 0 : Integer.parseInt(node.getValue("failedCount").toString())) + 1;
            node.put("failedCount", failedCount);
            node.put("lastFailedDate", new Date());
            nodeService.updateNode(node, "member");
            //context.setResult("비밀번호 인증 실패 " + failedCount + "회");

            if (failedCount < 5) {
                CommonService.setErrorMessage(context, "M0007");
            } else {
                CommonService.setErrorMessage(context, "M0008");
            }
        } else {
            context.setResult("비밀번호 인증 성공");
        }

        return context;
    }

    public ExecuteContext addWishList(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        Map<String, Object> resultMap = new HashMap<>();
        try {
            Map<String, Object> session = sessionService.getSession(context.getHttpRequest());
            String productId = JsonUtils.getStringValue(data, "productId");
            String memberNo = JsonUtils.getStringValue(session, "member.memberNo");
            String siteId = JsonUtils.getStringValue(session, "member.siteId");
            String searchText = "productId_matching=".concat(productId).concat("&memberNo_matching=").concat(memberNo).concat("&siteId_matching=").concat(siteId);
            List<Node> interestProductNodeList = nodeService.getNodeList("interestProduct", searchText);
            if(interestProductNodeList.size() > 0){
                nodeService.deleteNode("interestProduct", interestProductNodeList.get(0).getId());
                resultMap.put("message", "위시리스트가 해제되었습니다.");
            }else{
                Map<String, Object> storeData = new HashMap<>();
                storeData.put("productId", productId);
                storeData.put("memberNo", memberNo);
                storeData.put("siteId", siteId);
                nodeService.executeNode(storeData, "interestProduct", CommonService.CREATE);
                resultMap.put("message", "위시리스트에 등록되었습니다. 해당 상품의 세일정보가 알림메시지로 제공됩니다.");
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        context.setResult(resultMap);
        return context;
    }

    public ExecuteContext oneToOneProudctList(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String memberNo = String.valueOf(data.get("memberNo"));

        return context;
    }
}
