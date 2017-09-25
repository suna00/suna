package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service("cartService")
public class CartService {
    private static Logger logger = LoggerFactory.getLogger(CartService.class);

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String SAVE = "save";
    public static final String cartProduct_TID = "cartProduct";
    public static final String cartProductItem_TID = "cartProductItem";
    public static final String cartDeliveryPrice_TID = "cartDeliveryPrice";

    @Autowired
    private NodeService nodeService ;
    @Autowired
    private NodeBindingService nodeBindingService ;
    public NodeBindingInfo nodeBindingInfo;


    public ExecuteContext addCart(ExecuteContext context) throws IOException {
        nodeBindingInfo = NodeUtils.getNodeBindingInfo(context.getNodeType().getTypeId());
        Map<String, Object> data = context.getData();

        // validate
//        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get(cartProduct_TID).toString());
//        for(Map<String, Object> map : maps){
//            if(!checkSaleStatus(context, map.get("productId").toString())) return context;
//            if(!checkStock(context, map.get("baseOptionItemId").toString(), (int) map.get("quantity"))) return context;
//            for(Map<String, Object> item : (List<Map<String, Object>>) map.get(cartProductItem_TID)){
//                if(!checkStock(context, item.get("addOptionItemId").toString(), (int) item.get("quantity"))) return context;
//            }
//        }

        Node cart = (Node) nodeService.executeNode(data, "cart", SAVE);
        data.put("cartId", cart.getId());
        addProducts(data, cart.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("cartId", cart.getId());
        context.setResult(CommonService.getResult("S0002", result));

        return context;
    }

    // 재고
    public boolean checkStock(ExecuteContext context, String productOptionItemId, int quantity){
        Node node = NodeUtils.getNode("productOptionItem", productOptionItemId);

        if((int) node.getValue("stockQuantity") < quantity){
            context.setResult(CommonService.getResult("P0005"));
            return false;
        }
        return true;
    }

    // 판매가능여부
    public boolean checkSaleStatus(ExecuteContext context, String productId){
        Node node = NodeUtils.getNode("product", productId);

        if(!"approve".equals(node.getValue("approvalStatus"))){
            context.setResult(CommonService.getResult("P0001"));
            return false;
        }

        if(!"y".equals(node.getValue("productStatus"))){
            context.setResult(CommonService.getResult("P0002"));
            return false;
        }

        if("approve".equals(node.getValue("deleteStatus"))){
            context.setResult(CommonService.getResult("P0003"));
            return false;
        }

        if(!"sale".equals(node.getValue("saleStatus"))){
            context.setResult(CommonService.getResult("P0004"));
            return false;
        }

        return true;
    }

    // 수량변경
    public ExecuteContext changeQuantity(ExecuteContext context) throws IOException {
        nodeBindingInfo = NodeUtils.getNodeBindingInfo(context.getNodeType().getTypeId());
        Map<String, Object> data = context.getData();
        Map<String, Object> deliveryPriceMap = getCartDeliveryPriceMap(data.get("cartProductId").toString());

        Node node = NodeUtils.getNode("cartProduct", data.get("cartProductId").toString());

//        if(!checkSaleStatus(context, node.get("productId").toString())) return context;
//        if(!checkStock(context, node.getValue("baseOptionItemId").toString(), (int) data.get("quantity"))) return context;

        node.put("quantity", data.get("quantity"));
        if("quantity".equals(deliveryPriceMap.get("deliveryPriceType"))){
            Node product = NodeUtils.getNode("product", node.getValue("productId").toString());
            createCartProductByDeliveryQuantity(node, product);
        }else{
            nodeService.executeNode(node, cartProduct_TID, SAVE);
            // 배송비 재처리
            Map<String, Object> map = getTotalProductPriceMap(data.get("cartProductId").toString());
            if(Double.parseDouble(map.get("totalProductPrice").toString()) >= Double.parseDouble(map.get("deliveryConditionValue").toString())){
                deliveryPriceMap.put("deliveryPrice", 0);
            }else{
                deliveryPriceMap.put("deliveryPrice", map.get("deliveryPrice"));
            }
            nodeService.executeNode(deliveryPriceMap, cartDeliveryPrice_TID, SAVE);

        }

        context.setResult(CommonService.getResult("S0002"));

        return context;
    }

    public void removeProduct(ExecuteContext context) throws IOException {
        nodeBindingInfo = NodeUtils.getNodeBindingInfo(context.getNodeType().getTypeId());
        Map<String, Object> data = context.getData();
        String productIds = (String) data.get("productIds");
        String[] productIdsArray = productIds.split(",");
        for(String productId : productIdsArray){
            nodeBindingService.delete(cartProduct_TID, productId);
            List<Map<String, Object>> cartProductItemList = nodeBindingService.list(cartProductItem_TID, "cartProductId_in=".concat(productId));
            if(cartProductItemList.size() > 0){
                for(Map<String, Object> cartProductItem : cartProductItemList){
                    nodeBindingService.delete(cartProductItem_TID, String.valueOf(cartProductItem.get("cartProductItemId")));
                }
            }
            removeDeliveryPrice(productId);

        }
        context.setResult(CommonService.getResult("C0001"));
    }

    private void removeDeliveryPrice(String cartProductId) throws IOException {
        Map<String, Object> result = getCartDeliveryPriceMap(cartProductId);
        List<String> ids = new ArrayList<String>(Arrays.asList(result.get("cartProductIds").toString().split(",")));

        if(ids.size() == 1){
            nodeBindingService.delete(cartDeliveryPrice_TID, result.get("cartDeliveryPriceId").toString());
        }else{
            ids.remove(cartProductId);
            String cartProductIds = StringUtils.join(ids, ",");
            result.put("cartProductIds", cartProductIds);
            result.put("deliveryPrice", calculateDeliveryPrice(null, cartProductIds));
            nodeService.executeNode(result, cartDeliveryPrice_TID, UPDATE);
        }
    }

    private void addProducts(Map<String, Object> data, Object cartId) throws IOException {
        List<Map<String, Object>> referenced = nodeBindingService.list(cartProduct_TID, "cartId_equals="+cartId);
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get(cartProduct_TID).toString());

        for(Map<String, Object> map : maps){
            Node product = NodeUtils.getNode("product", map.get("productId").toString());
            boolean exist = false;
            boolean quantityDeliveryType = ("deliveryPriceType>quantity".equals(product.getValue("deliveryPriceType")));

            for(Map<String, Object> obj : referenced){
                if(!quantityDeliveryType && (map.get("baseOptionItemId").toString()).equals(obj.get("baseOptionItemId").toString())){
                    exist = true;
                }
            }
            if(!exist){
                map.putAll(data);
                map.put("vendorId",product.getValue("vendorId"));

                Map<String, Object> cartProductMap = new LinkedHashMap<>();
                if(quantityDeliveryType){
                    cartProductMap = createCartProductByDeliveryQuantity(map, product);
                }else{
                    cartProductMap = createCartProduct(map);
                    cartProductMap.putAll(product);

                    if(map.get(cartProductItem_TID) != null) createCartProductItem(cartProductMap);

                    setDeliveryPrice(map, cartProductMap);
                }

            }else{
                // 장바구니에 이미 존재하는 상품
            }
        }

    }

