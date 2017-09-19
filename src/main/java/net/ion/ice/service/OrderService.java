package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.ion.ice.core.node.NodeUtils.getNodeBindingService;

@Service("orderService")
public class OrderService {

    private static Logger logger = LoggerFactory.getLogger(OrderService.class);

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";

    @Autowired
    private NodeService nodeService;

    public void directOrder(ExecuteContext context) {
        Map<String, Object> data = context.getData();
    }

    public void orderFromCart(ExecuteContext context) {
        Map<String, Object> data = context.getData();
    }

    // 임시 주문서 작성
    public void addTempOrder(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String tempOrderId = (String) data.get("tempOrderId");

        Node node = null;

        if (tempOrderId != null) {
            node = NodeUtils.getNode("tempOrder", tempOrderId);
        }

        if (node == null) {
            try {
                createTempOrder(context);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

        }
    }

    private void addPurchasOrder(String tempOrderId) {

    }

    public String savePayment(Map<String, Object> payment) {
        Node node = (Node) nodeService.executeNode(payment, "payment", CREATE);
        return node.getId();
    }

    public String saveDelivery(Map<String, Object> delivery) {
        Node node = (Node) nodeService.executeNode(delivery, "delivery", CREATE);
        return node.getId();
    }



    public void savePgResponse(Map<String, Object> data, String paymentId) {
        Map<String, Object> pg = new HashMap<>();

        String JsonString = JsonUtils.toJsonString(data);
        String orderSheetId = String.valueOf(data.get("ordrIdxx"));

        pg.put("paymentId", paymentId);
        pg.put("orderSheetId", orderSheetId);
        pg.put("jsonResponse", JsonString);

        nodeService.executeNode(pg, "pg", CREATE);

    }

    private void createTempOrder(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        Map<String, Object> tempOrder = new HashMap<>();
        Node tempOrderNode = (Node) nodeService.executeNode(tempOrder, "tempOrder", CREATE);
        Map<String, Object> referencedCartDeliveryPrice = null;
        try {
            referencedCartDeliveryPrice = JsonUtils.parsingJsonToMap((String) data.get("referencedCartDeliveryPrice"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> cartDeliveryPriceList = (List<Map<String, Object>>) referencedCartDeliveryPrice.get("items");

        makeTempOrderProductData(tempOrderNode.getId(), cartDeliveryPriceList);
        context.setResult("");
    }

    private void makeTempOrderProductData(Object tempOrderId, List<Map<String, Object>> cartDeliveryPriceList) {
        for (Map<String, Object> cartDeliveryPrice : cartDeliveryPriceList) {
            Map<String, Object> tempOrderDeliveryPriceData = new HashMap<>();
            List<String> tempOrderProductIds = new ArrayList<>();
            tempOrderDeliveryPriceData.put("tempOrderId", tempOrderId);
            tempOrderDeliveryPriceData.put("vendorId", ((Map<String, Object>) cartDeliveryPrice.get("vendorId")).get("value"));
            tempOrderDeliveryPriceData.put("deliveryPrice", cartDeliveryPrice.get("deliveryPrice"));
            tempOrderDeliveryPriceData.put("bundleDeliveryYn", ((Map<String, Object>) cartDeliveryPrice.get("bundleDeliveryYn")).get("value"));
            tempOrderDeliveryPriceData.put("deliveryPriceType", cartDeliveryPrice.get("deliveryPriceType"));
            tempOrderDeliveryPriceData.put("hopeDeliveryDate", cartDeliveryPrice.get("hopeDeliveryDate"));

            List<Map<String, Object>> referencedCartProduct = (List<Map<String, Object>>) cartDeliveryPrice.get("referencedCartProduct");

            for (Map<String, Object> cartProduct : referencedCartProduct) {
                Map<String, Object> tempOrderProductData = new HashMap<>();

                Map<String, Object> calc = (Map<String, Object>) cartProduct.get("calculateItem");

                tempOrderProductData.put("tempOrderId", tempOrderId);
                tempOrderProductData.put("productId", calc.get("productId"));
                tempOrderProductData.put("baseOptionItemId", calc.get("baseOptionItemId"));
                tempOrderProductData.put("quantity", calc.get("quantity"));
                tempOrderProductData.put("vendorId", calc.get("vendorId"));
                tempOrderProductData.put("salePrice", ((Map<String, Object>) calc.get("calculateItem")).get("salePrice"));
                tempOrderProductData.put("baseAddPrice", ((Map<String, Object>) calc.get("calculateItem")).get("baseAddPrice"));
                tempOrderProductData.put("productPrice", ((Map<String, Object>) calc.get("calculateItem")).get("productPrice"));
                tempOrderProductData.put("totalAddOptionPrice", ((Map<String, Object>) calc.get("calculateItem")).get("totalAddOptionPrice"));

                Node tempOrderProductNode = (Node) nodeService.executeNode(tempOrderProductData, "tempOrderProduct", CREATE);
                tempOrderProductIds.add(tempOrderProductNode.getId());
                List<Map<String, Object>> referencedCartProductItem = (List<Map<String, Object>>) cartProduct.get("referencedCartProductItem");

                for (Map<String, Object> tempOrderProductItem : referencedCartProductItem) {
                    Map<String, Object> tempOrderProductItemData = new HashMap<>();
                    tempOrderProductItemData.put("tempOrderId", tempOrderId);
                    tempOrderProductItemData.put("tempOrderProductId", tempOrderProductNode.getId());
                    tempOrderProductItemData.put("productId", ((Map<String, Object>) tempOrderProductItem.get("productId")).get("value"));
                    tempOrderProductItemData.put("addOptionItemId", ((Map<String, Object>) tempOrderProductItem.get("addOptionItemId")).get("value"));
                    tempOrderProductItemData.put("quantity", tempOrderProductItem.get("quantity"));

                    nodeService.executeNode(tempOrderProductItemData, "tempOrderProductItem", CREATE);
                }
            }
            tempOrderDeliveryPriceData.put("tempOrderProductIds", StringUtils.join(tempOrderProductIds.toArray(), ","));
            nodeService.executeNode(tempOrderDeliveryPriceData, "tempOrderDeliveryPrice", CREATE);
        }
    }


    //    "nodeType=data" getList
    private List<Map<String, Object>> getList(String tid, String searchText) {
        NodeType nodeType = NodeUtils.getNodeType(tid);
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType);
        return getNodeBindingService().getNodeBindingInfo(nodeType.getTypeId()).list(queryContext);
    }

    private void newTempOrder(Map<String, Object> data) throws IOException {
        Node node = (Node) nodeService.executeNode(data, "cart", CREATE);
        data.put("cartId", node.getId());
    }


    // batch : 주문 성공 or 일정기간 주문 성사되지 않은 주문서 제거
    public void cleanTempOrder() {

    }

    // 주문성공
    public void addOrder() {

    }
}
