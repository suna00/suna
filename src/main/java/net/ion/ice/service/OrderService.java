package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.session.SessionService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("orderService")
public class OrderService {

    private static Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService;
    @Autowired
    private DeliveryService deliveryService;
    @Autowired
    private CouponService couponService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private PointService pointService;

    /**
     * 임시 주문서 조회
     */
    public ExecuteContext tempOrderRead(ExecuteContext context) throws IOException {
        Integer totalSize = 0;
        Map<String, Object> session = sessionService.getSession(context.getHttpRequest());
        String memberNo = JsonUtils.getStringValue(session, "member.memberNo");
        List<Map<String, Object>> tempOrderProducts = nodeBindingService.list("tempOrderProduct", "sorting=created&tempOrderId_equals=" + context.getData().get("tempOrderId"));
        List<Map<String, Object>> tempOrderProductItems = nodeBindingService.list("tempOrderProductItem", "sorting=created&tempOrderId_equals=" + context.getData().get("tempOrderId"));
        // cart 만들기
        for (Map<String, Object> tempOrderProduct : tempOrderProducts) {
            String tempOrderProductId = JsonUtils.getStringValue(tempOrderProduct, "tempOrderProductId");
            List<Map<String, Object>> subProductItems = new ArrayList<>();
            for (Map<String, Object> tempOrderProductItem : tempOrderProductItems) {
                if (StringUtils.equals(tempOrderProductId, JsonUtils.getStringValue(tempOrderProductItem, "tempOrderProductId"))) {
                    subProductItems.add(tempOrderProductItem);
                }
            }
            tempOrderProduct.put("tempOrderProductItem", subProductItems);
        }

        List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(tempOrderProducts, "tempOrder");
        Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "tempOrder");

        QueryResult queryResult = new QueryResult();
        List<QueryResult> items = new ArrayList<>();

        for (String key : deliveryPriceList.keySet()) {
            QueryResult itemResult = new QueryResult();
            itemResult.put("deliverySeq", key);
            List<Map<String, Object>> priceList = (List<Map<String, Object>>) deliveryPriceList.get(key);

            itemResult.put("deliveryPrice", priceList.get(0).get("deliveryPrice"));

            List<Map<String, Object>> subProductResult = new ArrayList<>();
            Double totalOrderPrice = 0D;
            for (Map<String, Object> priceProduct : priceList) {
                subProductResult.add(priceProduct);
                totalOrderPrice += JsonUtils.getDoubleValue(priceProduct, "orderPrice");
            }

            itemResult.put("orderPrice", totalOrderPrice);
            itemResult.put("item", subProductResult);
            totalSize += subProductResult.size();
            items.add(itemResult);
        }

        queryResult.put("length", totalSize);
        queryResult.put("items", items);

        Map<String, Object> tempOrder = nodeBindingService.read("tempOrder", JsonUtils.getStringValue(context.getData(), "tempOrderId"));

        if(StringUtils.equals(JsonUtils.getStringValue(tempOrder, "memberNo"), memberNo)){
            context.setResult(queryResult);
        }

