package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("cartService")
public class CartService {
    private static Logger logger = LoggerFactory.getLogger(CartService.class);

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String SAVE = "save";
    public static final String cartProduct_TID = "cartProduct";
    public static final String cartProductItem_TID = "cartProductItem";

    @Autowired
    private NodeService nodeService ;
    @Autowired
    private NodeBindingService nodeBindingService ;

    public void addCart(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        List<Map<String, Object>> cartProducts = new ArrayList<>();

        Node cart = (Node) nodeService.executeNode(data, "cart", SAVE);
        data.put("cartId", cart.getId());
        cartProducts = mergeList(data, cart.getId());

//        setDeliveryPrice(data, cartProducts);

    }

    public void removeProduct(ExecuteContext context) {
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
        }
        context.setResult(CommonService.getResult("C0001"));
    }

    private List<Map<String, Object>> mergeList(Map<String, Object> data, Object cartId) throws IOException {
        List<Map<String, Object>> referenced = nodeBindingService.list(cartProduct_TID, "cartId_equals="+cartId);
        List<Map<String, Object>> maps = JsonUtils.parsingJsonToList(data.get(cartProduct_TID).toString());
        List<Map<String, Object>> cartProducts = new ArrayList<>();

        for(Map<String, Object> map : maps){
            Node product = nodeService.getNode("product", map.get("productId").toString());
            boolean exist = false;
            boolean quantityDeliveryType = ("deliveryPriceType>quantity".equals(product.getValue("deliveryPriceType")));

            for(Map<String, Object> obj : referenced){
                if(!quantityDeliveryType && (map.get("baseOptionItemId").toString()).equals(obj.get("baseOptionItemId").toString())){
                    exist = true;
                    cartProducts.add(obj);
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
                }
                if(map.get(cartProductItem_TID) != null) createCartProductItem(cartProductMap);

                cartProducts.add(map);
            }else{
                // 장바구니에 이미 존재하는 상품
            }
        }

        return cartProducts;
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

                setDeliveryPrice("quantity", cartProductMap, product);
            }
        }else{
            cartProductMap = createCartProduct(map);

            setDeliveryPrice("quantity", cartProductMap, product);
        }

        return cartProductMap;
    }

    private Map<String, Object> createCartProduct(Map<String, Object> map) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        newMap.putAll(map);

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

    public void setDeliveryPrice(String deliveryPriceType, Map<String, Object> cartProduct, Map<String, Object> product) throws IOException {
        List<Map<String, Object>> deliveryPrice = new ArrayList<>();

        cartProduct.putAll(product);
        cartProduct.put("cartProductIds", cartProduct.get("cartProductId"));
        nodeService.executeNode(cartProduct, "cartDeliveryPrice", CREATE);
    }


}