    // 수량별 배송비 경우 cartProduct 쪼갬
    private Map<String, Object> createCartProductByDeliveryQuantity(Map<String, Object> map, Node product) throws IOException {
        int quantity = Integer.parseInt(map.get("quantity").toString());
        int deliveryConditionValue = Integer.parseInt(product.getValue("deliveryConditionValue").toString());
        Map<String, Object> cartProductMap = new LinkedHashMap<>();

        // 장바구니에 수량별 배송료 같은 기본옵션이 있는경우 나머지 수량 채워줌.
        List<Map<String, Object>> cartProducts = nodeBindingService.list(cartProduct_TID, "cartId_equals="+map.get("cartId")+"&baseOptionItemId_equals="+map.get("baseOptionItemId")+"&quantity_below="+deliveryConditionValue);
        if(cartProducts.size() > 0){
            for(Map<String, Object> cartProduct : cartProducts){
                int qtt = Integer.parseInt(cartProduct.get("quantity").toString());
                if(qtt < deliveryConditionValue){
                    cartProduct.put("quantity", deliveryConditionValue);
                    nodeService.executeNode(cartProduct, cartProduct_TID, UPDATE) ;

                    quantity = quantity - ( deliveryConditionValue - qtt );
                }
            }
        }

        if(quantity > deliveryConditionValue){
            int count = (int) Math.ceil((double) quantity / (double) deliveryConditionValue);
            for(int i=0 ; i < count ; i++){
                int cartProductQuantity = (i != count-1 ? deliveryConditionValue : ( quantity - (count-1) * deliveryConditionValue ));
                map.put("quantity", cartProductQuantity);
                cartProductMap = createCartProduct(map);
                cartProductMap.putAll(product);

                if(i == 0){
                    if(map.get(cartProductItem_TID) != null) createCartProductItem(cartProductMap);
                }
                setDeliveryPrice(map, cartProductMap);
            }
        }else{
            cartProductMap = createCartProduct(map);
            cartProductMap.putAll(product);

            if(map.get(cartProductItem_TID) != null) createCartProductItem(cartProductMap);

            setDeliveryPrice(map, cartProductMap);
        }

        return cartProductMap;
    }

