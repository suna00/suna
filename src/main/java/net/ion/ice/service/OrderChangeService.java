package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("orderChangeService")
public class OrderChangeService {
    private Logger logger = Logger.getLogger(ProductService.class);

    public static final String payment = "payment";
    public static final String orderChange = "orderChange";
    public static final String orderChangeProduct = "orderChangeProduct";
    public static final String orderChangeDeliveryPrice = "orderChangeDeliveryPrice";
    public static final String orderProduct = "orderProduct";
    public static final String delivery = "delivery";

    public static final String CANCEL = "cancel";
    public static final String EXCHANGE = "exchange";
    public static final String RETURN = "return";

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService ;
    @Autowired
    private DeliveryService deliveryService ;

/*  CANCEL
    {
        "memberNo": 88888,
        "ordersheetId": "B20170927232008520250",
        "orderChangeProduct": [
            {
                "orderProductId": 39,
                "productId": 3,
                "quantity": 1,
                "changeReasonType": "cancel001"
            },
            {
                "orderProductId": 40,
                "productId": 3,
                "quantity": 1,
                "changeReasonType": "cancel001"
            }
        ],
        "refundAccount": {  //무통장입금
            "accountNo": "000-1123-121412",
            "bankName": "하나은행",
            "accountOwner": "윤서눙"
        }
    }
*/


/*  RETURN
    {
        "memberNo": 88888,
        "ordersheetId": "B20170927232008520250",
        "orderChangeProduct": [
            {
                "orderProductId": 39,
                "productId": 3,
                "quantity": 1,
                "changeReasonType": "cancel001"
            },
            {
                "orderProductId": 40,
                "productId": 3,
                "quantity": 1,
                "changeReasonType": "cancel001"
            }
        ],
        "refundAccount": {  //무통장입금
            "accountNo": "000-1123-121412",
            "bankName": "하나은행",
            "accountOwner": "윤서눙"
        },
        "recallType": "directly",   //request 방문회수요청(회수배송비+반품배송비), directly 고객직접발송(반품배송비), already 고객발송완료(반품배송비)
        "trackingNo": "1234",
        "deliveryEnterpriseId": "6",
        "delivery": [
            {
                "deliveryType": "recallAddress",    //회수지 recallAddress,  반품지 returnAddress
                "recipient": "윤서눙",
                "cellphone": "011-011-011",
                "phone": "02-299-1234",
                "postCode": "135080",
                "address": "서울시 강남구 역삼동 823-39 아이온 빌딩 4"
            }
        ]
    }
*/

/*  EXCHANGE
    {
        "memberNo": 88888,
        "ordersheetId": "B20170927232008520250",
        "orderChangeProduct": [
            {
                "orderProductId": 39,
                "productId": 3,
                "quantity": 1,
                "changeReasonType": "cancel001"
            },
            {
                "orderProductId": 40,
                "productId": 3,
                "quantity": 1,
                "changeReasonType": "cancel001"
            }
        ],
        "recallType": "directly",   //request 방문회수요청(회수배송비+반품배송비), directly 고객직접발송(반품배송비), already 고객발송완료(반품배송비)
        "trackingNo": "1234",
        "deliveryEnterpriseId": "6",
        "delivery": [
            {
                "deliveryType": "recallAddress",    //회수지 recallAddress
                "recipient": "윤서눙",
                "cellphone": "011-011-011",
                "phone": "02-299-1234",
                "postCode": "135080",
                "address": "서울시 강남구 역삼동 823-39 아이온 빌딩 4"
            },
            {
                "deliveryType": "deliveryAddress",    //배송지 deliveryAddress
                "recipient": "윤서눙",
                "cellphone": "011-011-011",
                "phone": "02-299-1234",
                "postCode": "135080",
                "address": "서울시 강남구 역삼동 823-39 아이온 빌딩 4"
            }
        ]
    }
*/
// -------- 무통장입금 처리 안됨

    //취소,교환,반품 요청 가능여부
    public boolean isRequestPossible(Map<String, Object> data, String changeType) throws IOException {
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get("orderChangeProduct").toString());
        for(Map<String, Object> map : maps){
            Node orderProduct = NodeUtils.getNode("orderProduct", map.get("orderProductId").toString());

            if("exchange".equals(changeType) || "return".equals(changeType)){
                Node product = NodeUtils.getNode("product", map.get("productId").toString());
                // LG삼성 직배송, SMS = 교환 반품 불가
                if("delivery".equals(product.get("sms")) || "delivery".equals(product.get("lg")) || "delivery".equals(product.get("samsung"))) return false;

                //order005	배송중 / order006	배송완료 / order007	구매확정
                if(!("order005".equals(orderProduct.get("orderStatus"))
                        || "order006".equals(orderProduct.get("orderStatus"))
                        || "order007".equals(orderProduct.get("orderStatus")))) return false;
            }else{
                //cancel
                //order001	주문완료 / order002	입금대기 / order003	결제완료 / order004	상품준비중
                if(!("order001".equals(orderProduct.get("orderStatus"))
                        || "order002".equals(orderProduct.get("orderStatus"))
                        || "order003".equals(orderProduct.get("orderStatus"))
                        || "order004".equals(orderProduct.get("orderStatus")))) return false;
            }
        }

