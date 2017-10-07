package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service("deliveryService")
public class DeliveryService {
    @Autowired
    private NodeBindingService nodeBindingService;
    @Autowired
    private NodeService nodeService;

    public void removeDeliveryPrice(String cartProductId) throws IOException {
        Map<String, Object> result = getCartDeliveryPriceMap(cartProductId);
        List<String> ids = new ArrayList<String>(Arrays.asList(result.get("cartProductIds").toString().split(",")));

        if (ids.size() == 1) {
            nodeBindingService.delete(CartService.cartDeliveryPrice_TID, result.get("cartDeliveryPriceId").toString());
        } else {
            ids.remove(cartProductId);
            String cartProductIds = StringUtils.join(ids, ",");
            Integer deliveryPrice = calculateDeliveryPrice(cartProductIds);
            result.put("cartProductIds", cartProductIds);
            result.put("deliveryPriceType", (deliveryPrice > 0 ? "conditional" : "free"));
            result.put("deliveryPrice", deliveryPrice);
            CommonService.resetMap(result);
            nodeService.executeNode(result, CartService.cartDeliveryPrice_TID, CommonService.UPDATE);
        }
    }

    public void deliverPrice(ExecuteContext context) {

        List<Map<String, Object>> changeList = new ArrayList<>();
        List<Map<String, Object>> deliveryPriceList = new ArrayList<>();
        List<Map<String, Object>> cartProducts = nodeBindingService.list("cartProduct", "sorting=created&cartId_equals=" + context.getData().get("cartId"));
        List<Map<String, Object>> cartProductItems = nodeBindingService.list("cartProductItem", "sorting=created&cartId_equals=" + context.getData().get("cartId"));
        // cart 만들기
        for (Map<String, Object> cartProduct : cartProducts) {
            Integer cartProductId = JsonUtils.getIntValue(cartProduct, "cartProductId");
            List<Map<String, Object>> subCartProdductItems = new ArrayList<>();
            for (Map<String, Object> cartProductItem : cartProductItems) {
                if (cartProductId == JsonUtils.getIntValue(cartProductItem, "cartProductId")) {
                    subCartProdductItems.add(cartProductItem);
                }
            }
            cartProduct.put("cartProductItem", subCartProdductItems);
        }


        for (Map<String, Object> changeItem : changeList) {
            String changeType = (String) changeItem.get("changeType");
            if (changeType == null) {
                changeType = "update";
            }

            switch (changeType) {
                case "add": {
                    cartProducts.add(changeItem);
                }
                case "udpate": {
                    for (Map<String, Object> cartItem : cartProducts) {
                        if (cartItem.get("cartProductId").equals(changeItem.get("cartProductId"))) {
                            cartItem.putAll(changeItem);
                        }
                    }
                }
                case "remove": {
                    for (int i = 0; i < cartProducts.size(); i++) {
                        Map<String, Object> cartItem = cartProducts.get(i);
                        if (cartItem.get("cartProductId").equals(changeItem.get("cartProductId"))) {
                            cartProducts.remove(i);
                        }
                    }
                }
            }
        }

        calculateDeliveryPrice(cartProducts);

    }

    /*

    [

        {

            "productId": 512,
            "baseOptionId": 39,
            "quantity": 1,
            "productItem": [
                {
                    "addOptionId": 41,
                    "quantity": 1
                }
            ]
        },
        {

            "productId": 512,
            "baseOptionId": 40,
            "quantity": 1,
        }


    ]


*/


