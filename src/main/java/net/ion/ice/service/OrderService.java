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
import org.springframework.core.env.Environment;
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
    private Environment environment;

    /**
     * 임시 주문서 조회
     */
    public ExecuteContext tempOrderRead(ExecuteContext context) throws IOException {
        Integer totalSize = 0;
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
            for (Map<String, Object> priceProduct : priceList) {
                subProductResult.add(priceProduct);
            }

            itemResult.put("item", subProductResult);
            totalSize += subProductResult.size();
            items.add(itemResult);
        }

        queryResult.put("length", totalSize);
        queryResult.put("items", items);
        context.setResult(queryResult);
        return context;
    }

    /**
     * 바로 주문 저장
     */
    public void buyItNow(ExecuteContext context) {
        try {
            String tempOrderId = createTempOrder(context.getData());
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
            String tempOrderId = createTempOrder(context.getData());
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

            for(Map<String, Object> tempOrderProduct : tempOrderProducts){
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
            Map<String, Object> summaryResponse = getSummary((String) data.get("memberNo"));

            double useableYPoint = ((BigDecimal) summaryResponse.get("useableYPoint")).doubleValue();
            double useableWelfarepoint = ((BigDecimal) summaryResponse.get("useableWelfarepoint")).doubleValue();
            double useYPoint = JsonUtils.getDoubleValue(data, "useYPoint");
            double useWelfarepoint = JsonUtils.getDoubleValue(data, "useWelfarepoint");
            double deliveryPrice = JsonUtils.getDoubleValue(data, "deliveryPrice");

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

            for(Map<String, Object> tempOrderProduct : tempOrderProducts){
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
                deliveryPrice += Double.parseDouble(String.valueOf(priceList.get(0).get("deliveryPrice")));
            }


            totalPrice = totalPrice - useYPoint - useWelfarepoint + deliveryPrice;

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
    public ExecuteContext pointOrder(ExecuteContext context){
        Map<String, Object> data = context.getData();

        List<Map<String, Object>> tempOrderProducts = nodeBindingService.list("tempOrderProduct", "sorting=created&tempOrderId_equals=" + String.valueOf(data.get("ordrIdxx")));
        List<Map<String, Object>> tempOrderProductItems = nodeBindingService.list("tempOrderProductItem", "sorting=created&tempOrderId_equals=" + String.valueOf(data.get("ordrIdxx")));
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
        double totalProductPrice = 0;
        double totalDeliveryPrice = 0;
        double totalDiscountPrice = 0;
        double totalOrderPrice = 0;
        double couponDiscountPrice = 0;
        double totalPaymentPrice = JsonUtils.getDoubleValue(data, "finalPrice");

        Map<String, Object> summaryResponse = getSummary(JsonUtils.getStringValue(session, "member.memberNo"));

        double useableYPoint = ((BigDecimal) summaryResponse.get("useableYPoint")).doubleValue();
        double useableWelfarepoint = ((BigDecimal) summaryResponse.get("useableWelfarepoint")).doubleValue();

        double useYPoint = JsonUtils.getDoubleValue(data, "useYPoint");
        double useWelfarepoint = JsonUtils.getDoubleValue(data, "useWelfarepoint");

        if (useYPoint > useableYPoint && useWelfarepoint > useableWelfarepoint) {
            context.setResult(CommonService.getResult("O0006"));
        }


        List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(tempOrderProducts, "tempOrder");
        Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "tempOrder");

        for (String key : deliveryPriceList.keySet()) {
            QueryResult itemResult = new QueryResult();
            itemResult.put("deliverySeq", key);
            List<Map<String, Object>> priceList = (List<Map<String, Object>>) deliveryPriceList.get(key);
            itemResult.put("deliveryPrice", priceList.get(0).get("deliveryPrice"));
            totalDeliveryPrice += Double.parseDouble(String.valueOf(priceList.get(0).get("deliveryPrice")));

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

        for (Map<String, Object> deliveryItem : items) {

            totalDeliveryPrice += JsonUtils.getDoubleValue(deliveryItem, "deliveryPrice");

            for (Map<String, Object> product : (List<Map<String, Object>>) deliveryItem.get("item")) {
                Map<String, Object> storeOrderProduct = new HashMap<>();

                ///////orderProduct////////
                storeOrderProduct.put("orderSheetId", JsonUtils.getStringValue(data, "ordrIdxx"));
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
                Integer memberNo = JsonUtils.getIntValue(session, "member.memberNo");

                Map<String, Double> discountPriceMap = couponService.productCouponDiscountPrice(Integer.parseInt(tempOrderProductId), couponId, "", memberNo);
                storeOrderProduct.put("couponDiscountPrice", discountPriceMap.get("resultDiscountPrice"));
                storeOrderProduct.put("vendorId", JsonUtils.getIntValue(product, "vendorId"));
                storeOrderProduct.put("orderStatus", "order003");
                storeOrderProduct.put("paymentPrice", discountPriceMap.get("resultOrderPrice"));

                totalProductPrice += discountPriceMap.get("resultOrderPrice");

                ///////orderDeliveryPrice////////
//                storeOrderDeliveryPrice.put("deliveryMethod", JsonUtils.getValue(product, "product.deliveryMethod"));
//                storeOrderDeliveryPrice.put("bundleDeliveryYn", JsonUtils.getValue(product, "product.bundleDeliveryYn"));
//                storeOrderDeliveryPrice.put("deliveryPriceType", JsonUtils.getValue(product, "product.deliveryPriceType"));
//                storeOrderDeliveryPrice.put("vendorId", JsonUtils.getIntValue(product, "vendorId"));
//
                Node orderProductNode = (Node) nodeService.executeNode(storeOrderProduct, "orderProduct", CommonService.CREATE);

                for (Map<String, Object> productItem : (List<Map<String, Object>>) product.get("tempOrderProductItem")) {
                    Map<String, Object> storeOrderProductItem = new HashMap<>();
                    storeOrderProductItem.put("orderSheetId", JsonUtils.getStringValue(data, "ordrIdxx"));
                    storeOrderProductItem.put("orderProductId", orderProductNode.getId());
                    storeOrderProductItem.put("productId", JsonUtils.getStringValue(productItem, "productId"));
                    storeOrderProductItem.put("addOptionItemId", JsonUtils.getStringValue(productItem, "addOptionItemId"));
                    storeOrderProductItem.put("addOptionItemName", "");
                    storeOrderProductItem.put("quantity", JsonUtils.getStringValue(productItem, "quantity"));
                    storeOrderProductItem.put("addOptionPrice", JsonUtils.getStringValue(productItem, "addOptionPrice"));
                    nodeService.executeNode(storeOrderProductItem, "orderProductItem", CommonService.CREATE);
                }
//                orderProductIds.add(orderProductNode.getId());
            }
//            storeOrderDeliveryPrice.put("orderProductIds", StringUtils.join(orderProductIds, ","));
//            nodeService.executeNode(storeOrderDeliveryPrice, "orderDeliveryPrice", CommonService.CREATE);
        }

        totalOrderPrice = totalProductPrice - useYPoint - useWelfarepoint + totalDeliveryPrice; //총 주문금액

        if(totalPaymentPrice != totalProductPrice){
            context.setResult(CommonService.getResult("O0006"));
            return context;
        }

        Map<String, Object> storeOrderSheet = new HashMap<>();

        storeOrderSheet.put("orderSheetId", data.get("ordrIdxx"));   //주문서 번호
        storeOrderSheet.put("cartId", "");              //카트 아이디
        storeOrderSheet.put("memberNo", "");          //회원번호
        storeOrderSheet.put("siteId", "");              //사이트 아이디
        storeOrderSheet.put("totalProductPrice", totalProductPrice);         //총상품가격
        storeOrderSheet.put("totalOrderPrice", totalOrderPrice);             //총주문가격
        storeOrderSheet.put("totalDiscountPrice", totalDiscountPrice);       //총할인액
        storeOrderSheet.put("totalDeliveryPrice", totalDeliveryPrice);       //총배송비
        storeOrderSheet.put("totalPaymentPrice", totalPaymentPrice);         //결제금액
        storeOrderSheet.put("couponDiscountPrice", couponDiscountPrice);     //쿠폰 할인액
        storeOrderSheet.put("totalWelfarePoint", useYPoint);         //사용한 복지포인트
        storeOrderSheet.put("totalYPoint", useWelfarepoint);                     //사용한 Y포인트
        storeOrderSheet.put("purchaseaAgreementYn", "y");
        storeOrderSheet.put("purchaseDeviceType", "");
        nodeService.executeNode(storeOrderSheet, "orderSheet", CommonService.CREATE);


        List<Map<String, Object>> orderProducts = nodeBindingService.list("orderProduct", "sorting=created&orderSheetId_equals=" + String.valueOf(data.get("ordrIdxx")));
        List<Map<String, Object>> orderProductItems = nodeBindingService.list("orderProductItem", "sorting=created&orderSheetId_equals=" + String.valueOf(data.get("ordrIdxx")));

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


        deliveryService.makeDeliveryPrice(String.valueOf(data.get("ordrIdxx")), orderDeliveryPriceList);

        context.setResult(CommonService.getResult("O0005"));

        return context;



    }
    public String createOrderSheet(Map<String, Object> responseMap, HttpServletRequest request) {
        String bSucc = "true";
        List<Map<String, Object>> tempOrderProducts = nodeBindingService.list("tempOrderProduct", "sorting=created&tempOrderId_equals=" + String.valueOf(responseMap.get("ordrIdxx")));
        List<Map<String, Object>> tempOrderProductItems = nodeBindingService.list("tempOrderProductItem", "sorting=created&tempOrderId_equals=" + String.valueOf(responseMap.get("ordrIdxx")));
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
        double totalProductPrice = 0;
        double totalDeliveryPrice = 0;
        double totalDiscountPrice = 0;
        double totalOrderPrice = 0;
        double couponDiscountPrice = 0;
        double totalPaymentPrice = Double.parseDouble(String.valueOf(responseMap.get("amount")));
        double totalWelfarePoint = Double.parseDouble(String.valueOf(responseMap.get("useWelfarepoint")));
        double totalYPoint = Double.parseDouble(String.valueOf(responseMap.get("useYPoint")));

        List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(tempOrderProducts, "tempOrder");
        Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "tempOrder");

        for (String key : deliveryPriceList.keySet()) {
            QueryResult itemResult = new QueryResult();
            itemResult.put("deliverySeq", key);
            List<Map<String, Object>> priceList = (List<Map<String, Object>>) deliveryPriceList.get(key);
            itemResult.put("deliveryPrice", priceList.get(0).get("deliveryPrice"));
            totalDeliveryPrice += Double.parseDouble(String.valueOf(priceList.get(0).get("deliveryPrice")));

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
                storeOrderProduct.put("orderSheetId", JsonUtils.getStringValue(responseMap, "ordrIdxx"));
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
                Integer memberNo = JsonUtils.getIntValue(session, "member.memberNo");

                Map<String, Double> discountPriceMap = couponService.productCouponDiscountPrice(Integer.parseInt(tempOrderProductId), couponId, "", memberNo);
                storeOrderProduct.put("couponDiscountPrice", discountPriceMap.get("resultDiscountPrice"));
                storeOrderProduct.put("vendorId", JsonUtils.getIntValue(product, "vendorId"));
                storeOrderProduct.put("paymentPrice", discountPriceMap.get("resultOrderPrice"));
                storeOrderProduct.put("orderStatus", JsonUtils.getStringValue(responseMap, "orderStatus"));
                totalProductPrice += discountPriceMap.get("resultOrderPrice");

                ///////orderDeliveryPrice////////
//                storeOrderDeliveryPrice.put("deliveryMethod", JsonUtils.getValue(product, "product.deliveryMethod"));
//                storeOrderDeliveryPrice.put("bundleDeliveryYn", JsonUtils.getValue(product, "product.bundleDeliveryYn"));
//                storeOrderDeliveryPrice.put("deliveryPriceType", JsonUtils.getValue(product, "product.deliveryPriceType"));
//                storeOrderDeliveryPrice.put("vendorId", JsonUtils.getIntValue(product, "vendorId"));
//
                Node orderProductNode = (Node) nodeService.executeNode(storeOrderProduct, "orderProduct", CommonService.CREATE);

                for (Map<String, Object> productItem : (List<Map<String, Object>>) product.get("tempOrderProductItem")) {
                    Map<String, Object> storeOrderProductItem = new HashMap<>();
                    storeOrderProductItem.put("orderSheetId", JsonUtils.getStringValue(responseMap, "ordrIdxx"));
                    storeOrderProductItem.put("orderProductId", orderProductNode.getId());
                    storeOrderProductItem.put("productId", JsonUtils.getStringValue(productItem, "productId"));
                    storeOrderProductItem.put("addOptionItemId", JsonUtils.getStringValue(productItem, "addOptionItemId"));
                    storeOrderProductItem.put("addOptionItemName", "");
                    storeOrderProductItem.put("quantity", JsonUtils.getStringValue(productItem, "quantity"));
                    storeOrderProductItem.put("addOptionPrice", JsonUtils.getStringValue(productItem, "addOptionPrice"));
                    nodeService.executeNode(storeOrderProductItem, "orderProductItem", CommonService.CREATE);
                }
//                orderProductIds.add(orderProductNode.getId());
            }
//            storeOrderDeliveryPrice.put("orderProductIds", StringUtils.join(orderProductIds, ","));
//            nodeService.executeNode(storeOrderDeliveryPrice, "orderDeliveryPrice", CommonService.CREATE);
        }

        totalOrderPrice = totalProductPrice - totalYPoint - totalWelfarePoint + totalDeliveryPrice; //총 주문금액

        Map<String, Object> storeOrderSheet = new HashMap<>();

        storeOrderSheet.put("orderSheetId", responseMap.get("ordrIdxx"));   //주문서 번호
        storeOrderSheet.put("cartId", "");              //카트 아이디
        storeOrderSheet.put("memberNo", "");          //회원번호
        storeOrderSheet.put("siteId", "");              //사이트 아이디
        storeOrderSheet.put("totalProductPrice", totalProductPrice);         //총상품가격
        storeOrderSheet.put("totalOrderPrice", totalOrderPrice);             //총주문가격
        storeOrderSheet.put("totalDiscountPrice", totalDiscountPrice);       //총할인액
        storeOrderSheet.put("totalDeliveryPrice", totalDeliveryPrice);       //총배송비
        storeOrderSheet.put("totalPaymentPrice", totalPaymentPrice);         //결제금액
        storeOrderSheet.put("couponDiscountPrice", couponDiscountPrice);     //쿠폰 할인액
        storeOrderSheet.put("totalWelfarePoint", totalWelfarePoint);         //사용한 복지포인트
        storeOrderSheet.put("totalYPoint", totalYPoint);                     //사용한 Y포인트
        storeOrderSheet.put("purchaseaAgreementYn", "y");
        storeOrderSheet.put("purchaseDeviceType", "");
        nodeService.executeNode(storeOrderSheet, "orderSheet", CommonService.CREATE);


        List<Map<String, Object>> orderProducts = nodeBindingService.list("orderProduct", "sorting=created&orderSheetId_equals=" + String.valueOf(responseMap.get("ordrIdxx")));
        List<Map<String, Object>> orderProductItems = nodeBindingService.list("orderProductItem", "sorting=created&orderSheetId_equals=" + String.valueOf(responseMap.get("ordrIdxx")));

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


        deliveryService.makeDeliveryPrice(String.valueOf(responseMap.get("ordrIdxx")), orderDeliveryPriceList);

        return bSucc;
    }


    /**
     * 결제 후 최종적으로 한번 더 검증하여 주문서를 생성하는 Method.
     */
//    public String createOrderSheet(Map<String, Object> responseMap) {
//        String bSucc;
//
//        List<Map<String, Object>> tempOrderProducts = nodeBindingService.list("tempOrderProduct", "sorting=created&tempOrderId_equals=" + String.valueOf(responseMap.get("ordrIdxx")));
//        Map<String, Object> tempOrder = nodeBindingService.getNodeBindingInfo("tempOrder").retrieve(String.valueOf(responseMap.get("ordrIdxx")));
//        List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(tempOrderProducts, "tempOrder");
//        Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "tempOrder");
//
//        List<QueryResult> items = new ArrayList<>();
//        double totalProductPrice = 0;
//        double totalDeliveryPrice = 0;
//        double totalDiscountPrice = 0;
//        double totalOrderPrice = 0;
//        double couponDiscountPrice = 0;
//        double totalPaymentPrice = Double.parseDouble(String.valueOf(responseMap.get("amount")));
//        double totalWelfarePoint = Double.parseDouble(String.valueOf(responseMap.get("useWelfarepoint")));
//        double totalYPoint = Double.parseDouble(String.valueOf(responseMap.get("useYPoint")));
//
//        for (String key : deliveryPriceList.keySet()) {
//            QueryResult itemResult = new QueryResult();
//            itemResult.put("deliverySeq", key);
//            List<Map<String, Object>> priceList = (List<Map<String, Object>>) deliveryPriceList.get(key);
//
//            totalDeliveryPrice += Double.parseDouble(String.valueOf(priceList.get(0).get("deliveryPrice")));
//
//            List<Map<String, Object>> subProductResult = new ArrayList<>();
//            for (Map<String, Object> priceProduct : priceList) {
//                subProductResult.add(priceProduct);
//            }
//
//            itemResult.put("item", subProductResult);
//            items.add(itemResult);
//        }
//
//
//        Map<String, Object> summaryResponse = getSummary((String) responseMap.get("memberNo"));
//
//        double useableYPoint = (double) ((Map<String, Object>) summaryResponse.get("item")).get("useableYPoint");
//        double useableWelfarepoint = (double) ((Map<String, Object>) summaryResponse.get("item")).get("useableWelfarepoint");
//
//
//        /**
//         * 사용자 포인트를 조회하여 사용 포인트와 체크한다.
//         * 사용 포인트 > 보유 포인트 시 bSucc = "true"
//         * 사용 포인트 > 보유 포인트 시 bSucc = "false"
//         * */
//        if (totalYPoint > useableYPoint && totalWelfarePoint > useableWelfarepoint) {
//            bSucc = "false";
//            return bSucc;
//        }
//
//        Map<String, Object> couponIds = null;
//        try {
//            couponIds = JsonUtils.parsingJsonToMap((String) responseMap.get("usedCoupon"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        List<Map<String, Object>> couponResponseItems = getCoupons((String) responseMap.get("memberNo"), (String) responseMap.get("ordrIdxx")); //상품 정보
//
//        boolean duplicated = repetitionCheck(couponIds.values()); // 쿠폰 아이디 중복 체크
//
//        /**
//         * productPrice - couponDiscountPrice 가격을 모두 더하여 totalProductPrice 값을 만든다.
//         * orderProduct 생성
//         * */
//        for (Map<String, Object> item : couponResponseItems) {
//            double orderPrice = (double) item.get("orderPrice");
//
//            Map<String, Object> storeOrderProduct = new HashMap<>();
//
//            storeOrderProduct.put("orderSheetId", responseMap.get("ordrIdxx"));                                     //주문서 아이디
//            storeOrderProduct.put("productId", JsonUtils.getValue(item, "productId.value"));                   //상품 아이디
//            storeOrderProduct.put("baseOptionItemId", JsonUtils.getValue(item, "baseOptionItemId.value"));     //기본옵션 아이템 아이디
//            storeOrderProduct.put("baseOptionItemName", JsonUtils.getValue(item, "baseOptionItemId.label"));   //기본옵션 아이템명
//            storeOrderProduct.put("quantity", item.get("quantity"));                                                //수량
//            storeOrderProduct.put("salePrice", item.get("salePrice"));                                              //판매가
//            storeOrderProduct.put("baseAddPrice", item.get("baseAddPrice"));                                        //기본옵션추가금액
//            storeOrderProduct.put("productPrice", item.get("productPrice"));                                        //상품금액
//            storeOrderProduct.put("totalAddOptionPrice", item.get("totalAddOptionPrice"));                          //추가옵션금액
//            storeOrderProduct.put("orderPrice", item.get("orderPrice"));                                            //주문금액
//            storeOrderProduct.put("orderStatus", responseMap.get("orderStatus"));                                   //주문상태
//            storeOrderProduct.put("vendorId", item.get("vendorId"));                                                //벤더사 아이디
//            storeOrderProduct.put("purchasePhoneNo", "");                                                           //유가증권 구매 전화번호
//
//
//            String couponId = String.valueOf(couponIds.get("tempOrderProductId"));
//
//            List<Map<String, Object>> applicableCoupons = (List<Map<String, Object>>) item.get("applicableCoupons");
//            for (Map<String, Object> applicableCoupon : applicableCoupons) {
//                if (couponId.equals(applicableCoupon.get("couponId"))) {
//                    storeOrderProduct.put("couponId", applicableCoupon.get("couponId"));                     //쿠폰 아이디
//                    storeOrderProduct.put("discountPrice", applicableCoupon.get("discountPrice"));           //쿠폰 할인금액
//
//                    orderPrice = orderPrice - (double) applicableCoupon.get("discountPrice");
//                    couponDiscountPrice = couponDiscountPrice + (double) applicableCoupon.get("discountPrice");
//                }
//            }
//            Node orderProductNode = (Node) nodeService.executeNode(storeOrderProduct, "orderProduct", CommonService.CREATE);
//
//            orderProductIds.add(orderProductNode.getId()); //orderDeliveryPrice | orderProductIds 에 넣기 위하여.
//            /**
//             * orderProductItem 생성
//             * */
//
//            List<Map<String, Object>> tempOrderProductItemList = nodeBindingService.list("tempOrderProductItem", "tempOrderProductId_in=".concat(String.valueOf(item.get("tempOrderProductId"))));
//            for (Map<String, Object> tempOrderProductItem : tempOrderProductItemList) {
//                Map<String, Object> storeOrderProductItem = new HashMap<>();
//
//                storeOrderProductItem.put("orderSheetId", responseMap.get("ordrIdxx"));
//                storeOrderProductItem.put("orderProductId", orderProductNode.getId());
//                storeOrderProductItem.put("productId", tempOrderProductItem.get("productId"));
//                storeOrderProductItem.put("addOptionItemId", tempOrderProductItem.get("addOptionItemId"));
////                storeOrderProductItem.put("addOptionItemName", tempOrderProductItem.get(""));
//                storeOrderProductItem.put("quantity", tempOrderProductItem.get("quantity"));
//                storeOrderProductItem.put("addOptionPrice", tempOrderProductItem.get("addOptionPrice"));
//
//                nodeService.executeNode(storeOrderProductItem, "orderProductItem", CommonService.CREATE);
//            }
//            totalProductPrice = totalProductPrice + orderPrice;
//        }
//        totalDiscountPrice = totalYPoint + totalWelfarePoint + couponDiscountPrice; // 총 할인액
//        totalOrderPrice = totalProductPrice - totalYPoint - totalWelfarePoint + totalDeliveryPrice; //총 주문금액
//
//        /**
//         * 최종으로 totalOrderPrice 와 totalPaymentPrice 을 체크 및 쿠폰 중복 체크.
//         * 성공 시, bSucc = "true"
//         * 실패 시, bSucc = "false"
//         * */
//
//
////        List<Map<String, Object>> tempOrderProductList = nodeBindingService.list("tempOrderProduct", "tempOrderId_in=" + tempOrder.get("tempOrderId"));
////
////        for (Map<String, Object> tempOrderProduct : tempOrderProductList) {
////            totalOrderPrice += (double) tempOrderProduct.get("orderPrice");
////        }
//
//
////        orderSheet.put("sessionId", tempOrder.get("sessionId"));
//        storeOrderSheet.put("orderSheetId", responseMap.get("ordrIdxx"));   //주문서 번호
//        storeOrderSheet.put("cartId", tempOrder.get("cartId"));              //카트 아이디
//        storeOrderSheet.put("memberNo", tempOrder.get("memberNo"));          //회원번호
//        storeOrderSheet.put("siteId", tempOrder.get("siteId"));              //사이트 아이디
//        storeOrderSheet.put("totalProductPrice", totalProductPrice);         //총상품가격
//        storeOrderSheet.put("totalOrderPrice", totalOrderPrice);             //총주문가격
//        storeOrderSheet.put("totalDiscountPrice", totalDiscountPrice);       //총할인액
//        storeOrderSheet.put("totalDeliveryPrice", totalDeliveryPrice);       //총배송비
//        storeOrderSheet.put("totalPaymentPrice", totalPaymentPrice);         //결제금액
//        storeOrderSheet.put("couponDiscountPrice", couponDiscountPrice);     //쿠폰 할인액
//        storeOrderSheet.put("totalWelfarePoint", totalWelfarePoint);         //사용한 복지포인트
//        storeOrderSheet.put("totalYPoint", totalYPoint);                     //사용한 Y포인트
//        storeOrderSheet.put("purchaseaAgreementYn", "y");
//        storeOrderSheet.put("purchaseDeviceType", tempOrder.get(""));
//
//        nodeService.executeNode(storeOrderSheet, "orderSheet", CommonService.CREATE);
//
//        storeOrderDeliveryPrice.put("orderSheetId", responseMap.get("ordrIdxx"));
//        storeOrderDeliveryPrice.put("orderProductIds", StringUtils.join(orderProductIds, ","));
//        storeOrderDeliveryPrice.put("vendorId", StringUtils.join(orderProductIds, ","));
//
//        boolean saveDelivery = createDelivery(responseMap); // 배송지 저장
//
//        if (totalOrderPrice == totalPaymentPrice && !duplicated && saveDelivery) {
//            bSucc = "true";
//        } else {
//            bSucc = "false";
//        }
//        return bSucc;
//    }

    /**
     * 결제 정보를 저장하는 Method.
     */
    public String createPayment(Map<String, Object> responseMap) {
        Node node = (Node) nodeService.executeNode(responseMap, "payment", CommonService.CREATE);
        return node.getId();
    }


    /**
     * 주문서 배송지를 저장하는 Method.
     */
    public boolean createDelivery(Map<String, Object> responseMap) {

        boolean result = false;
        Map<String, Object> storeRefineDelivery = new HashMap<>();

        String address = String.valueOf(responseMap.get("shippingAddress")).concat(" ").concat(String.valueOf(responseMap.get("shippingDetailedAddress")));

        storeRefineDelivery.put("orderSheetId", responseMap.get("ordrIdxx"));
        storeRefineDelivery.put("addressName", responseMap.get("addressName"));
        storeRefineDelivery.put("address", address);
        storeRefineDelivery.put("cellphone", responseMap.get("shippingCellPhone"));
        storeRefineDelivery.put("phone", responseMap.get("shippingPhone"));
        storeRefineDelivery.put("deliveryMemo", responseMap.get("deliveryMemo"));
        storeRefineDelivery.put("postCode", responseMap.get("postCode"));
        storeRefineDelivery.put("recipient", responseMap.get("recipient"));
        storeRefineDelivery.put("deliveryType", responseMap.get("deliveryType"));

        nodeService.executeNode(storeRefineDelivery, "delivery", CommonService.CREATE);

        String myDeliveryAddressId = String.valueOf(responseMap.get("myDeliveryAddressId"));

        if (responseMap.get("addMyDeliveryAddress").equals("on")) {       //주소록 추가

            Map<String, Object> storeMyDeliveryAddress = new HashMap<>();

            storeMyDeliveryAddress.put("memberNo", responseMap.get("memberNo"));
            storeMyDeliveryAddress.put("siteId", responseMap.get("siteId"));
            storeMyDeliveryAddress.put("addressName", responseMap.get("addressName"));
            storeMyDeliveryAddress.put("postCode", responseMap.get("postCode"));
            storeMyDeliveryAddress.put("address", responseMap.get("shippingAddress"));
            storeMyDeliveryAddress.put("detailedAddress", responseMap.get("shippingDetailedAddress"));
            storeMyDeliveryAddress.put("cellphone", responseMap.get("shippingCellPhone"));
            storeMyDeliveryAddress.put("phone", responseMap.get("phone"));
            if (responseMap.get("changeDefaultAddress").equals("on")) {   //기본 배송지

                List<Node> myDeliveryAddressNodeList = nodeService.getNodeList("myDeliveryAddress", "defaultYn_matching=y");
                Node myDeliveryAddressNode = myDeliveryAddressNodeList.get(0);
                myDeliveryAddressNode.put("defaultYn", "n");
                nodeService.updateNode(myDeliveryAddressNode, "myDeliveryAddress");

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
    public void createPgResponse(Map<String, Object> responseMap, String paymentId) {
        Map<String, Object> storePg = new HashMap<>();

        String JsonString = JsonUtils.toJsonString(responseMap);
        String orderSheetId = String.valueOf(responseMap.get("ordrIdxx"));

        storePg.put("paymentId", paymentId);
        storePg.put("orderSheetId", orderSheetId);
        storePg.put("jsonResponse", JsonString);

        nodeService.executeNode(storePg, "pg", CommonService.CREATE);

    }

    /**
     * 주문서 생성 Method
     */

    private String createTempOrder(Map<String, Object> data) throws IOException {

        Map<String, Object> storeTempOrder = new HashMap<>();
        storeTempOrder.put("tempOrderId", orderNumberGenerator());
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

                double orderPrice = productPrice + totalAddOptionPrice;

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
