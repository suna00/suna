package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeQuery;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.session.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service("couponService")
public class CouponService {

    @Autowired
    private NodeService nodeService;
    @Autowired
    NodeBindingService nodeBindingService;
    @Autowired
    SessionService sessionService;

    CommonService common;


    public ExecuteContext download(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(common.PATTERN);
        LocalDateTime now = LocalDateTime.now();

        String[] params = {"memberNo", "couponTypeId"};
        if (common.requiredParams(context, data, params)) return context;

        Node couponType = nodeService.getNode("couponType", data.get("couponTypeId").toString());
        if (couponType == null) {
            common.setErrorMessage(context, "V0001");
            return context;
        }

        List<Node> list = nodeService.getNodeList("coupon", "memberNo_matching=" + data.get("memberNo") + "&couponTypeId_matching=" + data.get("couponTypeId"));
        if (list.size() > 0) {
            common.setErrorMessage(context, "V0005");
        }

        if ("createPeriodType>limit".equals(couponType.getValue("createPeriodType"))) {
            LocalDateTime start = LocalDateTime.parse(couponType.getValue("createStartDate").toString(), formatter);
            LocalDateTime end = LocalDateTime.parse(couponType.getValue("createEndDate").toString(), formatter);

            if (!(start.isBefore(now) && end.isAfter(now))) {
                common.setErrorMessage(context, "V0004");
                return context;
            }
        }

        if ("limitYn>limit".equals(couponType.getValue("limitedQuantityType")) && "0".equals(couponType.getValue("remainingQuantity"))) {
            common.setErrorMessage(context, "V0002");
            return context;
        }

/*
        동일인 재발급
        회원후기작성, 구매수량 충족 에만 해당
*/
//        if("limitYn>limit".equals(couponType.getValue("samePersonQuantityType"))){
//            if(list.size() > 0){
//                int count = (int) couponType.getValue("samePersonQuantity");
//                if(list.size() >= count){
//                    common.setErrorMessage(context, "V0003");
//                    return context;
//                }
//            }
//        }

        data.putAll(couponType);

        String endDate = common.unlimitedDate;
        if ("limitYn>limit".equals(couponType.getValue("validePeriodType"))) {
            LocalDateTime after = now.plusDays(Integer.parseInt(couponType.getValue("validePeriod").toString()));
            endDate = after.format(formatter);
        }

        data.put("publishedDate", now.format(formatter));
        data.put("startDate", now.format(formatter));
        data.put("endDate", endDate);
        data.put("couponStatus", "n");

        Object result = nodeService.executeNode(data, "coupon", common.SAVE);
        context.setResult(result);

        if ("limitYn>limit".equals(couponType.getValue("limitedQuantityType"))) {
            couponType.put("remainingQuantity", Integer.parseInt(couponType.getValue("remainingQuantity").toString()) - 1);
            nodeService.executeNode(couponType, "couponType", common.UPDATE);
        }

        return context;
    }