    public List<Map<String, Object>> makeDeliveryData(List<Map<String, Object>> cartProducts) {
        for (Map<String, Object> cartProduct : cartProducts) {
            System.out.println(cartProduct);
            double cartItemPrice = 0;
            Node product = nodeService.getNode("product", cartProduct.get("productId").toString());
            cartProduct.put("product", product);
            cartItemPrice = Double.parseDouble(product.getStringValue("salePrice"));

            //salePrice, 배송정책
            Node baseOptionItem = nodeService.getNode("productOptionItem", cartProduct.get("baseOptionItemId").toString());
            cartProduct.put("baseOptionItem", baseOptionItem);
            //addOptionPrice
            cartItemPrice += Double.parseDouble(baseOptionItem.getStringValue("addPrice"));

            double orderPrice = cartItemPrice * JsonUtils.getDoubleValue(cartProduct, "quantity");
            double addOptionPrice = 0;
            List<Map<String, Object>> cartProductItems = new ArrayList<>();
            for (Map<String, Object> cartProductItem : (List<Map<String, Object>>) cartProduct.get("cartProductItem")) {
                Node addOptionItem = nodeService.getNode("productOptionItem", cartProductItem.get("addOptionItemId").toString());
                addOptionItem.put("quantity", (cartProductItem.get("quantity")));
                cartProductItems.add(addOptionItem);

                orderPrice += JsonUtils.getDoubleValue(cartProductItem, "quantity") * Double.parseDouble(addOptionItem.getStringValue("addPrice"));
                addOptionPrice += JsonUtils.getDoubleValue(cartProductItem, "quantity") * Double.parseDouble(addOptionItem.getStringValue("addPrice"));
            }
            cartProduct.put("orderPrice", orderPrice);
            cartProduct.put("addOptionPrice", addOptionPrice);
            cartProduct.put("cartProductItems", cartProductItems);
        }

        return cartProducts;
//        calculateDeliveryPrice(cartProducts);
    }

    //    deliveryMethod
//            deliveryPriceType
//    deliveryPrice
//            deliveryConditionValue
//    bundleDeliveryYn
//            cashOnDeliveryYn
//    deliveryDateType
//            hopeDeliveryPossiblePeriod
//    scheduledDeliveryDate