        return context;
    }

    /**
     * 바로 주문 저장
     */
    public void buyItNow(ExecuteContext context) {
        try {
            String tempOrderId = createTempOrder(context, true);
            Map<String, Object> extraData = new HashMap<>();
            extraData.put("tempOrderId", tempOrderId);
            context.setResult(CommonService.getResult("O0001", extraData)); // 성공 시
        } catch (Exception e) {
            context.setResult("");
            e.printStackTrace();
        }
    }

    /**
     * 임시 주문서 저장
     */
    public void addTempOrder(ExecuteContext context) {
        try {
            String tempOrderId = createTempOrder(context, false);
            Map<String, Object> extraData = new HashMap<>();
            extraData.put("tempOrderId", tempOrderId);
            context.setResult(CommonService.getResult("O0001", extraData)); // 성공 시
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 사용자의 포인트, 쿠폰갯수를 조회
     */
    public Map<String, Object> getSummary(String memberNo) {
        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("member").getJdbcTemplate();
        String query = "SELECT name, membershipLevel, date_format(now(),'%m') as month,(SELECT IFNULL(sum(balance), 0) AS useableYPoint FROM ypoint WHERE memberNo = " + memberNo + " AND YPointType != 'tobe') AS useableYPoint, (SELECT IFNULL(sum(balance), 0) AS useableWelfarepoint FROM welfarepoint WHERE memberNo = " + memberNo + ") AS useableWelfarepoint, (SELECT IFNULL(count(*), 0) AS haveCoupon FROM coupon WHERE memberNo = " + memberNo + " AND couponStatus='n' AND endDate >= now()) AS haveCoupon from member where memberNo = " + memberNo;
        Map<String, Object> response = jdbcTemplate.queryForMap(query);

        return response;
    }

    /**
     * 상품에 적용 가능한 쿠폰을 조회
     */
    public List<Map<String, Object>> getCoupons(String memberNo, String tempOrderId) {
        List<Map<String, Object>> response = new ArrayList<>();
        List<Map<String, Object>> tempOrderProductList = nodeBindingService.list("tempOrderProduct", "tempOrderId_equals=".concat(tempOrderId));
        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("coupon").getJdbcTemplate();
        for (Map<String, Object> tempOrderProduct : tempOrderProductList) {
            String orderPrice = String.valueOf(tempOrderProduct.get("orderPrice"));
            String productId = String.valueOf(tempOrderProduct.get("productId"));
            String query = "select z.* from ( SELECT x.couponId, y.*, " + orderPrice + " as productPrice, IF(y.benefitsType='discountRate', IF((" + orderPrice + " / 100 * y.benefitsPrice) > y.maxDiscountPrice, y.maxDiscountPrice,(" + orderPrice + " / 100 * y.benefitsPrice)) , IF(y.minPurchasePrice < 1500, y.benefitsPrice, 0)) as discountPrice FROM ( (SELECT a.*, c.productId as useableProductId FROM coupon a, coupontypetoproductmap c WHERE a.memberNo = " + memberNo + " AND c.productId = " + productId + " AND a.couponTypeId = c.couponTypeId AND a.siteType in (select code from commoncode where upperCode='siteType' and find_in_set(code,IF(IFNULL(null, 'all') != 'all', concat('company',',all'), 'all')) > 0) AND a.channelType in (select code from commoncode where upperCode='channelType' and find_in_set(code,IF(IFNULL(null, 'all') != 'all', concat('pc',',all'), 'all')) > 0) AND a.startDate <= now() AND a.endDate >= now() AND a.couponStatus = 'n') UNION ALL (SELECT a.*, c.productId as useableProductId FROM coupon a , ( SELECT couponTypeId,productId FROM couponTypeToCategoryMap c, producttocategorymap p WHERE productId = " + productId + " AND p.categoryId = c.categoryId GROUP BY couponTypeId ) c WHERE a.memberNo = " + memberNo + " AND a.couponTypeId = c.couponTypeId AND a.siteType in (select code from commoncode where upperCode='siteType' and find_in_set(code,IF(IFNULL(null, 'all') != 'all', concat('company',',all'), 'all')) > 0) AND a.channelType in (select code from commoncode where upperCode='channelType' and find_in_set(code,IF(IFNULL(null, 'all') != 'all', concat('pc',',all'), 'all')) > 0) AND a.startDate <= now() AND a.endDate >= now() AND a.couponStatus = 'n' )) x, coupontype y where x.couponTypeId = y.couponTypeId ) z where z.discountPrice > 0 order by z.discountPrice desc";
            List<Map<String, Object>> applicableCoupons = jdbcTemplate.queryForList(query);
            tempOrderProduct.put("applicableCoupons", applicableCoupons);
            response.add(tempOrderProduct);
        }
        return response;

    }

    /**
     * 최종가격을 계산
     */
    public Double getFinalPrice(String reqTempOrderId, String reqMemberNo, String reqUseYPoint, String reqUseWelfarepoint, String usedCoupon) {
        double totalPrice = 0;

        try {

            Map<String, Object> couponIds = JsonUtils.parsingJsonToMap(usedCoupon);

            /*포인트*/
            double useYPoint = Double.parseDouble(reqUseYPoint);
            double useWelfarepoint = Double.parseDouble(reqUseWelfarepoint);
            double deliveryPrice = 0;

            List<Map<String, Object>> tempOrderProducts = nodeBindingService.list("tempOrderProduct", "sorting=created&tempOrderId_equals=" + String.valueOf(reqTempOrderId));
            List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(tempOrderProducts, "tempOrder");
            Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "tempOrder");

            for (String key : deliveryPriceList.keySet()) {
                List<Map<String, Object>> priceList = (List<Map<String, Object>>) deliveryPriceList.get(key);
                deliveryPrice += (Double) priceList.get(0).get("deliveryPrice");
            }

            for (Map<String, Object> tempOrderProduct : tempOrderProducts) {
                String tempOrderProductId = JsonUtils.getStringValue(tempOrderProduct, "tempOrderProductId");
                Integer couponId = JsonUtils.getIntNullableValue(couponIds, tempOrderProductId);
                Integer memberNo = Integer.parseInt(reqMemberNo);
                Map<String, Double> discountPriceMap = couponService.productCouponDiscountPrice(Integer.parseInt(tempOrderProductId), couponId, "", memberNo);

                Double resultOrderPrice = discountPriceMap.get("resultOrderPrice");
                totalPrice += resultOrderPrice;
            }


            totalPrice = totalPrice - useYPoint - useWelfarepoint + deliveryPrice;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return totalPrice;
    }

    /**
     * PG 결제 승인 요청 전에 검증하는 Method.
     */
    public void verification(ExecuteContext context) {
        try {
            Map<String, Object> data = context.getData();
            double totalPrice = 0;
            Map<String, Object> session = sessionService.getSession(context.getHttpRequest());
            Map<String, Object> couponIds = JsonUtils.parsingJsonToMap((String) data.get("usedCoupon"));
            /*포인트*/
            Map<String, Object> summaryResponse = getSummary(JsonUtils.getStringValue(session, "member.memberNo"));

            double useableYPoint = ((BigDecimal) summaryResponse.get("useableYPoint")).doubleValue();
            double useableWelfarepoint = ((BigDecimal) summaryResponse.get("useableWelfarepoint")).doubleValue();
            double useYPoint = JsonUtils.getDoubleValue(data, "useYPoint");
            double useWelfarepoint = JsonUtils.getDoubleValue(data, "useWelfarepoint");
            double finalDeliveryPrice = 0;

            double finalPrice = JsonUtils.getDoubleValue(data, "finalPrice");

            if (useYPoint > useableYPoint && useWelfarepoint > useableWelfarepoint) {
                context.setResult(CommonService.getResult("O0002"));            // 실패
                return;
            }

            /*쿠폰*/
//            boolean duplicated = repetitionCheck(couponIds.values());// 쿠폰 아이디 중복 체크

//            if(duplicated){
//                context.setResult(CommonService.getResult("O0002"));            // 실패
//                return;
//            }
            List<Map<String, Object>> tempOrderProducts = nodeBindingService.list("tempOrderProduct", "sorting=created&tempOrderId_equals=" + context.getData().get("tempOrderId"));

            for (Map<String, Object> tempOrderProduct : tempOrderProducts) {
                String tempOrderProductId = JsonUtils.getStringValue(tempOrderProduct, "tempOrderProductId");
                Integer couponId = JsonUtils.getIntNullableValue(couponIds, tempOrderProductId);
                Integer memberNo = JsonUtils.getIntValue(session, "member.memberNo");
                Map<String, Double> discountPriceMap = couponService.productCouponDiscountPrice(Integer.parseInt(tempOrderProductId), couponId, "", memberNo);

                Double resultOrderPrice = discountPriceMap.get("resultOrderPrice");
                totalPrice += resultOrderPrice;
            }

            List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(tempOrderProducts, "tempOrder");
            Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "tempOrder");

            for (String key : deliveryPriceList.keySet()) {
                List<Map<String, Object>> priceList = (List<Map<String, Object>>) deliveryPriceList.get(key);
                if (priceList.get(0).get("deliveryPrice") != null) {
                    finalDeliveryPrice += Double.parseDouble(String.valueOf(priceList.get(0).get("deliveryPrice")));
                }
            }

            totalPrice = totalPrice - useYPoint - useWelfarepoint + finalDeliveryPrice;

            if (totalPrice != finalPrice) {                       // 최종 가격 검증 & 쿠폰 중복 검증
                context.setResult(CommonService.getResult("O0003"));      // 검증실패
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("ordr_mony", totalPrice);
                context.setResult(CommonService.getResult("O0004", result));      // 검증성공

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 포인트로 인한 0원 결제
     */
    public ExecuteContext nonePgOrder(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String orderSheetId = JsonUtils.getStringValue(data, "ordrIdxx");
        List<Map<String, Object>> tempOrder = nodeBindingService.list("tempOrder", "tempOrderId_equals=" + orderSheetId);
        List<Map<String, Object>> tempOrderProducts = nodeBindingService.list("tempOrderProduct", "sorting=created&tempOrderId_equals=" + orderSheetId);
        List<Map<String, Object>> tempOrderProductItems = nodeBindingService.list("tempOrderProductItem", "sorting=created&tempOrderId_equals=" + orderSheetId);
        Map<String, Object> session = null;
        try {
            session = sessionService.getSession(context.getHttpRequest());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        for (Map<String, Object> tempOrderProduct : tempOrderProducts) {
            Integer tempOrderProductId = JsonUtils.getIntValue(tempOrderProduct, "tempOrderProductId");
            List<Map<String, Object>> subTempOrderProdductItems = new ArrayList<>();
            for (Map<String, Object> tempOrderProductItem : tempOrderProductItems) {
                if (tempOrderProductId.equals(JsonUtils.getIntValue(tempOrderProductItem, "tempOrderProductId"))) {
                    subTempOrderProdductItems.add(tempOrderProductItem);
                }
            }
            tempOrderProduct.put("tempOrderProductItem", subTempOrderProdductItems);
        }
        List<QueryResult> items = new ArrayList<>();
        Double totalProductPrice = 0D;
        Double totalDeliveryPrice = 0D;
        Double totalDiscountPrice = 0D;
        Double totalOrderPrice = 0D;
        Double couponDiscountPrice = 0D;
        Double totalPaymentPrice = JsonUtils.getDoubleValue(data, "finalPrice");
        Double welfarePoint = Double.parseDouble(String.valueOf(data.get("useWelfarepoint")));
        Double YPoint = Double.parseDouble(String.valueOf(data.get("useYPoint")));

        Integer memberNo = JsonUtils.getIntNullableValue(session, "member.memberNo");
        String memberName = JsonUtils.getStringValue(session, "member.name");
        String memberCellphone = JsonUtils.getStringValue(session, "member.cellphone");
        Integer affiliateId = JsonUtils.getIntNullableValue(session, "member.affiliateId");

        Map<String, Object> summaryResponse = getSummary(JsonUtils.getStringValue(session, "member.memberNo"));

        Double useableYPoint = ((BigDecimal) summaryResponse.get("useableYPoint")).doubleValue();
        Double useableWelfarepoint = ((BigDecimal) summaryResponse.get("useableWelfarepoint")).doubleValue();


        if (YPoint > useableYPoint && welfarePoint > useableWelfarepoint) {
            context.setResult(CommonService.getResult("O0006"));
        }

        Map<String, Object> pointStoreMap = new HashMap<>();

        pointStoreMap.put("orderSheetId", orderSheetId);
        pointStoreMap.put("memberNo", memberNo);
        pointStoreMap.put("affiliateId", affiliateId);
        pointStoreMap.put("YPoint", YPoint.intValue());
        pointStoreMap.put("welfarePoint", welfarePoint.intValue());

        if (!pointService.useYPoint(pointStoreMap) && !pointService.useWelfarePoint(pointStoreMap)) {
            context.setResult(CommonService.getResult("O0006"));
            return context;
        }

        List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(tempOrderProducts, "tempOrder");
        Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "tempOrder");

        for (String key : deliveryPriceList.keySet()) {
            QueryResult itemResult = new QueryResult();
            itemResult.put("deliverySeq", key);
            List<Map<String, Object>> priceList = (List<Map<String, Object>>) deliveryPriceList.get(key);
            itemResult.put("deliveryPrice", priceList.get(0).get("deliveryPrice"));
            List<Map<String, Object>> subProductResult = new ArrayList<>();
            for (Map<String, Object> priceProduct : priceList) {
                subProductResult.add(priceProduct);
            }

            itemResult.put("item", subProductResult);
            items.add(itemResult);
        }

        Map<String, Object> couponIds = null;
        try {
            couponIds = JsonUtils.parsingJsonToMap((String) data.get("usedCoupon"));
        } catch (Exception e) {
            e.printStackTrace();
        }

//        boolean duplicated = repetitionCheck(couponIds.values()); // 쿠폰 아이디 중복 체크

//        if (duplicated) {
//            bSucc = "false";
//            return bSucc;
//        }
        boolean cellPhonePaymentMode = false;
        for (Map<String, Object> deliveryItem : items) {

            totalDeliveryPrice += JsonUtils.getDoubleValue(deliveryItem, "deliveryPrice");

            for (Map<String, Object> product : (List<Map<String, Object>>) deliveryItem.get("item")) {
                ///////orderProduct////////
                Map<String, Object> storeOrderProduct = new HashMap<>();
                /*휴대폰구매*/
                if(StringUtils.equals(JsonUtils.getStringValue(product, "product.contentsType"), "contentsType>cellphone") && !cellPhonePaymentMode){
                    cellPhonePaymentMode = true;
                }
                storeOrderProduct.put("orderSheetId", orderSheetId);
                storeOrderProduct.put("productId", JsonUtils.getIntValue(product, "productId"));
                storeOrderProduct.put("baseOptionItemId", JsonUtils.getIntValue(product, "baseOptionItemId"));
                storeOrderProduct.put("baseOptionItemName", JsonUtils.getStringValue(product, "baseOptionItem.name"));
                storeOrderProduct.put("quantity", JsonUtils.getIntValue(product, "quantity"));
                storeOrderProduct.put("salePrice", JsonUtils.getDoubleValue(product, "salePrice"));
                storeOrderProduct.put("baseAddPrice", JsonUtils.getDoubleValue(product, "baseAddPrice"));
                storeOrderProduct.put("productPrice", JsonUtils.getDoubleValue(product, "productPrice"));
                storeOrderProduct.put("totalAddOptionPrice", JsonUtils.getDoubleValue(product, "totalAddOptionPrice"));
                storeOrderProduct.put("orderPrice", JsonUtils.getDoubleValue(product, "orderPrice"));

                String tempOrderProductId = JsonUtils.getStringValue(product, "tempOrderProductId");
                Integer couponId = JsonUtils.getIntNullableValue(couponIds, tempOrderProductId);
                storeOrderProduct.put("couponId", couponId);

                Map<String, Double> discountPriceMap = couponService.productCouponDiscountPrice(Integer.parseInt(tempOrderProductId), couponId, "", memberNo);
                storeOrderProduct.put("couponDiscountPrice", discountPriceMap.get("resultDiscountPrice"));
                storeOrderProduct.put("vendorId", JsonUtils.getIntValue(product, "vendorId"));

                if (StringUtils.equals(JsonUtils.getStringValue(data, "usePayMethod"), "000000000000")) {
                    storeOrderProduct.put("orderStatus", "order002"); //무통장입금_입금대기
                } else {
                    storeOrderProduct.put("orderStatus", "order003"); //포인트전액결제_결제완료
                }

                storeOrderProduct.put("paymentPrice", discountPriceMap.get("resultOrderPrice"));

                totalProductPrice += discountPriceMap.get("resultOrderPrice");
                totalDiscountPrice += discountPriceMap.get("resultDiscountPrice");

                Node productBaseOptionItemNode = nodeService.getNode("productOptionItem", JsonUtils.getStringValue(product, "baseOptionItemId"));

                Integer beforeQuantity = productBaseOptionItemNode.getIntValue("stockQuantity");

                if (beforeQuantity > 0) {
                    Integer afterQuantity = beforeQuantity - JsonUtils.getIntValue(product, "quantity");

                    productBaseOptionItemNode.put("stockQuantity", afterQuantity);
                    nodeService.executeNode(productBaseOptionItemNode, "productOptionItem", CommonService.UPDATE);
                }

                Node orderProductNode = (Node) nodeService.executeNode(storeOrderProduct, "orderProduct", CommonService.CREATE);

                for (Map<String, Object> productItem : (List<Map<String, Object>>) product.get("tempOrderProductItem")) {
                    Map<String, Object> storeOrderProductItem = new HashMap<>();
                    storeOrderProductItem.put("orderSheetId", orderSheetId);
                    storeOrderProductItem.put("orderProductId", orderProductNode.getId());
                    storeOrderProductItem.put("productId", JsonUtils.getStringValue(productItem, "productId"));
                    storeOrderProductItem.put("addOptionItemId", JsonUtils.getStringValue(productItem, "addOptionItemId"));
                    storeOrderProductItem.put("addOptionItemName", "");
                    storeOrderProductItem.put("quantity", JsonUtils.getStringValue(productItem, "quantity"));
                    storeOrderProductItem.put("addOptionPrice", JsonUtils.getStringValue(productItem, "addOptionPrice"));

                    Node productItemBaseOptionItemNode = nodeService.getNode("productOptionItem", JsonUtils.getStringValue(productItem, "addOptionItemId"));

                    Integer productItemBeforeQuantity = productItemBaseOptionItemNode.getIntValue("stockQuantity");

                    if (beforeQuantity > 0) {
                        Integer afterQuantity = productItemBeforeQuantity - JsonUtils.getIntValue(product, "quantity");

                        productItemBaseOptionItemNode.put("stockQuantity", afterQuantity);
                        nodeService.executeNode(productItemBaseOptionItemNode, "productOptionItem", CommonService.UPDATE);
                    }

                    nodeService.executeNode(storeOrderProductItem, "orderProductItem", CommonService.CREATE);
                }
//                orderProductIds.add(orderProductNode.getId());
            }
//            storeOrderDeliveryPrice.put("orderProductIds", StringUtils.join(orderProductIds, ","));
//            nodeService.executeNode(storeOrderDeliveryPrice, "orderDeliveryPrice", CommonService.CREATE);
        }
        /*휴대폰구매*/
        if(cellPhonePaymentMode){
            totalProductPrice = 0D;
        }
        totalOrderPrice = totalProductPrice - YPoint - welfarePoint + totalDeliveryPrice; //총 주문금액
        totalDiscountPrice = totalDiscountPrice + YPoint + welfarePoint;
        if (!StringUtils.equals(totalPaymentPrice.toString(), totalOrderPrice.toString())) {
            context.setResult(CommonService.getResult("O0006"));
            return context;
        }

        Map<String, Object> storeOrderSheet = new HashMap<>();

        storeOrderSheet.put("orderSheetId", orderSheetId);   //주문서 번호

        storeOrderSheet.put("memberNo", memberNo);                                                  //회원번호
        storeOrderSheet.put("buyerName", memberName);                                               //구매자명
        storeOrderSheet.put("buyerTel", memberCellphone);                                           //구매자전화번호
        storeOrderSheet.put("cartId", JsonUtils.getIntValue(tempOrder.get(0), "cartId"));      //카트 아이디
        storeOrderSheet.put("siteId", JsonUtils.getStringValue(tempOrder.get(0), "siteId"));   //사이트 아이디
        storeOrderSheet.put("totalProductPrice", totalProductPrice);         //총상품가격
        storeOrderSheet.put("totalOrderPrice", totalOrderPrice);             //총주문가격
        storeOrderSheet.put("totalDiscountPrice", totalDiscountPrice);       //총할인액
        storeOrderSheet.put("totalDeliveryPrice", totalDeliveryPrice);       //총배송비
        storeOrderSheet.put("totalPaymentPrice", totalPaymentPrice);         //결제금액
        storeOrderSheet.put("couponDiscountPrice", couponDiscountPrice);     //쿠폰 할인액
        storeOrderSheet.put("totalWelfarePoint", welfarePoint);         //사용한 복지포인트
        storeOrderSheet.put("totalYPoint", YPoint);                     //사용한 Y포인트


        if (StringUtils.equals(JsonUtils.getStringValue(data, "usePayMethod"), "000000000000")) {
            storeOrderSheet.put("usePayMethod", "000000000000");
            storeOrderSheet.put("usePayMethodName", "무통장입금");

            /*현금영수증발급신청데이터*/
            if (!JsonUtils.getStringValue(data, "trCode").equals("")) {

                Map<String, Object> storeCashReceiptRequest = new HashMap<>();

                storeCashReceiptRequest.put("orderSheetId", orderSheetId);
                storeCashReceiptRequest.put("trCode", JsonUtils.getStringValue(data, "trCode"));
                storeCashReceiptRequest.put("idInfo", JsonUtils.getStringValue(data, "idInfo"));

                nodeService.executeNode(storeCashReceiptRequest, "cashReceiptRequest", CommonService.CREATE);
            }

        } else {
            /*휴대폰구매*/
            if(cellPhonePaymentMode){
                storeOrderSheet.put("usePayMethod", "999999999999");
                storeOrderSheet.put("usePayMethodName", "휴대폰쿠폰");
            }else{
                storeOrderSheet.put("usePayMethod", "111111111111");
                storeOrderSheet.put("usePayMethodName", "포인트");
            }

        }


        storeOrderSheet.put("purchaseaAgreementYn", "y");
        storeOrderSheet.put("purchaseDeviceType", "");
        nodeService.executeNode(storeOrderSheet, "orderSheet", CommonService.CREATE);


        List<Map<String, Object>> orderProducts = nodeBindingService.list("orderProduct", "sorting=created&orderSheetId_equals=" + orderSheetId);
        List<Map<String, Object>> orderProductItems = nodeBindingService.list("orderProductItem", "sorting=created&orderSheetId_equals=" + orderSheetId);

        for (Map<String, Object> orderProduct : orderProducts) {
            Integer orderProductId = JsonUtils.getIntValue(orderProduct, "orderProductId");
            List<Map<String, Object>> subOrderProductItems = new ArrayList<>();
            for (Map<String, Object> orderProductItem : orderProductItems) {
                if (orderProductId.equals(JsonUtils.getIntValue(orderProductItem, "orderProductId"))) {
                    subOrderProductItems.add(orderProductItem);
                }
            }
            orderProduct.put("orderProductItem", subOrderProductItems);
        }

        List<Map<String, Object>> orderDeliveryProductList = deliveryService.makeDeliveryData(orderProducts, "order");
        Map<String, Object> orderDeliveryPriceList = deliveryService.calculateDeliveryPrice(orderDeliveryProductList, "order");


        deliveryService.makeDeliveryPrice(orderSheetId, orderDeliveryPriceList);

        Map<String, Object> storePayment = new HashMap<>();

        if (StringUtils.equals(JsonUtils.getStringValue(data, "usePayMethod"), "000000000000")) {
            storePayment.put("orderSheetId", orderSheetId);
            storePayment.put("memberNo", memberNo);
            storePayment.put("depositor", JsonUtils.getStringValue(data, "depositor")); //입금자명
            storePayment.put("usePayMethod", JsonUtils.getStringValue(data, "usePayMethod"));
            storePayment.put("usePayMethodName", "무통장입금");

        } else {
            /*휴대폰구매*/
            if(cellPhonePaymentMode){
                storePayment.put("orderSheetId", orderSheetId);
                storePayment.put("memberNo", memberNo);
                storePayment.put("usePayMethod", JsonUtils.getStringValue(data, "usePayMethod"));
                storePayment.put("usePayMethodName", "휴대폰쿠폰결제");
            }else{
                storePayment.put("orderSheetId", orderSheetId);
                storePayment.put("memberNo", memberNo);
                storePayment.put("usePayMethod", JsonUtils.getStringValue(data, "usePayMethod"));
                storePayment.put("usePayMethodName", "포인트결제");
            }
        }
        nodeService.executeNode(storePayment, "payment", CommonService.CREATE);

        createDelivery(data, JsonUtils.getIntValue(session, "member.memberNo"));

        context.setResult(CommonService.getResult("O0005"));

        nodeBindingService.delete("tempOrder", orderSheetId);

        return context;
    }

    /**
     * 최종 주문서 생성
     */

    public String createOrderSheet(Map<String, Object> responseMap, HttpServletRequest request) {
        String bSucc = "true";
        String orderSheetId = JsonUtils.getStringValue(responseMap, "ordrIdxx");
        List<Map<String, Object>> tempOrder = nodeBindingService.list("tempOrder", "tempOrderId_equals=" + orderSheetId);
        List<Map<String, Object>> tempOrderProducts = nodeBindingService.list("tempOrderProduct", "sorting=created&tempOrderId_equals=" + orderSheetId);
        List<Map<String, Object>> tempOrderProductItems = nodeBindingService.list("tempOrderProductItem", "sorting=created&tempOrderId_equals=" + orderSheetId);
        Map<String, Object> session = null;
        try {
            session = sessionService.getSession(request);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        for (Map<String, Object> tempOrderProduct : tempOrderProducts) {
            Integer tempOrderProductId = JsonUtils.getIntValue(tempOrderProduct, "tempOrderProductId");
            List<Map<String, Object>> subTempOrderProdductItems = new ArrayList<>();
            for (Map<String, Object> tempOrderProductItem : tempOrderProductItems) {
                if (tempOrderProductId.equals(JsonUtils.getIntValue(tempOrderProductItem, "tempOrderProductId"))) {
                    subTempOrderProdductItems.add(tempOrderProductItem);
                }
            }
            tempOrderProduct.put("tempOrderProductItem", subTempOrderProdductItems);
        }
        List<QueryResult> items = new ArrayList<>();
        Double totalProductPrice = 0D;
        Double totalDeliveryPrice = 0D;
        Double totalDiscountPrice = 0D;
        Double totalOrderPrice = 0D;
        Double couponDiscountPrice = 0D;
        Double totalPaymentPrice = Double.parseDouble(String.valueOf(responseMap.get("amount")));
        Double welfarePoint = Double.parseDouble(String.valueOf(responseMap.get("useWelfarepoint")));
        Double YPoint = Double.parseDouble(String.valueOf(responseMap.get("useYPoint")));


        Integer memberNo = JsonUtils.getIntNullableValue(session, "member.memberNo");
        String memberName = JsonUtils.getStringValue(session, "member.name");
        String memberCellphone = JsonUtils.getStringValue(session, "member.cellphone");
        Integer affiliateId = JsonUtils.getIntNullableValue(session, "member.affiliateId");

        Map<String, Object> pointStoreMap = new HashMap<>();

        pointStoreMap.put("orderSheetId", orderSheetId);
        pointStoreMap.put("memberNo", memberNo);
        pointStoreMap.put("affiliateId", affiliateId);
        pointStoreMap.put("YPoint", YPoint.intValue());
        pointStoreMap.put("welfarePoint", welfarePoint.intValue());

        if (!pointService.useYPoint(pointStoreMap) && !pointService.useWelfarePoint(pointStoreMap)) {
            bSucc = "false";
            return bSucc;
        }

        List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(tempOrderProducts, "tempOrder");
        Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "tempOrder");

        for (String key : deliveryPriceList.keySet()) {
            QueryResult itemResult = new QueryResult();
            itemResult.put("deliverySeq", key);
            List<Map<String, Object>> priceList = (List<Map<String, Object>>) deliveryPriceList.get(key);
            itemResult.put("deliveryPrice", priceList.get(0).get("deliveryPrice"));

            List<Map<String, Object>> subProductResult = new ArrayList<>();
            for (Map<String, Object> priceProduct : priceList) {
                subProductResult.add(priceProduct);
            }

            itemResult.put("item", subProductResult);
            items.add(itemResult);
        }

        Map<String, Object> couponIds = null;
        try {
            couponIds = JsonUtils.parsingJsonToMap((String) responseMap.get("usedCoupon"));
        } catch (Exception e) {
            e.printStackTrace();
        }

//        boolean duplicated = repetitionCheck(couponIds.values()); // 쿠폰 아이디 중복 체크

//        if (duplicated) {
//            bSucc = "false";
//            return bSucc;
//        }

        for (Map<String, Object> deliveryItem : items) {
//            Map<String, Object> storeOrderDeliveryPrice = new HashMap<>();
//            List orderProductIds = new ArrayList();
//            storeOrderDeliveryPrice.put("orderSheetId", JsonUtils.getStringValue(responseMap, "ordrIdxx"));
//            storeOrderDeliveryPrice.put("deliveryPrice", JsonUtils.getDoubleValue(deliveryItem, "deliveryPrice"));

            totalDeliveryPrice += JsonUtils.getDoubleValue(deliveryItem, "deliveryPrice");

            for (Map<String, Object> product : (List<Map<String, Object>>) deliveryItem.get("item")) {
                Map<String, Object> storeOrderProduct = new HashMap<>();

                ///////orderProduct////////
                storeOrderProduct.put("orderSheetId", orderSheetId);
                storeOrderProduct.put("productId", JsonUtils.getIntValue(product, "productId"));
                storeOrderProduct.put("baseOptionItemId", JsonUtils.getIntValue(product, "baseOptionItemId"));
                storeOrderProduct.put("baseOptionItemName", JsonUtils.getStringValue(product, "baseOptionItem.name"));
                storeOrderProduct.put("quantity", JsonUtils.getIntValue(product, "quantity"));
                storeOrderProduct.put("salePrice", JsonUtils.getDoubleValue(product, "salePrice"));
                storeOrderProduct.put("baseAddPrice", JsonUtils.getDoubleValue(product, "baseAddPrice"));
                storeOrderProduct.put("productPrice", JsonUtils.getDoubleValue(product, "productPrice"));
                storeOrderProduct.put("totalAddOptionPrice", JsonUtils.getDoubleValue(product, "totalAddOptionPrice"));
                storeOrderProduct.put("orderPrice", JsonUtils.getDoubleValue(product, "orderPrice"));

                String tempOrderProductId = JsonUtils.getStringValue(product, "tempOrderProductId");
                Integer couponId = JsonUtils.getIntNullableValue(couponIds, tempOrderProductId);
                storeOrderProduct.put("couponId", couponId);

                Map<String, Double> discountPriceMap = couponService.productCouponDiscountPrice(Integer.parseInt(tempOrderProductId), couponId, "", memberNo);
                storeOrderProduct.put("couponDiscountPrice", discountPriceMap.get("resultDiscountPrice"));
                storeOrderProduct.put("vendorId", JsonUtils.getIntValue(product, "vendorId"));
                storeOrderProduct.put("paymentPrice", discountPriceMap.get("resultOrderPrice"));
                storeOrderProduct.put("orderStatus", JsonUtils.getStringValue(responseMap, "orderStatus"));
                totalProductPrice += discountPriceMap.get("resultOrderPrice");
                totalDiscountPrice += discountPriceMap.get("resultDiscountPrice");

                Node productBaseOptionItemNode = nodeService.getNode("productOptionItem", JsonUtils.getStringValue(product, "baseOptionItemId"));

                Integer beforeQuantity = productBaseOptionItemNode.getIntValue("stockQuantity");

                if (beforeQuantity > 0) {
                    Integer afterQuantity = beforeQuantity - JsonUtils.getIntValue(product, "quantity");

                    productBaseOptionItemNode.put("stockQuantity", afterQuantity);
                    nodeService.executeNode(productBaseOptionItemNode, "productOptionItem", CommonService.UPDATE);
                }

                Node orderProductNode = (Node) nodeService.executeNode(storeOrderProduct, "orderProduct", CommonService.CREATE);

                for (Map<String, Object> productItem : (List<Map<String, Object>>) product.get("tempOrderProductItem")) {
                    Map<String, Object> storeOrderProductItem = new HashMap<>();
                    storeOrderProductItem.put("orderSheetId", orderSheetId);
                    storeOrderProductItem.put("orderProductId", orderProductNode.getId());
                    storeOrderProductItem.put("productId", JsonUtils.getStringValue(productItem, "productId"));
                    storeOrderProductItem.put("addOptionItemId", JsonUtils.getStringValue(productItem, "addOptionItemId"));
                    storeOrderProductItem.put("addOptionItemName", "");
                    storeOrderProductItem.put("quantity", JsonUtils.getStringValue(productItem, "quantity"));
                    storeOrderProductItem.put("addOptionPrice", JsonUtils.getStringValue(productItem, "addOptionPrice"));

                    Node productItemBaseOptionItemNode = nodeService.getNode("productOptionItem", JsonUtils.getStringValue(productItem, "addOptionItemId"));

                    Integer productItemBeforeQuantity = productItemBaseOptionItemNode.getIntValue("stockQuantity");

                    if (beforeQuantity > 0) {
                        Integer afterQuantity = productItemBeforeQuantity - JsonUtils.getIntValue(product, "quantity");

                        productItemBaseOptionItemNode.put("stockQuantity", afterQuantity);
                        nodeService.executeNode(productItemBaseOptionItemNode, "productOptionItem", CommonService.UPDATE);
                    }

                    nodeService.executeNode(storeOrderProductItem, "orderProductItem", CommonService.CREATE);
                }
//                orderProductIds.add(orderProductNode.getId());
            }
//            storeOrderDeliveryPrice.put("orderProductIds", StringUtils.join(orderProductIds, ","));
//            nodeService.executeNode(storeOrderDeliveryPrice, "orderDeliveryPrice", CommonService.CREATE);
        }

        totalOrderPrice = totalProductPrice - YPoint - welfarePoint + totalDeliveryPrice; //총 주문금액
        totalDiscountPrice = totalDiscountPrice + YPoint + welfarePoint;
        if (!StringUtils.equals(totalPaymentPrice.toString(), totalOrderPrice.toString())) {
            bSucc = "false";
            return bSucc;
        }
        Map<String, Object> storeOrderSheet = new HashMap<>();

        storeOrderSheet.put("orderSheetId", orderSheetId);                                          //주문서 번호
        storeOrderSheet.put("memberNo", memberNo);                                                  //회원번호
        storeOrderSheet.put("buyerName", memberName);                                               //구매자명
        storeOrderSheet.put("buyerTel", memberCellphone);                                           //구매자전화번호
        storeOrderSheet.put("cartId", JsonUtils.getIntValue(tempOrder.get(0), "cartId"));      //카트 아이디
        storeOrderSheet.put("siteId", JsonUtils.getStringValue(tempOrder.get(0), "siteId"));   //사이트 아이디
        storeOrderSheet.put("totalProductPrice", totalProductPrice);                                //총상품가격
        storeOrderSheet.put("totalOrderPrice", totalOrderPrice);                                    //총주문가격
        storeOrderSheet.put("totalDiscountPrice", totalDiscountPrice);                              //총할인액
        storeOrderSheet.put("totalDeliveryPrice", totalDeliveryPrice);                              //총배송비
        storeOrderSheet.put("totalPaymentPrice", totalPaymentPrice);                                //결제금액
        storeOrderSheet.put("couponDiscountPrice", couponDiscountPrice);                            //쿠폰 할인액
        storeOrderSheet.put("totalWelfarePoint", welfarePoint);                                //사용한 복지포인트
        storeOrderSheet.put("totalYPoint", YPoint);                                            //사용한 Y포인트
        storeOrderSheet.put("purchaseaAgreementYn", "y");
        storeOrderSheet.put("usePayMethod", JsonUtils.getStringValue(responseMap, "usePayMethod"));
        storeOrderSheet.put("usePayMethodName", JsonUtils.getStringValue(responseMap, "usePayMethodName"));
        storeOrderSheet.put("purchaseDeviceType", "");
        nodeService.executeNode(storeOrderSheet, "orderSheet", CommonService.CREATE);


        List<Map<String, Object>> orderProducts = nodeBindingService.list("orderProduct", "sorting=created&orderSheetId_equals=" + orderSheetId);
        List<Map<String, Object>> orderProductItems = nodeBindingService.list("orderProductItem", "sorting=created&orderSheetId_equals=" + orderSheetId);

        for (Map<String, Object> orderProduct : orderProducts) {
            Integer orderProductId = JsonUtils.getIntValue(orderProduct, "orderProductId");
            List<Map<String, Object>> subOrderProductItems = new ArrayList<>();
            for (Map<String, Object> orderProductItem : orderProductItems) {
                if (orderProductId.equals(JsonUtils.getIntValue(orderProductItem, "orderProductId"))) {
                    subOrderProductItems.add(orderProductItem);
                }
            }
            orderProduct.put("orderProductItem", subOrderProductItems);
        }

        List<Map<String, Object>> orderDeliveryProductList = deliveryService.makeDeliveryData(orderProducts, "order");
        Map<String, Object> orderDeliveryPriceList = deliveryService.calculateDeliveryPrice(orderDeliveryProductList, "order");

        createDelivery(responseMap, JsonUtils.getIntValue(session, "member.memberNo"));

        deliveryService.makeDeliveryPrice(orderSheetId, orderDeliveryPriceList);


        nodeBindingService.delete("tempOrder", orderSheetId);

        return bSucc;
    }

    /**
     * 결제 정보를 저장하는 Method.
     */
    public void createPayment(Map<String, Object> responseMap, String pgId) {

        responseMap.put("pgId", Integer.parseInt(pgId));
        nodeService.executeNode(responseMap, "payment", CommonService.CREATE);
    }


    /**
     * 주문서 배송지를 저장하는 Method.
     */
    public boolean createDelivery(Map<String, Object> responseMap, Integer memberNo) {

        boolean result = false;
        Map<String, Object> storeRefineDelivery = new HashMap<>();

        storeRefineDelivery.put("orderSheetId", responseMap.get("ordrIdxx"));
        storeRefineDelivery.put("addressName", responseMap.get("addressName"));
        storeRefineDelivery.put("address", responseMap.get("shippingAddress"));
        storeRefineDelivery.put("detailedAddress", responseMap.get("shippingDetailedAddress"));
        storeRefineDelivery.put("cellphone", responseMap.get("shippingCellPhone"));
        storeRefineDelivery.put("phone", responseMap.get("shippingPhone"));
        storeRefineDelivery.put("deliveryMemo", responseMap.get("deliveryMemo"));
        storeRefineDelivery.put("postCode", responseMap.get("postCode"));
        storeRefineDelivery.put("recipient", responseMap.get("recipient"));
        storeRefineDelivery.put("deliveryType", responseMap.get("deliveryType"));
        storeRefineDelivery.put("memberNo", memberNo);
        storeRefineDelivery.put("myDeliveryAddressId", JsonUtils.getStringValue(responseMap, "myDeliveryAddressId"));

        nodeService.executeNode(storeRefineDelivery, "delivery", CommonService.CREATE);


        if (responseMap.get("addMyDeliveryAddress").equals("on")) {       //주소록 추가

            Map<String, Object> storeMyDeliveryAddress = new HashMap<>();

            storeMyDeliveryAddress.put("memberNo", memberNo);
            storeMyDeliveryAddress.put("siteId", responseMap.get("siteId"));
            storeMyDeliveryAddress.put("addressName", responseMap.get("addressName"));
            storeMyDeliveryAddress.put("postCode", responseMap.get("postCode"));
            storeMyDeliveryAddress.put("address", responseMap.get("shippingAddress"));
            storeMyDeliveryAddress.put("detailedAddress", responseMap.get("shippingDetailedAddress"));
            storeMyDeliveryAddress.put("cellphone", responseMap.get("shippingCellPhone"));
            storeMyDeliveryAddress.put("phone", responseMap.get("shippingPhone"));
            storeMyDeliveryAddress.put("recipient", responseMap.get("recipient"));
            if (responseMap.get("changeDefaultAddress").equals("on")) {   //기본 배송지

                List<Node> myDeliveryAddressNodeList = nodeService.getNodeList("myDeliveryAddress", "defaultYn_matching=y");
                if (myDeliveryAddressNodeList.size() > 0) {
                    Node myDeliveryAddressNode = myDeliveryAddressNodeList.get(0);
                    myDeliveryAddressNode.put("defaultYn", "n");
                    nodeService.updateNode(myDeliveryAddressNode, "myDeliveryAddress");
                }

                storeMyDeliveryAddress.put("defaultYn", "y");

            } else {

                storeMyDeliveryAddress.put("defaultYn", "n");
            }
            nodeService.executeNode(storeMyDeliveryAddress, "myDeliveryAddress", CommonService.CREATE);

            result = true;

        } else {
            if (responseMap.get("changeDefaultAddress").equals("on")) {   //기본 배송지
                List<Node> myDefaultDeliveryAddressNodeList = nodeService.getNodeList("myDeliveryAddress", "defaultYn_matching=y");
                Node myDefaultDeliveryAddressNode = myDefaultDeliveryAddressNodeList.get(0);
                myDefaultDeliveryAddressNode.put("defaultYn", "n");
                nodeService.updateNode(myDefaultDeliveryAddressNode, "myDeliveryAddress");

                String myDeliveryAddressId = JsonUtils.getStringValue(responseMap, "myDeliveryAddressId");
                List<Node> myDeliveryAddressNodeList = nodeService.getNodeList("myDeliveryAddress", "myDeliveryAddressId_matching=".concat(myDeliveryAddressId));
                Node myDeliveryAddressNode = myDeliveryAddressNodeList.get(0);
                myDeliveryAddressNode.put("defaultYn", "y");
                nodeService.updateNode(myDeliveryAddressNode, "myDeliveryAddress");
                result = true;
            }
        }
        return result;
    }


    /**
     * PG return 데이터를 저장하는  Method.(리턴 값을 가공하지 JsonString 으로 저장, 일종의 Backup Data)
     */
    public String createPgResponse(Map<String, Object> responseMap) {
        Map<String, Object> storePg = new HashMap<>();

        String JsonString = JsonUtils.toJsonString(responseMap);
        String orderSheetId = String.valueOf(responseMap.get("ordrIdxx"));

        storePg.put("orderSheetId", orderSheetId);
        storePg.put("jsonResponse", JsonString);

        Node node = (Node) nodeService.executeNode(storePg, "pg", CommonService.CREATE);
        return node.getId();
    }

    /**
     * 주문서 생성 Method
     */

    private String createTempOrder(ExecuteContext context, boolean buyItNow) throws IOException {
        Map<String, Object> storeTempOrder = new HashMap<>();
        Map<String, Object> data = context.getData();
        Map<String, Object> session = sessionService.getSession(context.getHttpRequest());
        storeTempOrder.put("tempOrderId", orderNumberGenerator());
        storeTempOrder.put("memberNo", JsonUtils.getIntValue(session, "member.memberNo"));
        storeTempOrder.put("sessionId", sessionService.getSessionKey(context.getHttpRequest()));
        storeTempOrder.put("cartId", JsonUtils.getIntValue(data, "cartId"));
        storeTempOrder.put("siteId", JsonUtils.getStringValue(data, "siteId"));
        storeTempOrder.put("finishedYn", "n");

        if (buyItNow) {
            storeTempOrder.put("buyNowYn", "y");
        } else {
            storeTempOrder.put("buyNowYn", "n");
        }
        Node tempOrderNode = (Node) nodeService.executeNode(storeTempOrder, "tempOrder", CommonService.CREATE);
        String tempOrderId = tempOrderNode.getId();

        createTempOrderProduct(tempOrderId, data);

        return tempOrderId;
    }

    /**
     * 주문서 상품 생성 Method
     */
    private void createTempOrderProduct(String tempOrderId, Map<String, Object> data) {
        try {
            List<Map<String, Object>> productList = JsonUtils.parsingJsonToList(String.valueOf(data.get("product")));

            for (Map<String, Object> product : productList) {
                String productId = String.valueOf(JsonUtils.getValue(product, "productId"));
                String baseOptionItemId = String.valueOf(JsonUtils.getValue(product, "baseOptionItemId"));
                Integer quantity = (Integer) JsonUtils.getValue(product, "quantity");
                List<Map<String, Object>> productItemList = product.get("productItem") != null ? (List<Map<String, Object>>) product.get("productItem") : null;
                List<Map<String, Object>> storeProductItemList = new ArrayList<>();
                Map<String, Object> storeTempOrderProduct = new HashMap<>();

                Node productNode = nodeService.getNode("product", productId);
                Node productBaseOptionItemNode = nodeService.getNode("productOptionItem", String.valueOf(baseOptionItemId));

                double baseAddPrice = (double) productBaseOptionItemNode.get("addPrice");
                double salePrice = (double) productNode.getBindingValue("salePrice");
                double productPrice = baseAddPrice + salePrice;
                double totalAddOptionPrice = 0;

                storeTempOrderProduct.put("tempOrderId", tempOrderId);
                storeTempOrderProduct.put("productId", productId);
                storeTempOrderProduct.put("baseOptionItemId", baseOptionItemId);
                storeTempOrderProduct.put("quantity", quantity);
                storeTempOrderProduct.put("salePrice", salePrice);
                storeTempOrderProduct.put("vendorId", productNode.getBindingValue("vendorId"));


                if (productItemList != null) {
                    for (Map<String, Object> productItem : productItemList) {
                        Map<String, Object> storeTempOrderProductItem = new HashMap<>();

                        productId = String.valueOf(JsonUtils.getValue(productItem, "productId"));
                        String addOptionItemId = String.valueOf(JsonUtils.getValue(productItem, "addOptionItemId"));
                        quantity = (Integer) JsonUtils.getValue(productItem, "quantity");

                        Node productAddOptionItemNode = nodeService.getNode("productOptionItem", String.valueOf(addOptionItemId));

                        double addOptionPrice = (double) productAddOptionItemNode.get("addPrice");

                        storeTempOrderProductItem.put("tempOrderId", tempOrderId);
//                        storeTempOrderProductItem.put("tempOrderProductId", );
                        storeTempOrderProductItem.put("productId", productId);
                        storeTempOrderProductItem.put("addOptionItemId", addOptionItemId);
                        storeTempOrderProductItem.put("quantity", quantity);
                        storeTempOrderProductItem.put("addOptionPrice", addOptionPrice);

                        totalAddOptionPrice += addOptionPrice * quantity;

                        storeProductItemList.add(storeTempOrderProductItem);
                    }
                }

                storeTempOrderProduct.put("baseAddPrice", baseAddPrice);
                storeTempOrderProduct.put("productPrice", productPrice);
                storeTempOrderProduct.put("totalAddOptionPrice", totalAddOptionPrice);

                double orderPrice = (productPrice + totalAddOptionPrice) * quantity;

                storeTempOrderProduct.put("orderPrice", orderPrice);

                Node tempOrderProductNode = (Node) nodeService.executeNode(storeTempOrderProduct, "tempOrderProduct", CommonService.CREATE);

                for (Map<String, Object> storeTempOrderProductItem : storeProductItemList) {
                    storeTempOrderProductItem.put("tempOrderProductId", tempOrderProductNode.getId());
                    nodeService.executeNode(storeTempOrderProductItem, "tempOrderProductItem", CommonService.CREATE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * common_return.jsp
     * 주문서 업데이트 Method
     */
    public void accountTransferUpdate(Map<String, Object> responseMap) {
        String orderSheetId = JsonUtils.getStringValue(responseMap, "orderSheetId");
        if (!orderSheetId.equals("")) {
            List<Map<String, Object>> paymentList = nodeBindingService.list("payment", "orderSheetId_equals=".concat(orderSheetId));
            List<Map<String, Object>> orderProductList = nodeBindingService.list("orderProduct", "orderSheetId_equals=".concat(orderSheetId));
            Map<String, Object> paymentMap = paymentList.get(0);
            if (paymentMap != null) {
                paymentMap.putAll(responseMap);
                nodeService.executeNode(paymentMap, "payment", CommonService.UPDATE);
            }
            if (orderProductList.size() > 0) {
                for (Map<String, Object> orderProduct : orderProductList) {
                    orderProduct.put("orderStatus", "order003");
                    nodeService.executeNode(orderProduct, "orderProduct", CommonService.UPDATE);
                }
            }
        }
    }

    /**
     * 현금영수증 create Method
     */

    public void createCashReceipt(Map<String, Object> responseMap) {
        String orderSheetId = JsonUtils.getStringValue(responseMap, "orderSheetId");

        try {

            if (!StringUtils.equals(orderSheetId, "")) {
                Map<String, Object> orderSheetData = nodeBindingService.read("orderSheet", orderSheetId);

                Map<String, Object> storeCashReceiptMap = new HashMap<>();

                storeCashReceiptMap.putAll(responseMap);
                storeCashReceiptMap.put("appTime", JsonUtils.getDateValue(responseMap, "appTime"));
                storeCashReceiptMap.put("memberNo", JsonUtils.getIntNullableValue(orderSheetData, "memberNo"));
                storeCashReceiptMap.put("orderCreateDate", JsonUtils.getDateValue(orderSheetData, "created"));
                storeCashReceiptMap.put("amount", JsonUtils.getDoubleValue(orderSheetData, "totalOrderPrice"));

                nodeService.executeNode(storeCashReceiptMap, "cashReceipt", CommonService.CREATE);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 쿠폰 중복 체크 Method.
     */
    private boolean repetitionCheck(Collection<Object> ids) {
        List<Integer> idList = new ArrayList(ids);
        boolean duplication = false;
        for (int i = 0; i < idList.size(); i++) {
            for (int j = 1; j < idList.size(); j++) {
                if (idList.get(i) == idList.get(j)) {
                    duplication = true;
                }
            }
            if (duplication) {
                break;
            }
        }
        return duplication;
    }

    /**
     * 포인트 차감 Method.
     */
    private boolean pointsDeduction(String memberNo) {
        return false;
    }

    /**
     * 주문번호 생성 Method.
     */
    private String orderNumberGenerator() {
        Random random = new Random();

        String appendNumber1 = String.valueOf(random.nextInt(10));
        String appendNumber2 = String.valueOf(random.nextInt(10));
        String appendNumber3 = String.valueOf(random.nextInt(10));

        String orderNumber = "B";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.KOREA);
        Date currentTime = new Date();
        String time = simpleDateFormat.format(currentTime);

        orderNumber = orderNumber.concat(time).concat(appendNumber1).concat(appendNumber2).concat(appendNumber3);

        return orderNumber;
    }

    private void pointDeduction(String yPoin) {

    }

    /**
     * 주문 성공시 임시 주문서를 삭제하는 Method.
     * 일정 시간이 지난 주문서도 삭제 한다.
     */
    public void removeTempOrder() {

    }

}
