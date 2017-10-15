package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service("cartService")
public class CartService {
    private static Logger logger = LoggerFactory.getLogger(CartService.class);

    public static final String cartProduct_TID = "cartProduct";
    public static final String cartProductItem_TID = "cartProductItem";
    public static final String cartDeliveryPrice_TID = "cartDeliveryPrice";

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService;

    @Autowired
    private SessionService sessionService;

    // 장바구니 조회
    public ExecuteContext cartRead(ExecuteContext context) throws IOException {

        Map<String, Object> sessionData = sessionService.getSession(context.getHttpRequest());

        String sessionId = sessionService.getSessionKey(context.getHttpRequest());
        if (sessionData.containsKey("cartId")) {
            context.getData().put("cartId", sessionData.get("cartId"));
        } else {
            context.getData().put("cartId", "abcdefg");
        }

        context.getData().put("sessionId", sessionId);
        if (sessionData.containsKey("member")) {
            context.getData().put("memberNo", JsonUtils.getStringValue(sessionData, "memberNo"));
        }

        Integer totalSize = 0;
        List<Map<String, Object>> cartProducts = nodeBindingService.list("cartProduct", "sorting=created&cartId_equals=" + context.getData().get("cartId"));
        List<Map<String, Object>> cartProductItems = nodeBindingService.list("cartProductItem", "sorting=created&cartId_equals=" + context.getData().get("cartId"));
        // cart 만들기
        for (Map<String, Object> cartProduct : cartProducts) {
            Integer cartProductId = JsonUtils.getIntValue(cartProduct, "cartProductId");
            List<Map<String, Object>> subCartProdductItems = new ArrayList<>();
            for (Map<String, Object> cartProductItem : cartProductItems) {
                if (cartProductId.equals(JsonUtils.getIntValue(cartProductItem, "cartProductId"))) {
                    subCartProdductItems.add(cartProductItem);
                }
            }
            cartProduct.put("cartProductItem", subCartProdductItems);
        }

        List<Map<String, Object>> deliveryProductList = deliveryService.makeDeliveryData(cartProducts, "cart");
        Map<String, Object> deliveryPriceList = deliveryService.calculateDeliveryPrice(deliveryProductList, "cart");

        QueryResult queryResult = new QueryResult();
        List<QueryResult> items = new ArrayList<>();

        for (String key : deliveryPriceList.keySet()) {
            QueryResult itemResult = new QueryResult();
            itemResult.put("deliverySeq", key);
            List<Map<String, Object>> priceList = (List<Map<String, Object>>) deliveryPriceList.get(key);

            itemResult.put("deliveryPrice", priceList.get(0).get("deliveryPrice"));

            List<Map<String, Object>> subProductResult = new ArrayList<>();
            for (Map<String, Object> priceProduct : priceList) {
                priceProduct.put("downloadableCoupon", getCouponCount(JsonUtils.getStringValue(priceProduct, "productId"), JsonUtils.getStringValue(sessionData, "siteType")));
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


    // 장바구니 담기
    public ExecuteContext addCart(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();

        // validate
//        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get(cartProduct_TID).toString());
//        for(Map<String, Object> map : maps){
//            if(!checkSaleStatus(context, map.get("productId").toString())) return context;
//            if(!checkStock(context, map.get("baseOptionItemId").toString(), (int) map.get("quantity"))) return context;
//            if(map.get(cartProductItem_TID) != null){
//                for(Map<String, Object> item : (List<Map<String, Object>>) map.get(cartProductItem_TID)){
//                    if(!checkStock(context, item.get("addOptionItemId").toString(), (int) item.get("quantity"))) return context;
//                }
//            }
//            if(!checkQuantity(context, map)) return context;
//        }

        Map<String, Object> sessionData = sessionService.getSession(context.getHttpRequest());

        String sessionId = sessionService.getSessionKey(context.getHttpRequest());
        CommonService.resetMap(data);
        if (sessionData.containsKey("cartId")) {
            data.put("cartId", sessionData.get("cartId"));
        } else {
            data.put("cartId", "");
        }

        data.put("sessionId", sessionId);
        if (sessionData.containsKey("member")) {
            data.put("memberNo", JsonUtils.getStringValue(sessionData, "memberNo"));
        }

        Node cart = (Node) nodeService.executeNode(data, "cart", CommonService.SAVE);
        data.put("cartId", cart.getId());
        sessionData.put("cartId", cart.getId());


        if (data.get("product") != null) {
            addProducts(data, cart.getId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cartId", cart.getId());
        context.setResult(CommonService.getResult("S0002", result));

        return context;
    }

    // 재고
    public boolean checkStock(ExecuteContext context, String productOptionItemId, int quantity) {
        Node node = NodeUtils.getNode("productOptionItem", productOptionItemId);

        if ((int) node.getValue("stockQuantity") < quantity) {
            context.setResult(CommonService.getResult("P0005"));
            return false;
        }
        return true;
    }

    // 판매가능여부
    public boolean checkSaleStatus(ExecuteContext context, String productId) {
        Node node = NodeUtils.getNode("product", productId);

        if (!"approvalStatus>approve".equals(node.getValue("approvalStatus"))) {
            context.setResult(CommonService.getResult("P0001"));
            return false;
        }

        if (!"displayStatus>y".equals(node.getValue("productStatus"))) {
            context.setResult(CommonService.getResult("P0002"));
            return false;
        }

        if ("deleteStatus>approve".equals(node.getValue("deleteStatus"))) {
            context.setResult(CommonService.getResult("P0003"));
            return false;
        }

        if (!"saleStatus>sale".equals(node.getValue("saleStatus"))) {
            context.setResult(CommonService.getResult("P0004"));
            return false;
        }

        return true;
    }

    // 수량 체크 (최소,최대:0이면 무제한)
    public boolean checkQuantity(ExecuteContext context, Map<String, Object> map) {
        Node cartProduct = NodeUtils.getNode(cartProduct_TID, map.get("cartProductId").toString());
        int change = Integer.parseInt(cartProduct.get("quantity").toString()) + Integer.parseInt(map.get("quantity").toString());

        if (change <= 0) {
            context.setResult(CommonService.getResult("C0002"));
            return false;
        }
        if (map.get("productId") != null || map.get("cartProductId") != null) {
            String productId = null;
            if (map.get("productId") == null) {
                productId = cartProduct.getValue("productId").toString();
            } else {
                productId = map.get("productId").toString();
            }
            Node node = NodeUtils.getNode("product", productId);
            int min = Integer.parseInt((node.getValue("minOrderQuantity") == null ? 0 : node.getValue("minOrderQuantity")).toString());
            int max = Integer.parseInt((node.getValue("maxOrderQuantity") == null ? 0 : node.getValue("maxOrderQuantity")).toString());

            Map<String, Object> temp = new LinkedHashMap<>();
            temp.put("minOrderQuantity", min);
            temp.put("maxOrderQuantity", max);

            if (change < min) {
                context.setResult(CommonService.getResult("C0003", temp));
                return false;
            }
            if (max != 0 && change > max) {
                context.setResult(CommonService.getResult("C0004", temp));
                return false;
            }
        }
        return true;
    }

    public ExecuteContext changeCartProductQuantity(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        Map<String, Object> cartProduct = nodeBindingService.list(cartProduct_TID, "cartProductId_equals=".concat(String.valueOf(data.get("cartProductId")))).get(0);
        String productId = String.valueOf(cartProduct.get("productId"));
        Node productNode = nodeService.getNode("product", productId);

        Integer stockQuantity = productNode.getIntValue("stockQuantity");                       //재고
        Integer maxOrderQuantity = productNode.getIntValue("maxOrderQuantity");                 //최대구매수량
        Integer minOrderQuantity = productNode.getIntValue("minOrderQuantity");                 //최소구매수량
        Integer changeCartProductQuantity = JsonUtils.getIntValue(data, "quantity");            //변경수량

        Map<String, Object> extraData = new HashMap<>();
        if (changeCartProductQuantity > stockQuantity) {
            extraData.put("code", "F");
            extraData.put("message", "재고가 부족합니다.");
            context.setResult(extraData);
            return context;
        }

        if (changeCartProductQuantity > maxOrderQuantity) {
            extraData.put("code", "F");
            extraData.put("message", "구매 가능한 수량을 초과하였습니다.");
            context.setResult(extraData);
            return context;
        }

        if (minOrderQuantity > changeCartProductQuantity) {
            extraData.put("code", "F");
            extraData.put("message", "최소 구매수량 " + minOrderQuantity + " 입니다");
            context.setResult(extraData);
            return context;

        }
        cartProduct.put("quantity", changeCartProductQuantity);
        nodeService.executeNode(cartProduct, cartProduct_TID, CommonService.UPDATE);
        extraData.put("code", "S");
        extraData.put("message", "수량이 변경되었습니다.");
        context.setResult(extraData);
        return context;
    }


    // 수량변경
    public ExecuteContext changeQuantity(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        Node cartProduct = NodeUtils.getNode(cartProduct_TID, data.get("cartProductId").toString());
        int quantity = Integer.parseInt(data.get("quantity").toString()) + Integer.parseInt(cartProduct.get("quantity").toString());

        if (!checkSaleStatus(context, cartProduct.get("productId").toString())) return context;
        if (!checkStock(context, cartProduct.getValue("baseOptionItemId").toString(), quantity)) return context;
        if (!checkQuantity(context, data)) return context;

        changeQuantity(cartProduct, Integer.parseInt(data.get("quantity").toString()));
        context.setResult(CommonService.getResult("S0002"));

        return context;
    }

    public void changeQuantity(Map<String, Object> cartProduct, Integer changeCount) throws IOException {
        Map<String, Object> deliveryPriceMap = deliveryService.getCartDeliveryPriceMap(cartProduct.get("cartProductId").toString());

        if ("quantity".equals(deliveryPriceMap.get("deliveryPriceType"))) {
            Node product = NodeUtils.getNode("product", cartProduct.get("productId").toString());
            if (changeCount > 0) {
                cartProduct.put("quantity", changeCount);
                createCartProductByQuantity(cartProduct, product);
            } else {
                cartProduct.put("changeCount", changeCount);
                minusCartProductQuantity(cartProduct);
            }
        } else {
            cartProduct.put("quantity", changeCount + Integer.parseInt(cartProduct.get("quantity").toString()));
            CommonService.resetMap(cartProduct);
            nodeService.executeNode(cartProduct, cartProduct_TID, CommonService.SAVE);
            // 배송비 재처리
            deliveryPriceMap.put("deliveryPrice", deliveryService.calculateDeliveryPrice(cartProduct.get("cartProductId").toString()));
            CommonService.resetMap(deliveryPriceMap);
            nodeService.executeNode(deliveryPriceMap, cartDeliveryPrice_TID, CommonService.SAVE);

        }

    }

    public void removeProduct(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        String cartProductIds = (String) data.get("productIds");
        removeProduct(cartProductIds);
        context.setResult(CommonService.getResult("C0001"));
    }

    public void removeProduct(String cartProductIds) throws IOException {
        String[] cartProductIdsArray = cartProductIds.split(",");
        for (String cartProductId : cartProductIdsArray) {
            nodeBindingService.delete(cartProduct_TID, cartProductId);
            List<Map<String, Object>> cartProductItemList = nodeBindingService.list(cartProductItem_TID, "cartProductId_equals=".concat(cartProductId));
            if (cartProductItemList.size() > 0) {
                for (Map<String, Object> cartProductItem : cartProductItemList) {
                    nodeBindingService.delete(cartProductItem_TID, String.valueOf(cartProductItem.get("cartProductItemId")));
                }
            }
//            deliveryService.removeDeliveryPrice(cartProductId);

        }
    }

//    private void removeDeliveryPrice(String cartProductId) throws IOException {
//
//        deliveryService.removeDeliveryPrice(cartProductId);
//    }

    // 동일 장바구니 상품 처리 여부
    private boolean existCartProduct(Map<String, Object> map, Map<String, Object> cartProduct, Map<String, Object> product, boolean isFirstRow) {
        if (!(map.get("baseOptionItemId").toString()).equals(cartProduct.get("baseOptionItemId").toString())) {
            return false;
        }

//        Map<String, Object> m = deliveryService.getCartDeliveryPriceMap(cartProduct.get("cartProductId").toString());
//        if ("deliveryDateType>hopeDelivery".equals(product.get("deliveryDateType"))) {
//            String cartDate = m.get("hopeDeliveryDate").toString();
//            String mapDate = map.get("hopeDeliveryDate").toString();
//            if (!cartDate.equals(mapDate)) return false;
//        }

//        if ("deliveryDateType>scheduledDelivery".equals(product.get("deliveryDateType"))) {
//            String cartDate = m.get("scheduledDeliveryDate").toString();
//            String productDate = product.get("scheduledDeliveryDate").toString();
//            if (!cartDate.equals(productDate)) return false;
//        }

        if (!("deliveryPriceType>quantity".equals(product.get("deliveryPriceType")) && isFirstRow)) {
            return false;
        }

        return true;
    }

    private void addProducts(Map<String, Object> data, Object cartId) throws IOException {
        List<Map<String, Object>> referenced = nodeBindingService.list(cartProduct_TID, "cartId_equals=" + cartId);
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get("product").toString());

        for (Map<String, Object> map : maps) {
            Node product = NodeUtils.getNode("product", map.get("productId").toString());
            boolean exist = false;
            boolean quantityDeliveryType = ("deliveryPriceType>quantity".equals(product.getValue("deliveryPriceType")));

            for (Map<String, Object> obj : referenced) {
                if (existCartProduct(map, obj, product, obj.equals(referenced.get(0)))) {
                    changeQuantity(obj, Integer.parseInt(map.get("quantity").toString()));
                    obj.putAll(map);
                    if (map.get(cartProductItem_TID) != null) createCartProductItem(obj);
                    exist = true;
                }
            }
            if (!exist) {
                map.putAll(data);
                map.put("vendorId", product.getValue("vendorId"));

                Map<String, Object> cartProductMap = new LinkedHashMap<>();
                if (quantityDeliveryType) {
                    createCartProductByQuantity(map, product);
                } else {
                    cartProductMap = createCartProduct(map);
                    cartProductMap.putAll(product);

                    if (map.get(cartProductItem_TID) != null) createCartProductItem(cartProductMap);

                    //deliveryService.setDeliveryPrice(cartProductMap, product, "cart");
                }

            }
        }

    }

    // 수량별 배송비 경우 기준수량 초과 시 cartProduct 나누기.
    private Map<String, Object> createCartProductByQuantity(Map<String, Object> map, Node product) throws IOException {
        int quantity = Integer.parseInt(map.get("quantity").toString());
        int deliveryConditionValue = Integer.parseInt(product.getValue("deliveryConditionValue").toString());
        Map<String, Object> cartProductMap = new LinkedHashMap<>();

        // 장바구니에 수량별 배송료 같은 기본옵션이 있는경우 나머지 수량 채워줌.
        List<Map<String, Object>> cartProducts = nodeBindingService.list(cartProduct_TID, "sorting=created&cartId_equals=" + map.get("cartId") + "&baseOptionItemId_equals=" + map.get("baseOptionItemId"));
        if (cartProducts.size() > 0) {
            for (Map<String, Object> cartProduct : cartProducts) {
                int qtt = Integer.parseInt(cartProduct.get("quantity").toString());
                if (qtt < deliveryConditionValue) {
                    cartProduct.put("quantity", deliveryConditionValue);
                    CommonService.resetMap(cartProduct);
                    nodeService.executeNode(cartProduct, cartProduct_TID, CommonService.UPDATE);
                    quantity = quantity - (deliveryConditionValue - qtt);
                }
            }
        }
        if (quantity <= 0) return null;
        if (quantity > deliveryConditionValue) {
            int count = (int) Math.ceil((double) quantity / (double) deliveryConditionValue);
            for (int i = 0; i < count; i++) {
                int cartProductQuantity = (i != count - 1 ? deliveryConditionValue : (quantity - (count - 1) * deliveryConditionValue));
                map.put("quantity", cartProductQuantity);
                cartProductMap = createCartProduct(map);

                if (i == 0) {
                    if (map.get("productItem") != null) createCartProductItem(cartProductMap);
                }
                //deliveryService.setDeliveryPrice(cartProductMap, product, "cart");
            }
        } else {
            map.put("quantity", quantity);
            cartProductMap = createCartProduct(map);

            if (map.get("productItem") != null) createCartProductItem(cartProductMap);

            //deliveryService.setDeliveryPrice(cartProductMap, product, "cart");
        }

        return cartProductMap;
    }

    private void minusCartProductQuantity(Map<String, Object> map) throws IOException {
        int minusCount = -(Integer.parseInt(map.get("changeCount").toString()));
        int quantity = Integer.parseInt(map.get("quantity").toString());
        Node product = NodeUtils.getNode("product", map.get("productId").toString());
        int deliveryConditionValue = Integer.parseInt(product.getValue("deliveryConditionValue").toString());

        if (quantity == minusCount) { // 얘도 원래 있을 수 없지만 함 넣어 봐써
            removeProduct(map.get("cartProductId").toString());

        } else if (quantity > minusCount) {
            map.put("quantity", quantity - minusCount);
            CommonService.resetMap(map);
            nodeService.executeNode(map, cartProduct_TID, CommonService.UPDATE);

            // 배송비 기준 수량 미달인 카트상품 row > 1 이면 합쳐주기.
            List<Map<String, Object>> cartProducts = nodeBindingService.list(cartProduct_TID, "sorting=cartProductId&cartId_equals=" + map.get("cartId") + "&baseOptionItemId_equals=" + map.get("baseOptionItemId") + "&quantity_notEquals=" + deliveryConditionValue);
            if (cartProducts.size() > 1) {
                Map<String, Object> temp = new LinkedHashMap<>();
                int need = 0;
                for (Map<String, Object> cartProduct : cartProducts) {
                    if (temp.size() == 0) {
                        temp.putAll(cartProduct);
                    } else {
                        need = deliveryConditionValue - Integer.parseInt(temp.get("quantity").toString());
                        int resource = Integer.parseInt(cartProduct.get("quantity").toString());

                        if (need == resource) {
                            temp.put("quantity", deliveryConditionValue);
                            CommonService.resetMap(temp);
                            nodeService.executeNode(temp, cartProduct_TID, CommonService.UPDATE);
                            removeProduct(cartProduct.get("cartProductId").toString());

                        } else if (need < resource) {
                            temp.put("quantity", deliveryConditionValue);
                            CommonService.resetMap(temp);
                            nodeService.executeNode(temp, cartProduct_TID, CommonService.UPDATE);

                            cartProduct.put("quantity", resource - need);
                            CommonService.resetMap(cartProduct);
                            nodeService.executeNode(cartProduct, cartProduct_TID, CommonService.UPDATE);

                        } else {
                            // need > resource
                            temp.put("quantity", deliveryConditionValue);
                            CommonService.resetMap(temp);
                            nodeService.executeNode(temp, cartProduct_TID, CommonService.UPDATE);
                            removeProduct(cartProduct.get("cartProductId").toString());
                            need = need - resource;
                        }

                    }
                    temp.clear();
                    temp.putAll(cartProduct);
                }
            }
        } else {
            // quantity < minusCount : 있을 수 없음
        }

    }

    private Map<String, Object> createCartProduct(Map<String, Object> map) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        newMap.putAll(map);
        newMap.remove("cartProductId");
        newMap.remove("cartProductItem");
        CommonService.resetMap(newMap);
        Node node = (Node) nodeService.executeNode(newMap, cartProduct_TID, CommonService.CREATE);
        map.put("cartProductId", node.getId());

        return map;
    }

    private void createCartProductItem(Map<String, Object> cartProduct) {
        for (Map<String, Object> item : (List<Map<String, Object>>) cartProduct.get("productItem")) {
            List<Map<String, Object>> cartProductItems = nodeBindingService.list(cartProductItem_TID, "cartId_equals=" + cartProduct.get("cartId") + "&addOptionItemId_equals=" + item.get("addOptionItemId"));
            if (cartProductItems.size() > 0) {
                for (Map<String, Object> obj : cartProductItems) {
                    obj.put("quantity", Integer.parseInt(obj.get("quantity").toString()) + Integer.parseInt(item.get("quantity").toString()));
                    CommonService.resetMap(obj);
                    nodeService.executeNode(obj, cartProductItem_TID, CommonService.UPDATE);
                }
            } else {
                Map<String, Object> m = new HashMap<>(cartProduct);
                m.putAll(item);
                m.remove("productList");
                m.remove("productItem");
                CommonService.resetMap(m);
                nodeService.executeNode(m, cartProductItem_TID, CommonService.CREATE);
            }
        }

    }

    public Map<String, Object> getCouponCount(String productId, String siteType) {
        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("couponType").getJdbcTemplate();
        String query = "SELECT count(*) AS couponCount\n" +
                "FROM (SELECT couponTypeId\n" +
                "      FROM couponTypeToCategoryMap c, producttocategorymap p\n" +
                "      WHERE productId = ? AND p.categoryId = c.categoryId GROUP BY couponTypeId UNION ALL SELECT couponTypeId FROM couponTypeToProductMap WHERE productId = ?) a, coupontype b\n" +
                "WHERE b.couponTypeId = a.couponTypeId AND b.couponType IN ('product', 'category') AND\n" +
                "      b.siteType != IF(IFNULL( ?, 'company') = 'university', 'company', 'university') AND\n" +
                "      createPeriodType != 'stop' AND createStartDate <= now() AND createEndDate >= now() ";
        return jdbcTemplate.queryForMap(query, productId, productId, siteType);
    }

}