    public Map<String, Object> calculateDeliveryPrice(List<Map<String, Object>> cartProducts) {
        Map<String, Object> vendors = new LinkedHashMap<>();
        for (Map<String, Object> cartProduct : cartProducts) {
            String vendorId = JsonUtils.getStringValue(cartProduct, "product.vendorId");
            if (!vendors.containsKey(vendorId)) {
                List<Map<String, Object>> vendorCartProducts = new ArrayList<>();
                vendors.put(vendorId, vendorCartProducts);
            }
            ((List<Map<String, Object>>) vendors.get(vendorId)).add(cartProduct);
        }

        int deliverySeq = 0;

        for (Object vendor : vendors.values()) {
            List<Map<String, Object>> soldoutProducts = makeCondtionalList((List<Map<String, Object>>) vendor, "baseOptionItem.stockQuantity", "0");
            for (Map<String, Object> soldoutProduct : soldoutProducts) {
                soldoutProduct.put("soldout", true);
            }
            List<Map<String, Object>> productEndProducts = makeNotCondtionalList((List<Map<String, Object>>) vendor, "product.productStatus", "y");
            for (Map<String, Object> productEndProduct : productEndProducts) {
                productEndProduct.put("soldout", true);
            }
            List<Map<String, Object>> saleEndProducts = makeNotCondtionalList((List<Map<String, Object>>) vendor, "product.saleStatus", "sale");
            for (Map<String, Object> saleEndProduct : saleEndProducts) {
                saleEndProduct.put("soldout", true);
            }

            List<Map<String, Object>> bundleDeliveryProducts = makeCondtionalList((List<Map<String, Object>>) vendor, "product.bundleDeliveryYn", "y");
            List<Map<String, Object>> notBundleDeliveryProducts = makeCondtionalList((List<Map<String, Object>>) vendor, "product.bundleDeliveryYn", "n");

            List<Map<String, Object>> quantityBundleDeliveryProducts = makeCondtionalList(bundleDeliveryProducts, "product.deliveryPriceType", "quantity");
            List<Map<String, Object>> freeBundleDeliveryProducts = makeCondtionalList(bundleDeliveryProducts, "product.deliveryPriceType", "free");
            List<Map<String, Object>> conditionalBundleDeliveryProducts = makeCondtionalList(bundleDeliveryProducts, "product.deliveryPriceType", "conditional");


            if (freeBundleDeliveryProducts.size() > 0) {
                freeBundleDeliveryProducts.addAll(conditionalBundleDeliveryProducts);
                conditionalBundleDeliveryProducts = new ArrayList<>();
                deliverySeq++;
                for (Map<String, Object> freeBundleDeliveryProduct : freeBundleDeliveryProducts) {
                    freeBundleDeliveryProduct.put("deliveryPrice", 0);
                    freeBundleDeliveryProduct.put("deliverySeq", deliverySeq);
                }
            } else {
                double bundleOrderPrice = 0;
                for (Map<String, Object> conditionalBundleDeliveryProduct : conditionalBundleDeliveryProducts) {
                    bundleOrderPrice += JsonUtils.getDoubleValue(conditionalBundleDeliveryProduct, "orderPrice");
                }
                boolean freeYn = false;
                double lowDeliveryPrice = 0;
                for (Map<String, Object> conditionalBundleDeliveryProduct : conditionalBundleDeliveryProducts) {
                    Double deliveryPrice = JsonUtils.getDoubleValue(conditionalBundleDeliveryProduct, "product.deliveryPrice");
                    Double deliveryConditionValue = JsonUtils.getDoubleValue(conditionalBundleDeliveryProduct, "product.deliveryConditionValue");
                    if (freeYn == false && bundleOrderPrice >= deliveryConditionValue) {
                        freeYn = true;
                    }
                    if (lowDeliveryPrice == 0) {
                        lowDeliveryPrice = deliveryPrice;
                    } else if (lowDeliveryPrice > deliveryPrice) {
                        lowDeliveryPrice = deliveryPrice;
                    }
                }
                if (freeYn) {
                    lowDeliveryPrice = 0;
                }
                deliverySeq++;
                for (Map<String, Object> conditionalBundleDeliveryProduct : conditionalBundleDeliveryProducts) {
                    conditionalBundleDeliveryProduct.put("deliveryPrice", lowDeliveryPrice);
                    conditionalBundleDeliveryProduct.put("deliverySeq", deliverySeq);
                }
            }


            List<Map<String, Object>> freeNotBundleDeliveryProducts = makeCondtionalList(notBundleDeliveryProducts, "product.deliveryPriceType", "free");
            List<Map<String, Object>> condtionalNOtBundleDeliveryProducts = makeCondtionalList(notBundleDeliveryProducts, "product.deliveryPriceType", "conditional");
            List<Map<String, Object>> chargeNotBundleDeliveryProducts = makeCondtionalList(notBundleDeliveryProducts, "product.deliveryPriceType", "charge");
            List<Map<String, Object>> quantityNotBundleDeliveryProducts = makeCondtionalList(notBundleDeliveryProducts, "product.deliveryPriceType", "quantity");

            double vendorDeliveryPrice = 0;
            for (Map<String, Object> chargeBundleDeliveryProduct : chargeNotBundleDeliveryProducts) {
                Double cartProductDeliveryPrice = JsonUtils.getDoubleValue(chargeBundleDeliveryProduct, "product.deliveryPrice");
                chargeBundleDeliveryProduct.put("deliveryPrice", cartProductDeliveryPrice);
                vendorDeliveryPrice += cartProductDeliveryPrice;
            }
            for (Map<String, Object> quantityBundleDeliveryProduct : quantityNotBundleDeliveryProducts) {
                Double cartProductDeliveryPrice = JsonUtils.getDoubleValue(quantityBundleDeliveryProduct, "product.deliveryPrice") * JsonUtils.getDoubleValue(quantityBundleDeliveryProduct, "quantity");
                quantityBundleDeliveryProduct.put("deliveryPrice", cartProductDeliveryPrice);
                vendorDeliveryPrice += cartProductDeliveryPrice;
            }

            for (Map<String, Object> condtionalNOtBundleDeliveryProduct : condtionalNOtBundleDeliveryProducts) {
                Integer deliveryConditionValue = JsonUtils.getIntValue(condtionalNOtBundleDeliveryProduct, "product.deliveryConditionValue");
                Integer orderPrice = JsonUtils.getIntValue(condtionalNOtBundleDeliveryProduct, "orderPrice");
                if (orderPrice >= deliveryConditionValue) {
                    condtionalNOtBundleDeliveryProduct.put("deliveryPrice", 0);
                } else {
                    Double cartProductDeliveryPrice = JsonUtils.getDoubleValue(condtionalNOtBundleDeliveryProduct, "product.deliveryPrice");
                    condtionalNOtBundleDeliveryProduct.put("deliveryPrice", cartProductDeliveryPrice);
                    vendorDeliveryPrice += cartProductDeliveryPrice;
                }
            }
        }

        Map<String, Object> deliveryCartProduct = new LinkedHashMap<>();
        for (Map<String, Object> cartProduct : cartProducts) {
            String deliverySeqKey = null;
            if (cartProduct.containsKey("deliverySeq")) {
                deliverySeqKey = cartProduct.get("deliverySeq").toString();
            } else {
                deliverySeqKey = cartProduct.get("cartProductId").toString();
            }
            if (!deliveryCartProduct.containsKey(deliverySeqKey)) {
                deliveryCartProduct.put(deliverySeqKey, new ArrayList<Map<String, Object>>());
            }
            List<Map<String, Object>> subCartProducts = (List<Map<String, Object>>) deliveryCartProduct.get(deliverySeqKey);
            subCartProducts.add(cartProduct);
        }
        return deliveryCartProduct;
    }

