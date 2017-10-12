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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
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
        Map<String, Object> applicableCoupons = new HashMap<>();
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
        String targetProductItemTypeId = nodeType.getTypeId().concat("ProductItem");   //cartProductItem or tempOrderProductItem
        String id = nodeType.getIdablePIds().get(0);
        String idValue = JsonUtils.getStringValue(contextData, "id");

        List<Map<String, Object>> targetProductList = nodeBindingService.list(targetProductTypeId, "sorting=created&".concat(id).concat("_equals=").concat(idValue));
        List<Map<String, Object>> targetProductItemList = nodeBindingService.list(targetProductItemTypeId, "sorting=created&".concat(id).concat("_equals=)").concat(idValue));
        Node memberNode = (Node) sessionData.get("member");
        String memberNo = memberNode.getStringValue("memberNo");
        String siteType = String.valueOf(memberNode.getBindingValue("siteType"));
        List<Node> couponList = (List<Node>) NodeQuery.build("coupon").matching("memberNo", memberNo).matching("couponStatus", "n").matching("siteType", "all,".concat(siteType)).getList();

//        for (Node coupon : couponList) {
//            Node couponType = nodeService.getNode("couponType", JsonUtils.getStringValue(coupon, "couponTypeId"));
//            coupon.put("couponType", couponType);
//
//            switch (String.valueOf(couponType.getBindingValue("couponType"))) {
//                case "category":
//                    List<Map<String, Object>> couponTypeToCategoryMapList = nodeBindingService.list("couponTypeToCategoryMap", "couponTypeId_equals=".concat(JsonUtils.getStringValue(coupon, "couponTypeId")));
//                    coupon.put("couponTypeToCategoryMapList", couponTypeToCategoryMapList);
//                    break;
//
//                case "product":
//                    List<Map<String, Object>> couponTypeToProductMapList = nodeBindingService.list("couponTypeToProductMap", "couponTypeId_equals=".concat(JsonUtils.getStringValue(coupon, "couponTypeId")));
//                    coupon.put("couponTypeToProductMapList", couponTypeToProductMapList);
//                    break;
//                default:
//                    break;
//            }
//        }


        for (Map<String, Object> targetProduct : targetProductList) {
            String productId = JsonUtils.getStringValue(targetProduct, "productId");
            Double orderPrice = JsonUtils.getDoubleValue(targetProduct, "orderPrice");
            List<Node> productToCategoryMap = nodeService.getNodeList("productToCategoryMap", "productId_matching=".concat(productId));
            List<Node> productCoupon = new ArrayList<>() ;
            Map<String, Object> valueList1 = new HashMap<>();
            Map<String, Object> valueList2 = new HashMap<>();
            for (Node coupon : couponList) {
                coupon.clone();
                Node couponType = nodeService.getNode("couponType", JsonUtils.getStringValue(coupon, "couponTypeId"));
                switch (String.valueOf(couponType.getBindingValue("couponType"))) {
                    case "category":
                        List<Map<String, Object>> couponTypeToCategoryMapList = nodeBindingService.list("couponTypeToCategoryMap", "couponTypeId_equals=".concat(JsonUtils.getStringValue(coupon, "couponTypeId")));
                        coupon.put("couponTypeToCategoryMapList", couponTypeToCategoryMapList);
                        break;

                    case "product":
                        List<Map<String, Object>> couponTypeToProductMapList = nodeBindingService.list("couponTypeToProductMap", "couponTypeId_equals=".concat(JsonUtils.getStringValue(coupon, "couponTypeId")));
                        coupon.put("couponTypeToProductMapList", couponTypeToProductMapList);
                        break;
                    default:
                        break;
                }
                for (Node productCategory : productToCategoryMap) {
                    if (coupon.get("couponTypeToCategoryMapList") != null && ((List) coupon.get("couponTypeToCategoryMapList")).size() > 0) {
                        List<Map<String, Object>> couponTypeToCategoryMapList = (List<Map<String, Object>>) coupon.get("couponTypeToCategoryMapList");
                        List<Map<String, Object>> values = new ArrayList<>();
                        for (Map<String, Object> couponTypeToCategoryMap : couponTypeToCategoryMapList) {
                            Map<String, Object> value = new HashMap<>();
                            if (StringUtils.equals(JsonUtils.getStringValue(productCategory, "categoryId"), JsonUtils.getStringValue(couponTypeToCategoryMap, "categoryId"))) {
                                coupon.put("couponType", couponType);
                                Map<String, Double> couponDiscountCalculatorMap = couponDiscountCalculator(orderPrice, coupon);
                                couponTypeToCategoryMap.put("resultDiscountPrice", couponDiscountCalculatorMap.get("resultDiscountPrice"));
                                couponTypeToCategoryMap.put("resultOrderPrice", couponDiscountCalculatorMap.get("resultOrderPrice"));
                                value.put("resultDiscountPrice", couponDiscountCalculatorMap.get("resultDiscountPrice"));
                                value.put("resultOrderPrice", couponDiscountCalculatorMap.get("resultOrderPrice"));
                            }
                            values.add(value);
                        }
                        valueList1.put(coupon.getId(), values);
                        coupon.put("valueList1", valueList1);
                    }
                }
                if (coupon.get("couponTypeToProductMapList") != null && ((List) coupon.get("couponTypeToProductMapList")).size() > 0) {
                    List<Map<String, Object>> couponTypeToProductMapList = (List<Map<String, Object>>) coupon.get("couponTypeToProductMapList");
                    for (Map<String, Object> couponTypeToProductMap : couponTypeToProductMapList) {
                        if (StringUtils.equals(productId, JsonUtils.getStringValue(couponTypeToProductMap, "productId"))) {
                            coupon.put("couponType", couponType);
                            Map<String, Double> couponDiscountCalculatorMap = couponDiscountCalculator(orderPrice, coupon);
                            if (couponDiscountCalculatorMap.get("resultOrderPrice") != orderPrice) {
                                couponTypeToProductMap.put("resultDiscountPrice", couponDiscountCalculatorMap.get("resultDiscountPrice"));
                                couponTypeToProductMap.put("resultOrderPrice", couponDiscountCalculatorMap.get("resultOrderPrice"));
                            }
                        }
                    }
                }
                productCoupon.add(coupon);

            }
            targetProduct.put("coupon", productCoupon);
        }
        queryResult.put("items", targetProductList);

        context.setResult(queryResult);

        return context;
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

