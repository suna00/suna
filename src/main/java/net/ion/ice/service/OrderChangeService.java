package net.ion.ice.service;

import com.hazelcast.util.JsonUtil;
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
import java.util.*;

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
        "orderSheetId": "B20170927232008520250",
        "orderChangeProduct": [
            {
                "orderProductId": 39,
                "productId": 512,
                "quantity": 1,
                "changeReasonType": "cancel001"
            },
            {
                "orderProductId": 40,
                "productId": 521,
                "quantity": 1,
                "changeReasonType": "cancel001"
            }
        ],
        "refundAccount": {  //무통장입금
            "accountNo": "000-1123-121412",
            "bankName": "하나은행",
            "accountOwner": "윤서눙",
            "myRefundAccountYn": true
        }
    }
*/


/*  RETURN
    {
        "memberNo": 88888,
        "orderSheetId": "B20170927232008520250",
        "orderChangeProduct": [
            {
                "orderProductId": 39,
                "productId": 512,
                "quantity": 1,
                "changeReasonType": "cancel001"
            },
            {
                "orderProductId": 40,
                "productId": 521,
                "quantity": 1,
                "changeReasonType": "cancel001"
            }
        ],
        "refundAccount": {
            "accountNo": "000-1123-121412",
            "bankName": "하나은행",
            "accountOwner": "윤서눙",
            "myRefundAccountYn": true
        },
        "recallType": "request",   //request 방문회수요청(회수배송비+반품배송비), directly 고객직접발송(반품배송비), already 고객발송완료(반품배송비)
        "trackingNo": "1234",
        "deliveryEnterpriseId": "6",
        "delivery": [
            {
                "deliveryType": "recallAddress",
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
        "orderSheetId": "B20170927232008520250",
        "orderChangeProduct": [
            {
                "orderProductId": 39,
                "productId": 512,
                "quantity": 1,
                "changeReasonType": "cancel001"
            },
            {
                "orderProductId": 40,
                "productId": 521,
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
                "deliveryType": "exchangeAddress",    //교환배송지
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
                /*
                 * LG삼성 직배송, SMS = 교환 반품 불가
                    문자발송 : sms
                    LG배송 : lg
                    삼성배송 : samsung
                    업체택배 : delivery
                */

                String deliveryMethod = JsonUtils.getStringValue(product, "deliveryMethod");
                if("deliveryMethod>sms".equals(deliveryMethod) || "deliveryMethod>lg".equals(deliveryMethod) || "deliveryMethod>samsung".equals(deliveryMethod)) return false;

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
    public boolean isPartChangePossible(Map<String, Object> data){
        List<Map<String, Object>> list = nodeBindingService.list(payment, "ordrIdxx_equals=" + JsonUtils.getStringValue(data, "orderSheetId"));
        if(list.size() > 0){
            for(Map<String, Object> map : list){
                String usePayMethod = map.get("usePayMethod").toString();
                data.put("usePayMethod", usePayMethod);
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


    // 환불계좌
    public Map<String, Object> myRefundAccount(Map<String, Object> data) {
        Map<String, Object> refundAccount = (Map<String, Object>) data.get("refundAccount");
        if(JsonUtils.getBooleanValue(refundAccount, "myRefundAccountYn") && data.get("memberNo") != null){
            Node my = null;
            List<Node> list = nodeService.getNodeList("myRefundAccount", "memberNo_matching=" + JsonUtils.getStringValue(data, "memberNo"));
            if(list.size() > 0){
                my = list.get(0);
            }
            my.putAll(refundAccount);
            nodeService.executeNode(my, orderProduct, CommonService.SAVE);
        }

        return refundAccount;
    }

    /*
        취소신청
    */
    public ExecuteContext requestCancel(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();

        String[] params = { "orderSheetId","orderChangeProduct" };
        if (CommonService.requiredParams(context, data, params)) return context;

        if(!isRequestPossible(data, CANCEL)){
            context.setResult(CommonService.getResult("M0004"));
            return context;
        }

        if("part".equals(getChangeRange(data))){
            if(!isPartChangePossible(data)){
                context.setResult(CommonService.getResult("M0003"));
                return context;
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        if("010000000000".equals(JsonUtils.getStringValue(data, "usePayMethod")) || "001000000000".equals(JsonUtils.getStringValue(data, "usePayMethod"))) {
            if (data.get("refundAccount") == null) {
                context.setResult(CommonService.getResult("M0009"));
                return context;
            }
            map.putAll(myRefundAccount(data));
        }

        Map<String, Object> result = calculateRefundablePrice(data);
        map.putAll((Map<String, Object>) result.get("willBeRefundedItem"));
        map.put("changeType", CANCEL);
        map.put("orderStatus", "order008");    //취소신청

        Map<String, Object> orderChangeNode = createOrderChange(map);

        Map<String, Object> restMap = (Map<String, Object>) result.get("restItem");
        Map<String, Object> restDeliveryPriceList = (Map<String, Object>) restMap.get("restDeliveryPriceList");
//        deliveryService.makeDeliveryPrice(JsonUtils.getStringValue(data, "orderSheetId"), restDeliveryPriceList);

//        Map<String, Object> item = new LinkedHashMap<>();
//        item.put("item", orderChangeNode);
//        context.setResult(item);

        context.setResult(CommonService.getResult("M0004"));
        return context;
    }

    /*
        취소완료
    */
    public ExecuteContext completeCancel(ExecuteContext context) throws IOException {
        //  Update orderSheet
        //  updateOrderProduct(map, JsonUtils.getStringValue(willBeRefundedItem, "changeType"), JsonUtils.getStringValue(willBeRefundedItem, "orderStatus"));
        //  deliveryService.makeDeliveryPrice(JsonUtils.getStringValue(data, "orderSheetId"), restDeliveryPriceList);

        return context;
    }
    /*
        교환신청
    */
    public ExecuteContext requestExchange(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        String[] params = { "orderSheetId","orderChangeProduct" };
        if (CommonService.requiredParams(context, data, params)) return context;
        if(!isRequestPossible(data, EXCHANGE)){
            context.setResult(CommonService.getResult("M0005"));
            return context;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> result = calculateRefundablePrice(data);
        map.putAll((Map<String, Object>) result.get("willBeRefundedItem"));
        map.put("changeType", EXCHANGE);
        map.put("orderStatus", "order010");    //교환요청
        map.put("recallType", JsonUtils.getStringValue(data, "recallType"));
        map.put("trackingNo", JsonUtils.getStringValue(data, "trackingNo"));
        map.put("deliveryEnterpriseId", JsonUtils.getStringValue(data, "deliveryEnterpriseId"));
        map.put("exchangeReturnAgreeYn", JsonUtils.getStringValue(data, "exchangeReturnAgreeYn"));

        Map<String, Object> orderChangeNode = createOrderChange(map);
        createDeliveryAddress(data, JsonUtils.getStringValue(orderChangeNode, "orderChangeId"));

//        Map<String, Object> item = new LinkedHashMap<>();
//        item.put("item", orderChangeNode);
//        context.setResult(item);
        context.setResult(CommonService.getResult("M0004"));
        return context;
    }

    /*
        반품신청
    */
    public ExecuteContext requestReturn(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        String[] params = { "orderSheetId","orderChangeProduct" };
        if (CommonService.requiredParams(context, data, params)) return context;
        if(!isRequestPossible(data, RETURN)){
            context.setResult(CommonService.getResult("M0006"));
            return context;
        }

        if("part".equals(getChangeRange(data))){
            if(!isPartChangePossible(data)){
                context.setResult(CommonService.getResult("M0003"));
                return context;
            }

        }

        Map<String, Object> map = new LinkedHashMap<>();
        if("010000000000".equals(JsonUtils.getStringValue(data, "usePayMethod")) || "001000000000".equals(JsonUtils.getStringValue(data, "usePayMethod"))) {
            if (data.get("refundAccount") == null) {
                context.setResult(CommonService.getResult("M0009"));
                return context;
            }
            map.putAll(myRefundAccount(data));
        }

        Map<String, Object> result = calculateRefundablePrice(data);
        map.putAll((Map<String, Object>) result.get("willBeRefundedItem"));
        map.put("changeType", RETURN);
        map.put("orderStatus", "order017");    //반품요청
        map.put("recallType", JsonUtils.getStringValue(data, "recallType"));
        map.put("trackingNo", JsonUtils.getStringValue(data, "trackingNo"));
        map.put("deliveryEnterpriseId", JsonUtils.getStringValue(data, "deliveryEnterpriseId"));
        map.put("exchangeReturnAgreeYn", JsonUtils.getStringValue(data, "exchangeReturnAgreeYn"));

        Map<String, Object> orderChangeNode = createOrderChange(map);
        createDeliveryAddress(data, JsonUtils.getStringValue(orderChangeNode, "orderChangeId"));

//        Map<String, Object> item = new LinkedHashMap<>();
//        item.put("item", orderChangeNode);
//        context.setResult(item);
        context.setResult(CommonService.getResult("M0004"));
        return context;
    }


    public void updateOrderProduct(Map<String, Object> map, String changeType, String orderStatus){
        Node node = NodeUtils.getNode(orderProduct, map.get("orderProductId").toString());
        Integer quantity = JsonUtils.getIntValue(node, "quantity");
        Integer changeQuantity = JsonUtils.getIntValue(map, "quantity");

        if(quantity.equals(changeQuantity)){
            node.put("quantity", 0);
            node.put("orderPrice", 0);
            node.put("couponDiscountPrice", 0);
            node.put("orderStatus", orderStatus);    //(마이페이지 주문배송조회 40 : 통합관리자에 노출되고 취소 최종 승인 받게 되어있음.. 읭? 바로 취소완료아닝가..)
            nodeService.executeNode(node, orderProduct, CommonService.UPDATE);

        }else if(quantity > changeQuantity){
            // 주문상품의 수량 부분 취소/교환/반품
            double productPrice = JsonUtils.getDoubleValue(node, "productPrice");
            double totalAddOptionPrice = JsonUtils.getDoubleValue(node, "totalAddOptionPrice");
            double couponDiscountPrice = JsonUtils.getDoubleValue(node, "couponDiscountPrice");

            node.put("quantity", quantity - changeQuantity);
            node.put("orderPrice", (productPrice * (quantity - changeQuantity)) + totalAddOptionPrice);
//            node.put("paymentPrice", (productPrice * (quantity - changeQuantity)) + totalAddOptionPrice - couponDiscountPrice);
            nodeService.executeNode(node, orderProduct, CommonService.UPDATE);

        }else{
            //error
        }

    }

    // orderchangedeliveryprice 교환배송비 발생때만 생길듯.. 송장이 붙어있자나

    private Map<String, Object> createOrderChange(Map<String, Object> willBeRefundedItem) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderSheetId", JsonUtils.getStringValue(willBeRefundedItem, "orderSheetId"));
        m.put("memberNo", JsonUtils.getStringValue(willBeRefundedItem, "memberNo"));
        m.put("changeType", JsonUtils.getStringValue(willBeRefundedItem, "changeType"));
        m.put("cancelOrderPrice", JsonUtils.getDoubleValue(willBeRefundedItem, "cancelOrderPrice"));
        m.put("cancelProductPrice", JsonUtils.getDoubleValue(willBeRefundedItem, "cancelProductPrice"));
        m.put("cancelDeliveryPrice", JsonUtils.getDoubleValue(willBeRefundedItem, "cancelDeliveryPrice"));
        m.put("deductPrice", JsonUtils.getDoubleValue(willBeRefundedItem, "deductPrice"));
        m.put("addDeliveryPrice", JsonUtils.getDoubleValue(willBeRefundedItem, "addDeliveryPrice"));
        m.put("refundPrice", JsonUtils.getDoubleValue(willBeRefundedItem, "refundPrice"));
        m.put("refundWelfarePoint", JsonUtils.getDoubleValue(willBeRefundedItem, "refundWelfarePoint"));
        m.put("refundYPoint", JsonUtils.getDoubleValue(willBeRefundedItem, "refundYPoint"));
        m.put("refundPaymentPrice", JsonUtils.getDoubleValue(willBeRefundedItem, "refundPaymentPrice"));
        m.put("recallType", JsonUtils.getStringValue(willBeRefundedItem, "recallType"));
        m.put("trackingNo", JsonUtils.getStringValue(willBeRefundedItem, "trackingNo"));
        m.put("deliveryEnterpriseId", JsonUtils.getStringValue(willBeRefundedItem, "deliveryEnterpriseId"));
        m.put("exchangeReturnAgreeYn", JsonUtils.getStringValue(willBeRefundedItem, "exchangeReturnAgreeYn"));

        Node node = (Node) nodeService.executeNode(m, orderChange, CommonService.CREATE);
        m.put("orderChangeId", node.getId());

        Map<String, Object> orderChangeProductList = (Map<String, Object>) willBeRefundedItem.get("orderChangeDeliveryList");
        List<Map<String, Object>> tempList = new ArrayList<>();
        for(String key : orderChangeProductList.keySet()){
            for(Map<String, Object> map : (List<Map<String, Object>>) orderChangeProductList.get(key)){
                tempList.add(createOrderChangeProduct(m, map));
//                updateOrderProduct(map, JsonUtils.getStringValue(willBeRefundedItem, "changeType"), JsonUtils.getStringValue(willBeRefundedItem, "orderStatus"));
            }
        }
        //update orderSheet

        m.put("orderChangeProduct", tempList);
        return m;
    }

    private Map<String, Object> createOrderChangeProduct(Map<String, Object> orderChange, Map<String, Object> map) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderChangeId", JsonUtils.getStringValue(orderChange, "orderChangeId"));
        m.put("orderSheetId", JsonUtils.getStringValue(orderChange, "orderSheetId"));
        m.put("orderProductId", JsonUtils.getIntValue(map, "orderProductId"));
        m.put("productId", JsonUtils.getIntValue(map, "productId"));
        m.put("baseOptionItemId", JsonUtils.getIntValue(map, "baseOptionItemId"));
        m.put("baseOptionItemName", JsonUtils.getStringValue(map, "baseOptionItemName"));
        m.put("quantity", JsonUtils.getIntValue(map, "quantity"));
        m.put("salePrice", JsonUtils.getDoubleValue(map, "salePrice"));
        m.put("baseAddPrice", JsonUtils.getDoubleValue(map, "baseAddPrice"));
        m.put("productPrice", JsonUtils.getDoubleValue(map, "productPrice"));
        m.put("totalAddOptionPrice", JsonUtils.getDoubleValue(map, "totalAddOptionPrice"));
        m.put("orderPrice", JsonUtils.getDoubleValue(map, "orderPrice"));
        m.put("paymentPrice", JsonUtils.getDoubleValue(map, "paymentPrice"));
        m.put("couponId", JsonUtils.getIntValue(map, "couponId"));
        m.put("couponDiscountPrice", JsonUtils.getDoubleValue(map, "couponDiscountPrice"));
        m.put("changeType", JsonUtils.getStringValue(map, "changeType"));
        m.put("changeReasonType", JsonUtils.getStringValue(map, "changeReasonType"));
        m.put("reason", JsonUtils.getStringValue(map, "reason"));
        m.put("exchangeOption", JsonUtils.getStringValue(map, "exchangeOption"));
        m.put("vendorId", JsonUtils.getStringValue(map, "product.vendorId"));
        return (Map<String, Object>) nodeService.executeNode(m, orderChangeProduct, CommonService.CREATE);
    }


    // 반품 : 회수지, (반품지 : 벤더가 등록한 반품주소 노출)
    // 교환 : 회수지, 배송지
    private void createDeliveryAddress(Map<String, Object> data, String orderChangeId) throws IOException {
        for(Map<String, Object> m : JsonUtils.parsingJsonToList(data.get("delivery").toString())){
            m.put("orderSheetId", data.get("orderSheetId"));
            m.put("orderChangeId", orderChangeId);
            nodeService.executeNode(m, delivery, CommonService.CREATE);
        }
    }

    //취소신청 : 취소상품 선택 수량 변경 Event
    public ExecuteContext getRefundablePrice(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        String[] params = { "memberNo", "orderSheetId","orderChangeProduct" };
        if (CommonService.requiredParams(context, data, params)) return context;
        if(!isPartChangePossible(data)){
            context.setResult(CommonService.getResult("M0003"));
            return context;
        }
        Map<String, Object> result = calculateRefundablePrice(data);

        context.setResult(result);
        return context;
    }

    public Map<String, Object> calculateRefundablePrice(Map<String, Object> data) throws IOException {
        //주문서정보
        Map<String, Object> orderSheetNode = NodeUtils.getNode("orderSheet", data.get("orderSheetId").toString());
        List<Map<String, Object>> orderProducts = nodeBindingService.list("orderProduct", "sorting=created&orderSheetId_equals=" + data.get("orderSheetId"));
        List<Map<String, Object>> orderProductItems = nodeBindingService.list("orderProductItem", "sorting=created&orderSheetId_equals=" + data.get("orderSheetId"));

        //주문변경 신청 상품리스트
        List<Map<String, Object>> orderChangeProducts = JsonUtils.parsingJsonToList(data.get("orderChangeProduct").toString());

        List<Map<String, Object>> orderChangeProductList = changeProductList(orderProducts, orderProductItems, orderChangeProducts); // 변경할 상품
        List<Map<String, Object>> orderRestProductList = restProductList(orderProducts, orderProductItems, orderChangeProducts);    //변경하고 남을 상품

        List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(orderChangeProductList, "order") ;
        Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "order") ;

        List<Map<String, Object>> deliveryProductListForRest = deliveryService.makeDeliveryData(orderRestProductList, "order") ;
        Map<String, Object> deliveryPriceListForRest = deliveryService.calculateDeliveryPrice(deliveryProductListForRest, "order") ;


        double cancelOrderPrice = 0;
        double cancelProductPrice = 0;
        double cancelDeliveryPrice = 0;
        double cancelDiscountPrice = 0;
        double totalRestDeliveryPrice = 0;
        double deductPrice = 0;
        double addDeliveryPrice = 0;
        double refundPrice = 0;
        double refundPaymentPrice = 0;
        double refundYPoint = 0;
        double refundWelfarepoint = 0;

        Map<String, Double> orderChangeProducsDeliveryPrice = new HashMap<>();  //변경신청할 상품의 기존 배송비 distinct
        for(Map<String, Object> map : orderChangeProducts){
            Map<String, Object> deliveryPriceMap = deliveryService.getOrderDeliveryPriceMap(JsonUtils.getStringValue(map, "orderProductId"));
            orderChangeProducsDeliveryPrice.put(JsonUtils.getStringValue(deliveryPriceMap, "orderDeliveryPriceId"), JsonUtils.getDoubleValue(deliveryPriceMap, "deliveryPrice"));
        }
        for(String key : orderChangeProducsDeliveryPrice.keySet()){
            cancelDeliveryPrice += orderChangeProducsDeliveryPrice.get(key);
        }

        for(String key : deliveryPriceList.keySet()){
            for(Map<String, Object> map : (List<Map<String, Object>>) deliveryPriceList.get(key)){
                cancelProductPrice += JsonUtils.getDoubleValue(map, "orderPrice");
            }
        }

        for(String key : deliveryPriceListForRest.keySet()){
            for(Map<String, Object> map : (List<Map<String, Object>>) deliveryPriceListForRest.get(key)){
                totalRestDeliveryPrice += JsonUtils.getDoubleValue(map, "deliveryPrice");
            }
        }

        //상품금액 합계+배송비 합계-할인금액합계(total이 아니고 취소대상의 할인금액) 수정필요
        cancelOrderPrice = cancelProductPrice + cancelDeliveryPrice - cancelDiscountPrice; //JsonUtils.getDoubleValue(orderSheetNode, "totalDiscountPrice");
        addDeliveryPrice = (cancelDeliveryPrice + totalRestDeliveryPrice > JsonUtils.getDoubleValue(orderSheetNode, "totalDeliveryPrice") ? cancelDeliveryPrice + totalRestDeliveryPrice - JsonUtils.getDoubleValue(orderSheetNode, "totalDeliveryPrice") : 0);
        deductPrice = addDeliveryPrice;
        refundPrice = cancelOrderPrice - deductPrice ;

        double temp = refundPrice;
        double orderYPoint = JsonUtils.getDoubleValue(orderSheetNode, "totalYPoint");
        double orderWelfarePoint = JsonUtils.getDoubleValue(orderSheetNode, "totalWelfarePoint");
        if(temp >= orderYPoint){
            temp = temp - orderYPoint;
            refundYPoint = orderYPoint;
        }else{
            refundYPoint = temp;
            temp = 0;
        }

        if(temp >= orderWelfarePoint){
            temp = temp - orderWelfarePoint;
            refundWelfarepoint = orderWelfarePoint;
        }else{
            refundWelfarepoint = temp;
            temp = 0;
        }
        refundPaymentPrice = temp;

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("cancelOrderPrice", cancelOrderPrice);
        item.put("cancelProductPrice", cancelProductPrice);
        item.put("cancelDeliveryPrice", cancelDeliveryPrice);
        item.put("deductPrice", deductPrice);
        item.put("addDeliveryPrice", addDeliveryPrice);
        item.put("refundPrice", refundPrice);
        item.put("refundYPoint", refundYPoint);
        item.put("refundWelfarepoint", refundWelfarepoint);
        item.put("refundPaymentPrice", refundPaymentPrice);
        item.put("orderSheetId", data.get("orderSheetId"));
        item.put("memberNo", orderSheetNode.get("memberNo"));
        item.put("orderChangeDeliveryList", deliveryPriceList);

//        부분취소시 결제정보 :
        List<Map<String, Object>> list = nodeBindingService.list(payment, "orderSheetId_equals="+data.get("orderSheetId")+"&orderChangeId_equals=null");
        if(list.size() > 0){
            item.put("payment", list.get(0));
        }

        Map<String, Object> restItem = new LinkedHashMap<>();
        restItem.put("totalProductPrice", JsonUtils.getDoubleValue(orderSheetNode, "totalProductPrice") - cancelProductPrice);
        restItem.put("totalDeliveryPrice", JsonUtils.getDoubleValue(orderSheetNode, "totalDeliveryPrice") - cancelDeliveryPrice + addDeliveryPrice);
        restItem.put("totalDiscountPrice", JsonUtils.getDoubleValue(orderSheetNode, "totalDiscountPrice") - cancelDiscountPrice);
        restItem.put("totalOrderPrice", JsonUtils.getDoubleValue(orderSheetNode, "totalOrderPrice") - cancelOrderPrice);
        restItem.put("totalPaymentPrice", JsonUtils.getDoubleValue(orderSheetNode, "totalPaymentPrice") - refundPaymentPrice);
        restItem.put("couponDiscountPrice", JsonUtils.getDoubleValue(orderSheetNode, "couponDiscountPrice") - cancelDiscountPrice);
        restItem.put("totalYPoint", JsonUtils.getDoubleValue(orderSheetNode, "totalYPoint") - refundYPoint);
        restItem.put("totalWelfarePoint", JsonUtils.getDoubleValue(orderSheetNode, "totalWelfarePoint") - refundWelfarepoint);
        restItem.put("restDeliveryPriceList", deliveryPriceListForRest);


        orderSheetNode.put("orderProduct", orderProducts);
//        data.put("orderChangeProduct", JsonUtils.parsingJsonToList(data.get("orderChangeProduct").toString()));
//        item.put("requestParams", data);
        result.put("willBeRefundedItem", item);
        result.put("restItem", restItem);
        result.put("orderSheet", orderSheetNode);
//        result.put("deliveryPriceList", deliveryPriceList);
        return result;
    }

    private List<Map<String, Object>> changeProductList(List<Map<String, Object>> orderProducts, List<Map<String, Object>> orderProductItems, List<Map<String, Object>> orderChangeProducts) {
        List<Map<String, Object>> orderChangeProductList = new ArrayList<>();
        for(Map<String, Object> orderChangeProduct : orderChangeProducts){
            for(Map<String, Object> orderProduct : orderProducts){
                Integer orderProductId = JsonUtils.getIntValue(orderProduct, "orderProductId") ;
                if(orderProductId.equals(JsonUtils.getIntValue(orderChangeProduct, "orderProductId"))){
                    Map<String, Object> temp = new HashMap<>();
                    temp.putAll(orderProduct);
                    temp.putAll(orderChangeProduct);
                    if(JsonUtils.getIntValue(orderChangeProduct, "quantity").equals(JsonUtils.getIntValue(orderProduct, "quantity"))){
                        List<Map<String, Object>> subOrderProdductItems = new ArrayList<>() ;
                        for(Map<String, Object> orderProductItem : orderProductItems){
                            if(orderProductId == JsonUtils.getIntValue(orderProductItem, "orderProductId")){
                                subOrderProdductItems.add(orderProductItem) ;
                            }
                        }
                        temp.put("orderProductItem", subOrderProdductItems) ;
                    }
                    orderChangeProductList.add(temp);
                }
            }
        }
        return orderChangeProductList;
    }


    private List<Map<String, Object>> restProductList(List<Map<String, Object>> orderProducts, List<Map<String, Object>> orderProductItems, List<Map<String, Object>> orderChangeProducts) {
        List<Map<String, Object>> orderRestProductList = new ArrayList<>();


        for(Map<String, Object> orderProduct : orderProducts){
            boolean exist = false;
            for(Map<String, Object> orderChangeProduct : orderChangeProducts){
                Integer orderProductId = JsonUtils.getIntValue(orderProduct, "orderProductId") ;
                if(orderProductId.equals(JsonUtils.getIntValue(orderChangeProduct, "orderProductId"))){
                    exist = true;
                    if(JsonUtils.getIntValue(orderProduct, "quantity") > JsonUtils.getIntValue(orderChangeProduct, "quantity")){
                        orderProduct.put("quantity", JsonUtils.getIntValue(orderProduct, "quantity") - JsonUtils.getIntValue(orderChangeProduct, "quantity"));
                        List<Map<String, Object>> subOrderProdductItems = new ArrayList<>() ;
                        for(Map<String, Object> orderProductItem : orderProductItems){
                            if(orderProductId == JsonUtils.getIntValue(orderProductItem, "orderProductId")){
                                subOrderProdductItems.add(orderProductItem) ;
                            }
                        }
                        orderProduct.put("orderProductItem", subOrderProdductItems) ;
                        orderRestProductList.add(orderProduct);
                    }else{
                        //제거대상
                    }
                }
            }

            if(!exist && JsonUtils.getIntValue(orderProduct, "quantity") > 0){
                orderRestProductList.add(orderProduct);
            }
        }

        return orderRestProductList;
    }
}