        return true;
    }

    //전체/부분
    public String getChangeRange(Map<String, Object> data) throws IOException {
        List<Map<String, Object>> orderProducts = nodeBindingService.list(orderProduct, "orderSheetId_equals=" + data.get("orderSheetId"));
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get("orderChangeProduct").toString());
        if(orderProducts.size() == maps.size()){
            for(Map<String, Object> orderProduct : orderProducts){
                for(Map<String, Object> map : maps){
                    if(orderProduct.get("orderProductId").equals(map.get("orderProductId"))
                            && !orderProduct.get("quantity").equals(map.get("quantity"))){
                        return "part";
                    }
                }
            }
            return "all";
        }
        if(orderProducts.size() > maps.size()) return "part";


        return "all";
    }

    // 부분취소/부분반품 가능여부
    public boolean isPartChangePossible(String orderSheetId){
        List<Map<String, Object>> list = nodeBindingService.list(payment, "ordrIdxx_equals=" + orderSheetId);
        if(list.size() > 0){
            for(Map<String, Object> map : list){
                String usePayMethod = map.get("usePayMethod").toString();
                //계좌이체 : 010000000000
                //무통장입금 가상계좌 : 001000000000
                if("010000000000".equals(usePayMethod) || "001000000000".equals(usePayMethod))return false;
            }

        }

        return true;
    }

//    cancel001,단순변심 / cancel002,재주문 / cancel003,품절
//    returnExchange001,단순변심 / returnExchange002,제품하자 / returnExchange003,부품하자 / returnExchange004,오배송 / returnExchange005,재주문

    //귀책대상(판매자/구매자)
    public String getBlameTarget(String changeReasonType){
        if("cancel001".equals(changeReasonType)
                || "cancel002".equals(changeReasonType)
                || "returnExchange001".equals(changeReasonType)
                || "returnExchange005".equals(changeReasonType)) return "customer";
        return "vendor";
    }

    public ExecuteContext requestCancel(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();

        String[] params = { "memberNo", "ordersheetId","orderChangeProduct" };
        if (CommonService.requiredParams(context, data, params)) return context;

        if(!isRequestPossible(data, CANCEL)){
            context.setResult(CommonService.getResult("M0004"));
            return context;
        }

        if("part".equals(getChangeRange(data))){
            if(!isPartChangePossible(data.get("ordersheetId").toString())){
                context.setResult(CommonService.getResult("M0003"));
                return context;
            }

            if("추가배송비발생".equals("")){
                //읭?
            }
        }

        data.put("changeType", CANCEL);
        data.put("orderStatus", "order008");    //취소신청
        createOrderChange(data);

        return context;
    }

    public ExecuteContext requestExchange(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        String[] params = { "memberNo", "ordersheetId","orderChangeProduct" };
        if (CommonService.requiredParams(context, data, params)) return context;
        if(!isRequestPossible(data, EXCHANGE)){
            context.setResult(CommonService.getResult("M0005"));
            return context;
        }

        data.put("changeType", EXCHANGE);
        data.put("orderStatus", "order010");    //교환요청
        String orderChangeId = createOrderChange(data);
        createDeliveryAddress(data, orderChangeId);

        return context;
    }

    public ExecuteContext requestReturn(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        String[] params = { "memberNo", "ordersheetId","orderChangeProduct" };
        if (CommonService.requiredParams(context, data, params)) return context;
        if(!isRequestPossible(data, RETURN)){
            context.setResult(CommonService.getResult("M0006"));
            return context;
        }

        if("part".equals(getChangeRange(data))){
            if(!isPartChangePossible(data.get("ordersheetId").toString())){
                context.setResult(CommonService.getResult("M0003"));
                return context;
            }

        }
        data.put("changeType", RETURN);
        data.put("orderStatus", "order017");    //반품요청
        String orderChangeId = createOrderChange(data);
        createDeliveryAddress(data, orderChangeId);

        return context;
    }


    public void updateOrderProduct(Map<String, Object> map, String changeType, String orderStatus){
        Node node = NodeUtils.getNode(orderProduct, map.get("orderProductId").toString());
        Integer quantity = Integer.parseInt(node.get("quantity").toString());
        Integer changeQuantity = Integer.parseInt(map.get("quantity").toString());

        if(quantity.equals(changeQuantity)){
            node.put("quantity", 0);
            node.put("orderPrice", 0);
            node.put("orderStatus", orderStatus);    //(마이페이지 주문배송조회 40 : 통합관리자에 노출되고 취소 최종 승인 받게 되어있음.. 읭? 바로 취소완료아닝가..)
            nodeService.executeNode(node, orderProduct, CommonService.UPDATE);

        }else if(quantity > changeQuantity){
            // 주문상품의 수량 부분 취소/교환/반품
            Integer productPrice = Integer.parseInt(node.get("productPrice").toString());
            Integer totalAddOptionPrice = Integer.parseInt(node.get("totalAddOptionPrice").toString());

            node.put("quantity", quantity - changeQuantity);
            node.put("orderPrice", (productPrice * (quantity - changeQuantity)) + totalAddOptionPrice);
            nodeService.executeNode(node, orderProduct, CommonService.UPDATE);

            // 추가배송비 발생 여부 체크
            if(true){

            }

        }else{
            //error
        }

    }

    // orderchangedeliveryprice 교환배송비 발생때만 생길듯.. 송장이 붙어있자나. 부분취소로 추가배송비 붙는거는 orderdeliveryprice 에 넣어야하나......하아...

    private String createOrderChange(Map<String, Object> data) throws IOException {
        Map<String, Object> item = new LinkedHashMap<>(data);
        item.putAll(calculateRefundPrice(data));
        Node node = (Node) nodeService.executeNode(item, orderChange, CommonService.CREATE);
        item.put("orderChangeId", node.getId());

        for(Map<String, Object> map : JsonUtils.parsingJsonToList(item.get("orderChangeProduct").toString())){
            createOrderChangeProduct(item, map);
            updateOrderProduct(map, data.get("changeType").toString(), data.get("orderStatus").toString());

        }
        return node.getId();
    }

    private void createOrderChangeProduct(Map<String, Object> item, Map<String, Object> map) {
        Map<String, Object> m = new LinkedHashMap<>(item);
        m.putAll(map);
        Node product = NodeUtils.getNode("product", map.get("productId").toString());
        m.put("vendorId", product.get("vendorId"));
        nodeService.executeNode(CommonService.resetMap(m), orderChangeProduct, CommonService.CREATE);
    }


    // 반품 : 반품지
    // 교환 : 회수지, 배송지
    private void createDeliveryAddress(Map<String, Object> data, String orderChangeId) throws IOException {
        for(Map<String, Object> m : JsonUtils.parsingJsonToList(data.get("delivery").toString())){
            m.put("orderSheetId", data.get("orderSheetId"));
            m.put("orderChangeId", orderChangeId);
            nodeService.executeNode(m, delivery, CommonService.CREATE);
        }
    }

    //취소신청 : 취소상품 선택 수량 변경 Event
    public ExecuteContext refundablePrice(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        String[] params = { "memberNo", "ordersheetId","orderChangeProduct" };
        if (CommonService.requiredParams(context, data, params)) return context;
        if(!isPartChangePossible(data.get("ordersheetId").toString())){
            context.setResult(CommonService.getResult("M0003"));
            return context;
        }

        Map<String, Object> item = calculateRefundPrice(data);
        context.setResult(item);
        return context;
    }

    private Map<String, Object> calculateRefundPrice(Map<String, Object> data) throws IOException {
        Node orderSheet = NodeUtils.getNode("ordersheet", data.get("ordersheetId").toString());
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get("orderChangeProduct").toString());

        double totalProductPrice = 0;
        double totalDeliveryPrice = 0;