/*//    적용가능한
    public ExecuteContext applicable(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        if(data.get("memberNo") == null) return context;

        String[] params = { "tempOrderId" };
        if (common.requiredParams(context, data, params)) return context;

        NodeBindingInfo nodeBindingInfo = NodeUtils.getNodeBindingInfo(context.getNodeType().getTypeId()) ;
        List<Map<String, Object>> tempOrderProducts = nodeBindingService.list("tempOrderProduct", "tempOrderId_matching="+data.get("tempOrderId"));

        String siteType = (data.get("siteType") == null ? "" : data.get("siteType").toString());
        for(Map<String, Object> tempOrderProduct : tempOrderProducts){
            List<Map<String, Object>> list = new ArrayList<>();
            List<Map<String, Object>> coupons = getUseableCouponsForProduct(nodeBindingInfo, siteType, data.get("memberNo").toString(), tempOrderProduct.get("productId").toString());
            for(Map<String, Object> coupon : coupons){
                Node node = nodeService.getNode("couponType", coupon.get("couponTypeId").toString());
                Integer discountPrice = getDiscountPrice(node, tempOrderProduct.get("orderPrice"));
                if(discountPrice > 0){
                    coupon.put("discountPrice", discountPrice);
                    list.add(coupon);
                }
            }

            tempOrderProduct.put("applicableCoupons", list);

        }

        Map<String, Object> object = new HashMap<>();
        object.put("items", tempOrderProducts);

        context.setResult(object);

        return context;
    }

    public Integer getDiscountPrice(Node couponType, Object orderPrice){
        Integer result = 0;
        if("discountRate".equals(couponType.getValue("benefitsType"))){
//            IF((15000 / 100 * y.benefitsPrice) > y.maxDiscountPrice, y.maxDiscountPrice,(15000 / 100 * y.benefitsPrice))

        }else if("discountPrice".equals(couponType.getValue("benefitsType"))){
//            IF(y.minPurchasePrice < 1500, y.benefitsPrice, 0)


        }else if("freeDelivery".equals(couponType.getValue("benefitsType"))){

        }

        return result;
    }

    public List<Map<String, Object>> getUseableCouponsForProduct(NodeBindingInfo nodeBindingInfo, String siteType, String memberNo, String productId){
        List<Map<String, Object>> list = new ArrayList<>();
        String query = "SELECT a.*, c.productId as useableProductId\n" +
                "FROM coupon a, coupontypetoproductmap c\n" +
                "WHERE a.memberNo = ? AND c.productId = ? \n" +
                "      AND a.couponTypeId = c.couponTypeId\n" +
                "      AND a.siteType != IF(IFNULL(?, 'company') = 'university', 'company', 'university')\n" +
                "UNION ALL\n" +
                "SELECT a.*, c.productId as useableProductId\n" +
                "FROM coupon a\n" +
                "  , (\n" +
                "      SELECT couponTypeId,productId\n" +
                "      FROM couponTypeToCategoryMap c, producttocategorymap p\n" +
                "      WHERE productId = ? \n" +
                "            AND p.categoryId = c.categoryId\n" +
                "      GROUP BY couponTypeId\n" +
                "    ) c\n" +
                "WHERE a.memberNo = ? \n" +
                "      AND a.couponTypeId = c.couponTypeId";
        list = nodeBindingInfo.getJdbcTemplate().queryForList(query, memberNo, productId, siteType, productId, memberNo);

        return list;
    }*/
}