    private Map<String, Object> createCartProduct(Map<String, Object> map) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        newMap.putAll(map);
        newMap.remove("cartProductId");
        Node node = (Node) nodeService.executeNode(newMap, cartProduct_TID, CREATE);
        newMap.put("cartProductId", node.getId());

        return newMap;
    }

    private void createCartProductItem(Map<String, Object> cartProduct) {
        for(Map<String, Object> item : (List<Map<String, Object>>) cartProduct.get(cartProductItem_TID)){
            item.putAll(cartProduct);
            nodeService.executeNode(item, cartProductItem_TID, CREATE) ;
        }

    }

    private void updateCartProduct(Map<String, Object> map) {

        Node node = (Node) nodeService.executeNode(map, cartProduct_TID, CREATE);
        map.put("cartProductId", node.getId());

        createCartProductItem(map);
    }

    public void setDeliveryPrice(Map<String, Object> map, Map<String, Object> cartProduct) throws IOException {
        String deliveryPriceType = StringUtils.substringAfter(cartProduct.get("deliveryPriceType").toString(),">");
        String deliveryMethod = StringUtils.substringAfter(cartProduct.get("deliveryMethod").toString(),">");

//        유료배송비 : charge
//        수량별배송비 : quantity
        if("quantity".equals(deliveryPriceType) || "charge".equals(deliveryPriceType)){
            cartProduct.put("cartProductIds", cartProduct.get("cartProductId"));
            cartProduct.put("deliveryMethod", deliveryMethod);
            nodeService.executeNode(cartProduct, cartDeliveryPrice_TID, CREATE);
        }else{
//            무료배송비 : free
//            조건부무료배송 : conditional
            List<Map<String, Object>> cartDeliveryPrices = nodeBindingService.list(cartDeliveryPrice_TID, "cartId_equals="+cartProduct.get("cartId")+"&vendorId_equals="+cartProduct.get("vendorId")+"&deliveryMethod_equals="+deliveryMethod+"&bundleDeliveryYn_equals=y&deliveryPriceType_in=free,conditional");
            if(cartDeliveryPrices.size() == 0){
                cartProduct.put("cartProductIds", cartProduct.get("cartProductId"));
                cartProduct.put("deliveryPrice", calculateDeliveryPrice(null, cartProduct.get("cartProductId").toString()));
                cartProduct.put("deliveryMethod", deliveryMethod);
                nodeService.executeNode(cartProduct, cartDeliveryPrice_TID, CREATE);
            }else{
                for(Map<String, Object> deliveryPrice : cartDeliveryPrices){
                    if("free".equals(deliveryPrice.get("deliveryPriceType")) || "free".equals(deliveryPriceType)){
                        deliveryPrice.put("deliveryPriceType", "free");
                        deliveryPrice.put("cartProductIds", (deliveryPrice.get("cartProductIds").toString()).concat(",").concat(cartProduct.get("cartProductId").toString()));
                        nodeService.executeNode(deliveryPrice, cartDeliveryPrice_TID, UPDATE);
                    }else{
                        deliveryPrice.put("deliveryPrice", calculateDeliveryPrice(map, deliveryPrice.get("cartProductIds").toString()));
                        deliveryPrice.put("cartProductIds", (deliveryPrice.get("cartProductIds").toString()).concat(",").concat(cartProduct.get("cartProductId").toString()));
                        nodeService.executeNode(deliveryPrice, cartDeliveryPrice_TID, UPDATE);

                    }
                }
            }
        }
    }