    public ExecuteContext applicableCouponList(ExecuteContext context) {
        Map<String, Object> contextData = context.getData();
        Map<String, Object> sessionData = null;
        QueryResult queryResult = new QueryResult();

        try {
            sessionData = sessionService.getSession(context.getHttpRequest());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String typeId = JsonUtils.getStringValue(contextData, "typeId");                       //cart or tempOrder
        NodeType nodeType = nodeService.getNodeType(typeId);
        String targetProductTypeId = nodeType.getTypeId().concat("Product");           //cartProduct or tempOrderProduct
        String id = nodeType.getIdablePIds().get(0);
        String idValue = JsonUtils.getStringValue(contextData, "id");

        List<Map<String, Object>> targetProductList = nodeBindingService.list(targetProductTypeId, "sorting=created&".concat(id).concat("_equals=").concat(idValue));
        Node memberNode = (Node) sessionData.get("member");
        String memberNo = memberNode.getStringValue("memberNo");
        String siteType = String.valueOf(memberNode.getBindingValue("siteType"));
        String now = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
        List<Node> couponList = (List<Node>) NodeQuery.build("coupon").matching("memberNo", memberNo).matching("couponStatus", "n").matching("siteType", "all,".concat(siteType)).above("endDate", now).getList();

        for (Map<String, Object> targetProduct : targetProductList) {
            String productId = JsonUtils.getStringValue(targetProduct, "productId");
            Double orderPrice = JsonUtils.getDoubleValue(targetProduct, "orderPrice");
            Map<String, Object> productInfo =  getProductInfo(targetProduct);
            targetProduct.put("productName", productInfo.get("productName"));
            targetProduct.put("baseOptionItemName", productInfo.get("baseOptionItemName"));
            List<Node> productToCategoryMap = nodeService.getNodeList("productToCategoryMap", "productId_matching=".concat(productId));
            List<Node> productCoupon = new ArrayList<>();
            for (Node coupon : couponList) {
                Node couponType = nodeService.getNode("couponType", JsonUtils.getStringValue(coupon, "couponTypeId"));
                List<Map<String, Object>> couponTypeToCategoryMapList = null;
                List<Map<String, Object>> couponTypeToProductMapList = null;
                for (Node productCategory : productToCategoryMap) {
                    couponTypeToCategoryMapList = nodeBindingService.list("couponTypeToCategoryMap", "couponTypeId_equals=".concat(JsonUtils.getStringValue(coupon, "couponTypeId").concat("&categoryId_equals=").concat(JsonUtils.getStringValue(productCategory, "categoryId"))));
                    if (couponTypeToCategoryMapList != null && couponTypeToCategoryMapList.size() > 0) {
                        for (Map<String, Object> couponTypeToCategoryMap : couponTypeToCategoryMapList) {
                            coupon.put("couponType", couponType);
                            Map<String, Double> couponDiscountCalculatorMap = couponDiscountCalculator(orderPrice, coupon);
                            coupon.put("tempOrderProductId", JsonUtils.getDoubleValue(targetProduct, "tempOrderProductId"));
                            coupon.put("orderPrice", JsonUtils.getDoubleValue(targetProduct, "orderPrice"));
                            coupon.put("resultDiscountPrice", couponDiscountCalculatorMap.get("resultDiscountPrice"));
                            coupon.put("resultOrderPrice", couponDiscountCalculatorMap.get("resultOrderPrice"));
                            productCoupon.add(coupon.clone());
                        }
                    }
                }
                coupon.put("couponTypeToCategoryMapList", couponTypeToCategoryMapList);
                couponTypeToProductMapList = nodeBindingService.list("couponTypeToProductMap", "couponTypeId_equals=".concat(JsonUtils.getStringValue(coupon, "couponTypeId").concat("&productId_equals=").concat(productId)));
                if (coupon.get("couponTypeToProductMapList") != null && ((List) coupon.get("couponTypeToProductMapList")).size() > 0) {
                    for (Map<String, Object> couponTypeToProductMap : couponTypeToProductMapList) {
                        coupon.put("couponType", couponType);
                        Map<String, Double> couponDiscountCalculatorMap = couponDiscountCalculator(orderPrice, coupon);
                        if (couponDiscountCalculatorMap.get("resultOrderPrice") != orderPrice) {
                            coupon.put("tempOrderProductId", JsonUtils.getDoubleValue(targetProduct, "tempOrderProductId"));
                            coupon.put("orderPrice", JsonUtils.getDoubleValue(targetProduct, "orderPrice"));
                            coupon.put("resultDiscountPrice", couponDiscountCalculatorMap.get("resultDiscountPrice"));
                            coupon.put("resultOrderPrice", couponDiscountCalculatorMap.get("resultOrderPrice"));
                            productCoupon.add(coupon.clone());
                        }
                    }
                }
                coupon.put("couponTypeToProductMapList", couponTypeToProductMapList);
            }
            targetProduct.put("coupon", productCoupon);
        }
        queryResult.put("items", targetProductList);

        context.setResult(queryResult);

        return context;
    }

    private ExecuteContext maxumDiscountPriceCoupons(ExecuteContext context){


        return context;
    }

    private Map<String, Object> getProductInfo(Map<String, Object> product) {
        Map<String, Object> productInfo = new HashMap<>();
        String productId = JsonUtils.getStringValue(product, "productId");
        String baseOptionItemId = JsonUtils.getStringValue(product, "baseOptionItemId");

        Node productNode = nodeService.getNode("product", productId);
        Node productOptionItemNode = nodeService.getNode("productOptionItem", baseOptionItemId);

        productInfo.put("productName", productNode.getStringValue("name"));
        productInfo.put("baseOptionItemName", productOptionItemNode.getStringValue("name"));

        return productInfo;
    }

    private Map<String, Double> couponDiscountCalculator(Double orderPrice, Node coupon) {
        Map<String, Double> result = new HashMap<>();
        Node couponType = (Node) coupon.get("couponType");

        String benefitsType = String.valueOf(couponType.getBindingValue("benefitsType"));
        Double benefitsPrice = (Double) couponType.getBindingValue("benefitsPrice");
        Double maxDiscountPrice = (Double) couponType.get("maxDiscountPrice");
        Double minPurchasePrice = (Double) couponType.get("minPurchasePrice");
        Double resultDiscountPrice = 0D;
        Double resultOrderPrice = 0D;


        switch (benefitsType) {
            case "discountRate":
                resultDiscountPrice = orderPrice * benefitsPrice / 100;
                if (resultDiscountPrice > maxDiscountPrice) {
                    resultDiscountPrice = maxDiscountPrice;
                }
                break;

            case "discountPrice":
                if (minPurchasePrice <= orderPrice) {
                    resultDiscountPrice = benefitsPrice;
                }
                break;
        }

        resultOrderPrice = Math.ceil(orderPrice - resultDiscountPrice);

        result.put("resultOrderPrice", resultOrderPrice);
        result.put("resultDiscountPrice", resultDiscountPrice);

        return result;
    }
}
