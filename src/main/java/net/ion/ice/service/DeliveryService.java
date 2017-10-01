package net.ion.ice.service;

import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service("deliveryService")
public class DeliveryService {
    @Autowired
    private NodeBindingService nodeBindingService ;
    @Autowired
    private NodeService nodeService ;

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

    public void setDeliveryPrice(Map<String, Object> cartProduct, Node product) throws IOException {
        Map<String, Object> map = new LinkedHashMap<String, Object>(cartProduct);
        String deliveryPriceType = StringUtils.substringAfter(product.get("deliveryPriceType").toString(), ">");
        String deliveryMethod = StringUtils.substringAfter(product.get("deliveryMethod").toString(), ">");
        String deliveryDateType = StringUtils.substringAfter(product.get("deliveryDateType").toString(), ">");
        map.putAll(product);
        map.remove("cartProductItem");

        // 유료배송비 : charge
        // 수량별배송비 : quantity (기준수량별로 장바구니 상품 row 나뉘고 setDeliveryPrice 이므로 무조건 create)
        if ("quantity".equals(deliveryPriceType) || "charge".equals(deliveryPriceType)) {
            map.put("cartProductIds", map.get("cartProductId"));
            CommonService.resetMap(map);
            nodeService.executeNode(map, CartService.cartDeliveryPrice_TID, CommonService.CREATE);
        } else {
            // 무료배송비 : free
            // 조건부무료배송 : conditional
            String searchText = "cartId_equals=" + map.get("cartId")
                    + "&vendorId_equals=" + map.get("vendorId")
                    + "&bundleDeliveryYn_equals=y&deliveryPriceType_in=free,conditional"
                    + "&deliveryMethod_equals=" + deliveryMethod
                    + "&deliveryDateType_equals=" + deliveryDateType
                    + ("hopeDelivery".equals(deliveryDateType) ? "&hopeDeliveryDate_equals=" + cartProduct.get("hopeDeliveryDate") : "")
                    + ("scheduledDelivery".equals(deliveryDateType) ? "&scheduledDeliveryDate_equals=" + cartProduct.get("scheduledDeliveryDate") : "");

            List<Map<String, Object>> cartDeliveryPrices = nodeBindingService.list(CartService.cartDeliveryPrice_TID, searchText);
            if (cartDeliveryPrices.size() == 0) {
                map.put("deliveryPrice", calculateDeliveryPrice(map.get("cartProductId").toString()));
                map.put("cartProductIds", map.get("cartProductId"));
                CommonService.resetMap(map);
                nodeService.executeNode(map, CartService.cartDeliveryPrice_TID, CommonService.CREATE);
            } else {
                for (Map<String, Object> deliveryPrice : cartDeliveryPrices) {
                    String cartProductIds = (deliveryPrice.get("cartProductIds").toString()).concat(",").concat(map.get("cartProductId").toString());

                    deliveryPrice.put("deliveryPriceType", ("free".equals(deliveryPrice.get("deliveryPriceType")) || "free".equals(deliveryPriceType) ? "free" : "conditional"));
                    deliveryPrice.put("deliveryPrice", calculateDeliveryPrice(cartProductIds));
                    deliveryPrice.put("cartProductIds", cartProductIds);
                    CommonService.resetMap(deliveryPrice);
                    nodeService.executeNode(deliveryPrice, CartService.cartDeliveryPrice_TID, CommonService.UPDATE);

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