//    배송비 계산
    public Integer calculateDeliveryPrice(Map<String, Object> map, String cartProductIds) throws IOException {
        Map<String, Object> m = getTotalProductPriceMap(cartProductIds);
        Integer totalProductPrice = (m.get("totalProductPrice") != null ? (int) Double.parseDouble(m.get("totalProductPrice").toString()) : 0);

        String deliveryPriceType = m.get("deliveryPriceType").toString();
        Integer deliveryConditionValue = Integer.parseInt(m.get("deliveryConditionValue").toString());

        if(deliveryPriceType.contains("free")) return 0;
        if(deliveryPriceType.contains("conditional")){
            Integer price = (map != null ? getTotalProductPrice(map) : 0);
            if(cartProductIds != null){
                price = price + totalProductPrice;
            }
            if(price >= deliveryConditionValue) return 0;
        }
        return (int) Double.parseDouble(m.get("deliveryPrice").toString());
    }

//    담을 상품
    public Integer getTotalProductPrice(Map<String, Object> map) throws IOException {
//        {
//            "productId": 503,
//                "baseOptionItemId": 50019,
//                "quantity": 2,
//                "cartProductItem": [
//            {
//                "addOptionItemId": 50020,
//                    "quantity": 3
//            }
//  ]
//        }
        Integer price = getBaseOptionProductPrice(map);

        if(map.get("cartProductItem") != null){
            price = price + getAddOptionProductPrice(map);
        }

        return price;
    }

    private Integer getAddOptionProductPrice(Map<String, Object> map) throws IOException {
        Integer price = 0;
        String query = "select IFNULL(max(pi.addPrice * ?), 0) as addOptionPrice\n" +
                "      from productoptionitem pi\n" +
                "      where pi.productOptionItemId = ? " ;

        for(Map<String, Object> item : JsonUtils.parsingJsonToList(map.get("cartProductItem").toString())){
            Map<String, Object> result = nodeBindingInfo.getJdbcTemplate().queryForMap(query, item.get("quantity"), item.get("addOptionItemId"));
            price = price + (int) Double.parseDouble(result.get("addOptionPrice").toString());
        }
        return price;
    }

    private Integer getBaseOptionProductPrice(Map<String, Object> map) {
        String query = "select\n" +
                        "  IFNULL(max((p.salePrice + pi.addPrice) * ?), 0) as baseOptionPrice\n" +
                        "from productoptionitem pi, product p\n" +
                        "where p.productId = pi.productId\n" +
                        "  and pi.productOptionItemId = ? " ;
        Map<String, Object> result = nodeBindingInfo.getJdbcTemplate().queryForMap(query, map.get("quantity"), map.get("baseOptionItemId"));
        return (int) Double.parseDouble(result.get("baseOptionPrice").toString());
    }

    //    기존 담긴 상품
    public Map<String, Object> getTotalProductPriceMap(String cartProductIds){
        String[] ids = StringUtils.split(cartProductIds,",");
        List<String> holder = new ArrayList<>();
        for(String id : ids){
            holder.add("?");
        }

        String query = "select (totalBaseOptionPrice + totalAddOptionPrice) as totalProductPrice, deliveryPrice, deliveryConditionValue, deliveryPriceType\n" +
                "from (\n" +
                "   SELECT\n" +
                "     cp.cartId\n" +
                "     , sum((ifnull(pi.addPrice, 0) + ifnull(p.salePrice, 0)) * cp.quantity) AS totalBaseOptionPrice\n" +
                "     , ifnull((SELECT sum(pi.addPrice * ci.quantity) FROM cartproductitem ci, productoptionitem pi WHERE cartId = cp.cartId AND ci.addOptionItemId = pi.productOptionItemId), 0) AS totalAddOptionPrice\n" +
                "     , min(p.deliveryPrice) as deliveryPrice, min(p.deliveryConditionValue) as deliveryConditionValue, group_concat(p.deliveryPriceType) as deliveryPriceType\n" +
                "   FROM cartproduct cp, productoptionitem pi, product p\n" +
                "   WHERE cp.baseOptionItemId = pi.productOptionItemId AND p.productId = pi.productId\n" +
                "         and cp.cartProductId in ( " + StringUtils.join(holder, ", ") + ")\n" +
                "   group by cp.cartId\n" +
                ") x";

        return nodeBindingInfo.getJdbcTemplate().queryForMap(query, cartProductIds);
    }

    private Map<String, Object> getCartDeliveryPriceMap(String cartProductId) {
        String query = "select * from cartdeliveryprice\n" +
                "where find_in_set(?, cartProductIds) > 0 " ;
        return nodeBindingInfo.getJdbcTemplate().queryForMap(query, cartProductId);
    }

}