    private List<Map<String, Object>> makeCondtionalList(List<Map<String, Object>> data, String key, String value) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> item : data) {
            String val = JsonUtils.getStringValue(item, key);
            if (val != null && val.contains(">")) {
                val = StringUtils.substringAfterLast(val, ">");
            }
            if (val.equals(value) && !(item.containsKey("soldout") && (boolean) item.get("soldout"))) {
                list.add(item);
            }
        }
        return list;
    }

    private List<Map<String, Object>> makeNotCondtionalList(List<Map<String, Object>> data, String key, String value) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> item : data) {
            String val = JsonUtils.getStringValue(item, key);
            if (val != null && val.contains(">")) {
                val = StringUtils.substringAfterLast(val, ">");
            }
            if (!val.equals(value) && !(item.containsKey("soldout") && (boolean) item.get("soldout"))) {
                list.add(item);
            }
        }
        return list;
    }


    public void setDeliveryPrice(Map<String, Object> storeProduct, Node productNode, String nodeTypeId) throws IOException {
        Map<String, Object> storeProductMap = new LinkedHashMap(storeProduct);
        String deliveryPriceType = String.valueOf(productNode.getBindingValue("deliveryPriceType"));
        String deliveryMethod = String.valueOf(productNode.getBindingValue("deliveryMethod"));
        String deliveryDateType = String.valueOf(productNode.getBindingValue("deliveryDateType"));
        storeProductMap.putAll(productNode);
        storeProductMap.remove("cartProductItem");

        // 유료배송비 : charge
        // 수량별배송비 : quantity (기준수량별로 장바구니 상품 row 나뉘고 setDeliveryPrice 이므로 무조건 create)
        if ("quantity".equals(deliveryPriceType) || "charge".equals(deliveryPriceType)) {
            switch (nodeTypeId) {

                case "cart":
                    storeProductMap.put("cartProductIds", storeProductMap.get("cartProductId"));
                    CommonService.resetMap(storeProductMap);
                    nodeService.executeNode(storeProductMap, CartService.cartDeliveryPrice_TID, CommonService.CREATE);
                    break;

                case "tempOrder":
                    storeProductMap.put("tempOrderProductIds", storeProductMap.get("tempOrderProductId"));
                    CommonService.resetMap(storeProductMap);
                    nodeService.executeNode(storeProductMap, "tempOrderDeliveryPrice", CommonService.CREATE);
                    break;
            }

        } else {
            // 무료배송비 : free
            // 조건부무료배송 : conditional
            String searchText = "";
            List<Map<String, Object>> deliveryPriceList = new ArrayList<>();

            switch (nodeTypeId) {
                case "cart":
                    searchText = "cartId_equals=" + storeProductMap.get("cartId")
                            + "&vendorId_equals=" + storeProductMap.get("vendorId")
                            + "&bundleDeliveryYn_equals=y&deliveryPriceType_in=free,conditional"
                            + "&deliveryMethod_equals=" + deliveryMethod
                            + "&deliveryDateType_equals=" + deliveryDateType
                            + ("hopeDelivery".equals(deliveryDateType) ? "&hopeDeliveryDate_equals=" + storeProduct.get("hopeDeliveryDate") : "")
                            + ("scheduledDelivery".equals(deliveryDateType) ? "&scheduledDeliveryDate_equals=" + storeProduct.get("scheduledDeliveryDate") : "");
                    deliveryPriceList = nodeBindingService.list(CartService.cartDeliveryPrice_TID, searchText);
                    break;

                case "tempOrder":
                    searchText = "tempOrderId_equals=" + storeProductMap.get("tempOrderId")
                            + "&vendorId_equals=" + storeProductMap.get("vendorId")
                            + "&bundleDeliveryYn_equals=y&deliveryPriceType_in=free,conditional"
                            + "&deliveryMethod_equals=" + deliveryMethod
                            + "&deliveryDateType_equals=" + deliveryDateType
                            + ("hopeDelivery".equals(deliveryDateType) ? "&hopeDeliveryDate_equals=" + storeProduct.get("hopeDeliveryDate") : "")
                            + ("scheduledDelivery".equals(deliveryDateType) ? "&scheduledDeliveryDate_equals=" + storeProduct.get("scheduledDeliveryDate") : "");
                    deliveryPriceList = nodeBindingService.list(CartService.cartDeliveryPrice_TID, searchText);
                    break;

            }

            if (deliveryPriceList.size() == 0) {
                switch (nodeTypeId) {
                    case "cart":
                        storeProductMap.put("deliveryPrice", calculateDeliveryPrice(storeProductMap.get("cartProductId").toString()));
                        storeProductMap.put("cartProductIds", storeProductMap.get("cartProductId"));
                        CommonService.resetMap(storeProductMap);
                        nodeService.executeNode(storeProductMap, CartService.cartDeliveryPrice_TID, CommonService.CREATE);
                        break;

                    case "tempOrder":
                        storeProductMap.put("deliveryPrice", calculateDeliveryPrice(storeProductMap.get("tempOrderProductId").toString()));
                        storeProductMap.put("tempOrderProductIds", storeProductMap.get("tempOrderProductId"));
                        CommonService.resetMap(storeProductMap);
                        nodeService.executeNode(storeProductMap, "tempOrderDeliveryPrice", CommonService.CREATE);
                        break;
                }


            } else {
                switch (nodeTypeId) {
                    case "cart":
                        for (Map<String, Object> cartDeliveryPrice : deliveryPriceList) {
                            String cartProductIds = (cartDeliveryPrice.get("cartProductIds").toString()).concat(",").concat(storeProductMap.get("cartProductIds").toString());
                            cartDeliveryPrice.put("deliveryPriceType", ("free".equals(cartDeliveryPrice.get("deliveryPriceType")) || "free".equals(deliveryPriceType) ? "free" : "conditional"));
                            cartDeliveryPrice.put("deliveryPrice", calculateDeliveryPrice(cartProductIds));
                            cartDeliveryPrice.put("cartProductIds", cartProductIds);
                            CommonService.resetMap(cartDeliveryPrice);
                            nodeService.executeNode(cartDeliveryPrice, CartService.cartDeliveryPrice_TID, CommonService.UPDATE);
                        }
                        break;

                    case "tempOrder":
                        for (Map<String, Object> tempOrderDeliveryPrice : deliveryPriceList) {
                            String tempOrderProductIds = (tempOrderDeliveryPrice.get("tempOrderProductIds").toString()).concat(",").concat(storeProductMap.get("tempOrderProductIds").toString());
                            tempOrderDeliveryPrice.put("deliveryPriceType", ("free".equals(tempOrderDeliveryPrice.get("deliveryPriceType")) || "free".equals(deliveryPriceType) ? "free" : "conditional"));
                            tempOrderDeliveryPrice.put("deliveryPrice", calculateDeliveryPrice(tempOrderProductIds));
                            tempOrderDeliveryPrice.put("tempOrderProductIds", tempOrderProductIds);
                            CommonService.resetMap(tempOrderDeliveryPrice);
                            nodeService.executeNode(tempOrderDeliveryPrice, "tempOrderDeliveryPrice", CommonService.UPDATE);
                        }
                        break;
                }
            }
        }
    }

    //    배송비 계산
    public Integer calculateDeliveryPrice(String cartProductIds) throws IOException {
        Map<String, Object> m = getTotalProductPriceMap(cartProductIds);
        Integer totalProductPrice = (m.get("totalProductPrice") != null ? (int) Double.parseDouble(m.get("totalProductPrice").toString()) : 0);
        String deliveryPriceType = m.get("deliveryPriceType").toString();

        if (deliveryPriceType.contains("free")) return 0;

        Integer deliveryConditionValue = Integer.parseInt(m.get("deliveryConditionValue").toString());
        if (deliveryPriceType.contains("conditional") && totalProductPrice >= deliveryConditionValue) return 0;

        return (int) Double.parseDouble(m.get("deliveryPrice").toString());
    }

    public Integer getTotalProductPriceFromParam(Map<String, Object> map) throws IOException {

        Integer price = getBaseOptionProductPrice(map.get("baseOptionItemId"), map.get("quantity"));

        if (map.get("cartProductItem") != null) {
            price = price + getAddOptionProductPrice(map.get("cartProductItem").toString());
        }

        return price;
    }

    // 현재 추가옵션 가격
    public Integer getAddOptionProductPrice(String cartProductItem) throws IOException {
        Integer price = 0;
        String query = "select IFNULL(max(pi.addPrice * ?), 0) as addOptionPrice\n" +
                "      from productoptionitem pi\n" +
                "      where pi.productOptionItemId = ? ";
        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("YPoint").getJdbcTemplate();
        for (Map<String, Object> item : JsonUtils.parsingJsonToList(cartProductItem)) {
            Map<String, Object> result = jdbcTemplate.queryForMap(query, item.get("quantity"), item.get("addOptionItemId"));
            price = price + (int) Double.parseDouble(result.get("addOptionPrice").toString());
        }
        return price;
    }

    // 현재 기본옵션 상품 가격
    public Integer getBaseOptionProductPrice(Object baseOptionItemId, Object quantity) {
        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("YPoint").getJdbcTemplate();
        String query = "select\n" +
                "  IFNULL(max((p.salePrice + pi.addPrice) * ?), 0) as baseOptionPrice\n" +
                "from productoptionitem pi, product p\n" +
                "where p.productId = pi.productId\n" +
                "  and pi.productOptionItemId = ? ";
        Map<String, Object> result = jdbcTemplate.queryForMap(query, baseOptionItemId, quantity);
        return (int) Double.parseDouble(result.get("baseOptionPrice").toString());
    }

    public Map<String, Object> getTotalProductPriceMap(String cartProductIds) {
        String[] ids = StringUtils.split(cartProductIds, ",");
        List<String> holder = new ArrayList<String>();
        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("YPoint").getJdbcTemplate();
        for (String id : ids) {
            holder.add("?");
        }
        String holders = StringUtils.join(holder, ",");

        String query = "select (totalBaseOptionPrice + totalAddOptionPrice) as totalProductPrice, deliveryPrice, deliveryConditionValue, deliveryPriceType\n" +
                "from (\n" +
                "   SELECT\n" +
                "     cp.cartId\n" +
                "     , sum((ifnull(pi.addPrice, 0) + ifnull(p.salePrice, 0)) * cp.quantity) AS totalBaseOptionPrice\n" +
                "     , ifnull((SELECT sum(pi.addPrice * ci.quantity) FROM cartproductitem ci, productoptionitem pi WHERE cartId = cp.cartId AND ci.addOptionItemId = pi.productOptionItemId), 0) AS totalAddOptionPrice\n" +
                "     , min(p.deliveryPrice) as deliveryPrice, min(p.deliveryConditionValue) as deliveryConditionValue, group_concat(p.deliveryPriceType) as deliveryPriceType\n" +
                "   FROM cartproduct cp, productoptionitem pi, product p\n" +
                "   WHERE cp.baseOptionItemId = pi.productOptionItemId AND p.productId = pi.productId\n" +
                "         and cp.cartProductId in ( " + holders + ")\n" +
                "   group by cp.cartId\n" +
                ") x";

        return jdbcTemplate.queryForMap(query, ids);
    }

    public Map<String, Object> getCartDeliveryPriceMap(String cartProductId) {
        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("YPoint").getJdbcTemplate();
        String query = "select\n" +
                "  cartDeliveryPriceId\n" +
                "  ,cartId\n" +
                "  ,cartProductIds\n" +
                "  ,vendorId\n" +
                "  ,deliveryPrice\n" +
                "  ,bundleDeliveryYn\n" +
                "  ,deliveryMethod\n" +
                "  ,deliveryPriceType\n" +
                "  ,deliveryDateType\n" +
                "  ,date_format(scheduledDeliveryDate, '%Y%m%d%H%i%s') as scheduledDeliveryDate\n" +
                "  ,date_format(hopeDeliveryDate, '%Y%m%d%H%i%s') as hopeDeliveryDate\n" +
                "  ,created\n" +
                "  ,changed\n" +
                "from cartdeliveryprice\n" +
                "where find_in_set(?, cartProductIds) > 0 ";
        return jdbcTemplate.queryForMap(query, cartProductId);
    }
}