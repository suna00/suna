package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeQuery;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.session.SessionService;
import net.ion.ice.plugin.excel.ExcelService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service("couponService")
public class CouponService {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private ExcelService excelService;
    @Autowired
    private DeliveryService deliveryService;

    CommonService common;

    public ExecuteContext download(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());
        Map<String, Object> session = null;
        try {
            session = sessionService.getSession(context.getHttpRequest());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(common.PATTERN);
        LocalDateTime now = LocalDateTime.now();

        String[] params = {"couponTypeId"};
        if (common.requiredParams(context, data, params)) return context;

        String memberNo = JsonUtils.getStringValue(session, "member.memberNo");

        Node couponType = nodeService.getNode("couponType", data.get("couponTypeId").toString());
        if (couponType == null) {
            common.setErrorMessage(context, "V0001");
            return context;
        }

        List<Node> list = nodeService.getNodeList("coupon", "memberNo_matching=" + memberNo + "&couponTypeId_matching=" + data.get("couponTypeId"));
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
        if (StringUtils.equals("limit", String.valueOf(couponType.getBindingValue("validePeriodType")))) {
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

    public Map<String,Double> productCouponDiscountPrice(Integer tempOrderProductId, String couponId, String siteType, Integer memberNo){
        Map<String, Double> couponDiscountPriceMap = new HashMap<>();
        List<Map<String,Object>> tempOrderProduct = nodeBindingService.list("tempOrderProduct", "tempOrderProductId_equals=".concat(String.valueOf(tempOrderProductId)));
        String now = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
        List<Node> couponList = new ArrayList<>();
        if(!couponId.equals("")){
            couponList = (List<Node>) NodeQuery.build("coupon").matching("couponId", String.valueOf(couponId)).matching("memberNo", String.valueOf(memberNo)).matching("couponStatus", "n").matching("siteType", "all,".concat(siteType)).above("endDate", now).getList();
        }

        Double orderPrice =  ((BigDecimal) tempOrderProduct.get(0).get("orderPrice")).doubleValue();
        if(couponList.size() > 0){
            Node couponNode = couponList.get(0);
            Node couponType = nodeService.getNode("couponType", JsonUtils.getStringValue(couponNode, "couponTypeId"));
            couponDiscountPriceMap = couponDiscountCalculator(orderPrice, couponType);
        }else{
            couponDiscountPriceMap.put("orderPrice", orderPrice);
            couponDiscountPriceMap.put("resultOrderPrice", orderPrice);
            couponDiscountPriceMap.put("resultDiscountPrice", 0D);
        }
        return couponDiscountPriceMap;
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
        List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(targetProductList, typeId);
        Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, typeId);

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
            items.add(itemResult);
        }



        Node memberNode = (Node) sessionData.get("member");
        String memberNo = memberNode.getStringValue("memberNo");
        String siteType = String.valueOf(memberNode.getBindingValue("siteType"));
        String now = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
        List<Node> couponList = (List<Node>) NodeQuery.build("coupon").matching("memberNo", memberNo).matching("couponStatus", "n").matching("siteType", "all,".concat(siteType)).above("endDate", now).getList();
        Map<String, Object> initCouponMap = new HashMap<>();
        Map<String, Object> initDeliveryCouponMap = new HashMap<>();

        for(Map<String, Object> item : items){
            List<Map<String, Object>> productList = (List<Map<String, Object>>) item.get("item");

            for (Map<String, Object> targetProduct : productList) {
                initCouponMap.put(String.valueOf(targetProduct.get("tempOrderProductId")), null);
                initDeliveryCouponMap.put(String.valueOf(targetProduct.get("tempOrderProductId")), null);
                String productId = JsonUtils.getStringValue(targetProduct, "productId");
                Double orderPrice = JsonUtils.getDoubleValue(targetProduct, "orderPrice");
                Map<String, Object> productInfo = getProductInfo(targetProduct);
                targetProduct.put("productName", productInfo.get("productName"));
                targetProduct.put("baseOptionItemName", productInfo.get("baseOptionItemName"));
                targetProduct.put("contentsType", productInfo.get("contentsType"));
                targetProduct.put("deliverySeq", JsonUtils.getStringValue(item, "deliverySeq"));
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
                                Map<String, Double> couponDiscountCalculatorMap = couponDiscountCalculator(orderPrice, couponType);
                                coupon.put("tempOrderProductId", JsonUtils.getIntValue(targetProduct, "tempOrderProductId"));
                                coupon.put("orderPrice", JsonUtils.getDoubleValue(targetProduct, "orderPrice"));
                                coupon.put("resultDiscountPrice", couponDiscountCalculatorMap.get("resultDiscountPrice"));
                                coupon.put("resultOrderPrice", couponDiscountCalculatorMap.get("resultOrderPrice"));
                                coupon.put("deliveryPrice", JsonUtils.getDoubleValue(item, "deliveryPrice"));
                                productCoupon.add(coupon.clone());
                            }
                        }
                    }
                    coupon.put("couponTypeToCategoryMapList", couponTypeToCategoryMapList);
                    couponTypeToProductMapList = nodeBindingService.list("couponTypeToProductMap", "couponTypeId_equals=".concat(JsonUtils.getStringValue(coupon, "couponTypeId").concat("&productId_equals=").concat(productId)));
                    if (coupon.get("couponTypeToProductMapList") != null && ((List) coupon.get("couponTypeToProductMapList")).size() > 0) {
                        for (Map<String, Object> couponTypeToProductMap : couponTypeToProductMapList) {
                            coupon.put("couponType", couponType);
                            Map<String, Double> couponDiscountCalculatorMap = couponDiscountCalculator(orderPrice, couponType);
                            if (couponDiscountCalculatorMap.get("resultOrderPrice") != orderPrice) {
                                coupon.put("tempOrderProductId", JsonUtils.getIntValue(targetProduct, "tempOrderProductId"));
                                coupon.put("orderPrice", JsonUtils.getDoubleValue(targetProduct, "orderPrice"));
                                coupon.put("resultDiscountPrice", couponDiscountCalculatorMap.get("resultDiscountPrice"));
                                coupon.put("resultOrderPrice", couponDiscountCalculatorMap.get("resultOrderPrice"));
                                coupon.put("deliveryPrice", JsonUtils.getDoubleValue(item, "deliveryPrice"));
                                productCoupon.add(coupon.clone());
                            }
                        }
                    }
                    coupon.put("couponTypeToProductMapList", couponTypeToProductMapList);
                }
                targetProduct.put("coupon", productCoupon);
            }
        }
        List<Map<String, Object>> allBasicCouponList = allCouponList(targetProductList, "basic");
        List<Map<String, Object>> allDeliveryCouponList = allCouponList(targetProductList, "delivery");
        Map<String, Object> maximumCouponMap = sortMaximumList(initCouponMap, allBasicCouponList);
        Map<String, Object> maximumDeliveryCouponMap = sortMaximumDeliveryList(initDeliveryCouponMap, allDeliveryCouponList);

        queryResult.put("items", targetProductList);
        queryResult.put("maximum", maximumCouponMap);
        queryResult.put("delivery", maximumDeliveryCouponMap);
        context.setResult(queryResult);

        return context;
    }

    private Map<String, Object> sortMaximumDeliveryList(Map<String, Object> resultMap, List<Map<String, Object>> allCouponList) {
        List<Integer> selectedProductList = new ArrayList<>();
        List<Integer> selectedCouponList = new ArrayList<>();

        int index = 0;

        for (Map<String, Object> coupon : allCouponList) {
            Map<String, Object> couponType = (Map<String, Object>) coupon.get("couponType");

            if(!StringUtils.equals(JsonUtils.getStringValue(couponType,"benefitsType"), "benefitsType>freeDelivery")){
                continue;
            }

            boolean duplicated1 = false;
            boolean duplicated2 = false;

            if (index == 0) {
                resultMap.put(String.valueOf(coupon.get("tempOrderProductId")), coupon.get("couponId"));
                selectedProductList.add((Integer) coupon.get("tempOrderProductId"));
                selectedCouponList.add((Integer) coupon.get("couponId"));
                index++;
                continue;
            }

            for (Integer tempOrderProductId : selectedProductList) {
                if (StringUtils.equals(String.valueOf(coupon.get("tempOrderProductId")), String.valueOf(tempOrderProductId))) {
                    if(resultMap.get(String.valueOf(tempOrderProductId)) != null){
                        duplicated1 = true;
                        break;
                    }
                }
            }

            for (Integer couponId : selectedCouponList) {
                if (StringUtils.equals(String.valueOf(coupon.get("couponId")), String.valueOf(couponId))) {
                    duplicated2 = true;
                    break;
                }

            }

            if (duplicated1 || duplicated2) {
                continue;
            }
            resultMap.put(String.valueOf(coupon.get("tempOrderProductId")), coupon.get("couponId"));
            selectedProductList.add((Integer) coupon.get("tempOrderProductId"));
            selectedCouponList.add((Integer) coupon.get("couponId"));
        }
        return resultMap;
    }

    private Map<String, Object> sortMaximumList(Map<String, Object> resultMap, List<Map<String, Object>> allCouponList) {
        List<Integer> selectedProductList = new ArrayList<>();
        List<Integer> selectedCouponList = new ArrayList<>();

        int index = 0;

        for (Map<String, Object> coupon : allCouponList) {
            Map<String, Object> couponType = (Map<String, Object>) coupon.get("couponType");

            if(StringUtils.equals(JsonUtils.getStringValue(couponType,"benefitsType"), "benefitsType>freeDelivery")){
                break;
            }

            boolean duplicated1 = false;
            boolean duplicated2 = false;

            if (index == 0) {
                resultMap.put(String.valueOf(coupon.get("tempOrderProductId")), coupon.get("couponId"));
                selectedProductList.add((Integer) coupon.get("tempOrderProductId"));
                selectedCouponList.add((Integer) coupon.get("couponId"));
                index++;
                continue;
            }

            for (Integer tempOrderProductId : selectedProductList) {
                if (StringUtils.equals(String.valueOf(coupon.get("tempOrderProductId")), String.valueOf(tempOrderProductId))) {
                    if(resultMap.get(String.valueOf(tempOrderProductId)) != null){
                        duplicated1 = true;
                        break;
                    }
                }
            }

            for (Integer couponId : selectedCouponList) {
                if (StringUtils.equals(String.valueOf(coupon.get("couponId")), String.valueOf(couponId))) {
                    duplicated2 = true;
                    break;
                }

            }

            if (duplicated1 || duplicated2) {
                continue;
            }
            resultMap.put(String.valueOf(coupon.get("tempOrderProductId")), coupon.get("couponId"));
            selectedProductList.add((Integer) coupon.get("tempOrderProductId"));
            selectedCouponList.add((Integer) coupon.get("couponId"));
        }
        return resultMap;
    }

    private List<Map<String, Object>> allCouponList(List<Map<String, Object>> productList, String couponType) {
        List<Map<String, Object>> allCouponList = new ArrayList<>();

        for (Map<String, Object> product : productList) {
            List<Map<String, Object>> couponList = (List<Map<String, Object>>) product.get("coupon");
            allCouponList.addAll(couponList);
        }

        if(couponType.equals("basic")){
            Collections.sort(allCouponList, new sortMaximumDiscountPriceCompare());
        }else{
            Collections.sort(allCouponList, new sortMaximumDeliveryPriceCompare());
        }
        return allCouponList;
    }

    static class sortMaximumDiscountPriceCompare implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            return (Double) o1.get("resultDiscountPrice") > (Double) o2.get("resultDiscountPrice") ? -1 : (Double) o1.get("resultDiscountPrice") < (Double) o2.get("resultDiscountPrice") ? 1 : 0;
        }
    }


    static class sortMaximumDeliveryPriceCompare implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            return (Double) o1.get("deliveryPrice") > (Double) o2.get("deliveryPrice") ? -1 : (Double) o1.get("deliveryPrice") < (Double) o2.get("deliveryPrice") ? 1 : 0;
        }
    }

    private Map<String, Object> getProductInfo(Map<String, Object> product) {
        Map<String, Object> productInfo = new HashMap<>();
        String productId = JsonUtils.getStringValue(product, "productId");
        String baseOptionItemId = JsonUtils.getStringValue(product, "baseOptionItemId");

        Node productNode = nodeService.getNode("product", productId);
        Node productOptionItemNode = nodeService.getNode("productOptionItem", baseOptionItemId);

        productInfo.put("productName", productNode.getStringValue("name"));
        productInfo.put("contentsType", productNode.getBindingValue("contentsType"));
        productInfo.put("baseOptionItemName", productOptionItemNode.getStringValue("name"));

        return productInfo;
    }

    private Map<String, Double> couponDiscountCalculator(Double orderPrice, Node couponType) {
        Map<String, Double> result = new HashMap<>();

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

        result.put("orderPrice", orderPrice);
        result.put("resultOrderPrice", resultOrderPrice);
        result.put("resultDiscountPrice", resultDiscountPrice);

        return result;
    }

    public ExecuteContext uploadExcel(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        MultipartFile file = data.get("excelFile") == null ? null : (MultipartFile) data.get("excelFile");
        Map<String, Object> parsedResult = excelService.parsingExcelFile(file);
        Iterator<String> parsedResultKeyIterator = parsedResult.keySet().iterator();
        String firstKey = parsedResultKeyIterator.next();

        List<Map<String, String>> items = new ArrayList<>();

        if (parsedResult.get(firstKey) != null) {
            List<Map<String, String>> memberDataList = (List<Map<String, String>>) parsedResult.get("회원정보");
            for (Map<String, String> memberData : memberDataList) {
                List<String> values = new ArrayList<>(memberData.values());

                String memberId = values.get(0);

                Node memberNode = nodeService.getNode("member", memberId);
                if (memberNode != null) {
                    String memberNo = memberNode.getBindingValue("memberNo").toString();
                    String userId = memberNode.getBindingValue("userId").toString();
                    String name = memberNode.getBindingValue("name").toString();
                    String siteType = memberNode.getBindingValue("siteType").toString();
                    String company = memberNode.getBindingValue("company") == null ? "" : memberNode.getBindingValue("company").toString();
                    String universityName = memberNode.getBindingValue("universityName") == null ? "" : memberNode.getBindingValue("universityName").toString();
                    String companyUniversityName = "";
                    if (StringUtils.equals(siteType, "company")) {
                        companyUniversityName = company;
                    } else if (StringUtils.equals(siteType, "university")) {
                        companyUniversityName = universityName;
                    }

                    Map<String, String> item = new HashMap<>();
                    item.put("label", String.format("%s(%s/%s)", userId, name, companyUniversityName));
                    item.put("value", memberNo);

                    items.add(item);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);

        context.setResult(result);

        return context;
    }

    public ExecuteContext distribute(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String couponTypeIdValue = data.get("couponTypeId") == null ? "" : data.get("couponTypeId").toString();
        String couponDistributeTargetTypeValue = data.get("couponDistributeTargetType") == null ? "" : data.get("couponDistributeTargetType").toString();
        String couponDistributeTargetValue = data.get("couponDistributeTarget") == null ? "" : data.get("couponDistributeTarget").toString();
        Date publishedDate = new Date();

        Node couponTypeNode = nodeService.getNode("couponType", couponTypeIdValue);

        Boolean distributeValidation = true;

        String limitedQuantityType = couponTypeNode.getBindingValue("limitedQuantityType") == null ? "" : couponTypeNode.getBindingValue("limitedQuantityType").toString();
        if (StringUtils.equals(limitedQuantityType, "limit")) {
            Integer limitedQuantity = couponTypeNode.getBindingValue("limitedQuantity") == null ? 0 : (Integer) couponTypeNode.getBindingValue("limitedQuantity");
            Integer distributeQuantity = couponTypeNode.getBindingValue("distributeQuantity") == null ? 0 : (Integer) couponTypeNode.getBindingValue("distributeQuantity");

            if (distributeQuantity >= limitedQuantity) distributeValidation = false;
        }

        if (distributeValidation) {
            if (StringUtils.equals(couponDistributeTargetTypeValue, "group")) {
                List<String> couponDistributeTargetList = Arrays.asList(StringUtils.split(couponDistributeTargetValue, ","));

                for (String couponDistributeTarget : couponDistributeTargetList) {
                    if (StringUtils.equals(couponDistributeTarget, "companyMember")) {
                        List<Node> companyMemberNodeList = (List<Node>) NodeQuery.build("member").matching("siteType", "company").matching("memberStatus", "join").getList();

                        for (Node companyMemberNode : companyMemberNodeList) {
                            createCoupon(couponTypeNode, companyMemberNode, publishedDate);
                        }
                    } else if (StringUtils.equals(couponDistributeTarget, "universityMember")) {
                        List<Node> universityMemberNodeList = (List<Node>) NodeQuery.build("member").matching("siteType", "university").matching("memberStatus", "join").getList();

                        for (Node universityMemberNode : universityMemberNodeList) {
                            createCoupon(couponTypeNode, universityMemberNode, publishedDate);
                        }
                    }
                }
            } else if (StringUtils.equals(couponDistributeTargetTypeValue, "search")) {
                List<String> memberNoList = Arrays.asList(StringUtils.split(couponDistributeTargetValue, ","));

                for (String memberNo : memberNoList) {
                    Node memberNode = nodeService.getNode("member", memberNo);
                    String memberStatus = memberNode.getBindingValue("memberStatus").toString();
                    if (StringUtils.equals(memberStatus, "join")) {
                        createCoupon(couponTypeNode, memberNode, publishedDate);
                    }
                }
            } else if (StringUtils.equals(couponDistributeTargetTypeValue, "excel")) {
                List<String> memberNoList = Arrays.asList(StringUtils.split(couponDistributeTargetValue, ","));

                for (String memberNo : memberNoList) {
                    Node memberNode = nodeService.getNode("member", memberNo);
                    String memberStatus = memberNode.getBindingValue("memberStatus").toString();
                    if (StringUtils.equals(memberStatus, "join")) {
                        createCoupon(couponTypeNode, memberNode, publishedDate);
                    }
                }
            }

            Integer distributeQuantity = couponTypeNode.getBindingValue("distributeQuantity") == null ? 0 : (Integer) couponTypeNode.getBindingValue("distributeQuantity");

            Map<String, Object> couponTypeData = new HashMap<>();
            couponTypeData.put("couponTypeId", couponTypeNode.getBindingValue("couponTypeId"));
            couponTypeData.put("distributeQuantity", distributeQuantity+1);
            nodeService.executeNode(couponTypeData, "couponType", EventService.UPDATE);
        }

        return context;
    }

    private void createCoupon(Node couponTypeNode, Node memberNode, Date publishedDate) {
        if (couponTypeNode == null || memberNode == null) return;

        String validPeriodType = couponTypeNode.getBindingValue("validPeriodType").toString();
        Date startDate = null;
        Date endDate = null;
        if (StringUtils.equals(validPeriodType, "limit")) {
            Integer validPeriod = (Integer) couponTypeNode.getStoreValue("validPeriod");
            if (validPeriod == null) validPeriod = 0;

            Calendar publishedCal = Calendar.getInstance();
            publishedCal.setTime(publishedDate);

            startDate = publishedCal.getTime();
            publishedCal.add(Calendar.DATE, validPeriod);
            endDate = publishedCal.getTime();
        }

        Map<String, Object> couponData = new HashMap<>();
        couponData.put("name", couponTypeNode.getBindingValue("name"));
        couponData.put("memberNo", memberNode.getBindingValue("memberNo"));
        couponData.put("siteType", memberNode.getBindingValue("siteType"));
        couponData.put("channelType", couponTypeNode.getBindingValue("channelType"));
        couponData.put("affiliateId", memberNode.getBindingValue("affiliateId"));
        couponData.put("couponTypeId", couponTypeNode.getBindingValue("couponTypeId"));
        couponData.put("publishedDate", publishedDate);
        couponData.put("startDate", startDate);
        couponData.put("endDate", endDate);
        couponData.put("couponStatus", "y");
        nodeService.executeNode(couponData, "coupon", EventService.CREATE);
    }
}