//        for(Map<String, Object> map : maps){
//            Node orderProduct = NodeUtils.getNode("orderProduct", map.get("orderProductId").toString());
//            int quantity = Integer.parseInt(orderProduct.get("quantity").toString());
//            int cancelQuantity = Integer.parseInt(map.get("quantity").toString());
//
//            totalProductPrice = totalProductPrice + (Double.parseDouble(orderProduct.get("orderPrice").toString()) / quantity * cancelQuantity);
//
//            Map<String, Object> deliveryPriceMap = deliveryService.getOrderDeliveryPriceMap(map.get("orderProductId").toString());
//            double deliveryPrice = Double.parseDouble(deliveryPriceMap.get("deliveryPrice").toString());
//            String[] ids = StringUtils.split(deliveryPriceMap.get("orderProductIds").toString(), ",");
//
//
//            if(quantity - cancelQuantity == 0){ //주문수 = 취소수
//                if(ids.length - 1 == 0){
//                    // 배송비 환불
//                }else if(ids.length - 1 > 0){
//                    // 배송비 재계산
//                    // 0 -> 2500 : 배송비부과, 차감금액
//                }
//
//            }else if(quantity - cancelQuantity > 0){ //주문수 > 취소수
//
//            }else{
//                //주문수 < 취소수 error
//            }
//
//
//            totalDeliveryPrice = totalDeliveryPrice + deliveryPrice;
//
//
//        }

        Map<String, Object> item = new LinkedHashMap<>();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("AAAAA", "현재 하드코딩 데이터임. 계산로직 아직.");
        result.put("cancelOrderPrice", 17500);
        result.put("totalProductPrice", 15000);
        result.put("totalDeliveryPrice", 2500);
        result.put("deductPrice", 0);
        result.put("addDeliveryPrice", 0);
        result.put("refundPrice", 17500);
        result.put("refundPaymentPrice", 13000);
        result.put("refundYPoint", 4000);
        result.put("refundWelfarepoint", 500);

        List<Map<String, Object>> list = nodeBindingService.list(payment, "paymentId_equals=63");
        if(list.size() > 0){
            result.put("payment", list.get(0));
        }
        item.put("item", result);
        return item;
    }
}
