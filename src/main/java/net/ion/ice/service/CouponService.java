package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("couponService")
public class CouponService {

    @Autowired
    private NodeService nodeService;
    @Autowired
    NodeBindingService nodeBindingService ;

    CommonService common;


    public ExecuteContext download(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(common.PATTERN);
        LocalDateTime now = LocalDateTime.now();

        String[] params = { "memberNo","couponTypeId" };
        if (common.requiredParams(context, data, params)) return context;

        Node couponType = nodeService.getNode("couponType", data.get("couponTypeId").toString());
        if(couponType == null){
            common.setErrorMessage(context, "V0001");
            return context;
        }

        List<Node> list = nodeService.getNodeList("coupon", "memberNo_matching="+data.get("memberNo")+"&couponTypeId_matching="+data.get("couponTypeId"));
        if(list.size() > 0){
            common.setErrorMessage(context, "V0005");
        }

        if("createPeriodType>limit".equals(couponType.getValue("createPeriodType"))){
            LocalDateTime start = LocalDateTime.parse(couponType.getValue("createStartDate").toString(), formatter);
            LocalDateTime end = LocalDateTime.parse(couponType.getValue("createEndDate").toString(), formatter);

            if(!(start.isBefore(now) && end.isAfter(now))){
                common.setErrorMessage(context, "V0004");
                return context;
            }
        }

        if("limitYn>limit".equals(couponType.getValue("limitedQuantityType")) && "0".equals(couponType.getValue("remainingQuantity"))){
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
        if("limitYn>limit".equals(couponType.getValue("validePeriodType"))){
            LocalDateTime after = now.plusDays(Integer.parseInt(couponType.getValue("validePeriod").toString()));
            endDate = after.format(formatter);
        }

        data.put("publishedDate", now.format(formatter));
        data.put("startDate", now.format(formatter));
        data.put("endDate", endDate);
        data.put("couponStatus", "n");

        Object result = nodeService.executeNode(data, "coupon", common.SAVE);
        context.setResult(result);

        if("limitYn>limit".equals(couponType.getValue("limitedQuantityType"))){
            couponType.put("remainingQuantity", Integer.parseInt(couponType.getValue("remainingQuantity").toString()) - 1);
            nodeService.executeNode(couponType, "couponType", common.UPDATE);
        }

        return context;
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
