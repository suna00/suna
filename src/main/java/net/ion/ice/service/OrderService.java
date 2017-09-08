package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
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
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService;

    public void directOrder(ExecuteContext context) {
        Map<String, Object> data = context.getData();


    }

    public void orderFromCart(ExecuteContext context) {
        Map<String, Object> data = context.getData();


    }

    //  주문서 작성
    public void addTempOrder(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String tempOrderId = (String) data.get("tempOrderId");

        Node node = null;

        if (tempOrderId != null) {
            node = NodeUtils.getNode("tempOrder", tempOrderId);
        }

        if (node == null) {
            try {
                createTempOrder(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

//            nodeService.executeNode(data, "tempOrder", UPDATE);
//            mergeList(data, cartId, "cartProduct");
        }

    }

    private void createTempOrder(Map<String, Object> data) throws IOException {
        Map<String, Object> tempOrder = new HashMap<>();
        Node tempOrderNode = nodeService.executeNode(tempOrder, "tempOrder", CREATE);
        Map<String, Object> referencedCartProduct = null;
        try {
            referencedCartProduct = JsonUtils.parsingJsonToMap((String) data.get("referencedCartProduct"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> cartProduct = (List<Map<String, Object>>) referencedCartProduct.get("items");

        makeTempOrderProductData(tempOrderNode.getId(), cartProduct);
    }

    private void makeTempOrderProductData(Object tempOrderId, List<Map<String, Object>> cartProduct) {

        for (Map<String, Object> product : cartProduct) {
            Map<String, Object> tempOrderProductData = new HashMap<>();

            Map<String, Object> calc = (Map<String, Object>) product.get("calculateItem");

            tempOrderProductData.put("tempOrderId", tempOrderId);
            tempOrderProductData.put("productId", calc.get("productId"));
            tempOrderProductData.put("baseOptionItemId", calc.get("baseOptionItemId"));
            tempOrderProductData.put("quantity", calc.get("quantity"));
            tempOrderProductData.put("vendorId", calc.get("vendorId"));
            tempOrderProductData.put("tempOrderDeliveryPriceId", product.get("tempOrderDeliveryPriceId"));

            Node tempOrderProductNode = nodeService.executeNode(tempOrderProductData, "tempOrderProduct", CREATE);

            List<Map<String, Object>> referencedCartProductItem = (List<Map<String, Object>>) product.get("referencedCartProductItem");
            for (Map<String, Object> tempOrderProductItem : referencedCartProductItem) {
                Map<String, Object> tempOrderProductItemData = new HashMap<>();
                tempOrderProductItemData.put("tempOrderId", tempOrderId);
                tempOrderProductItemData.put("tempOrderProductId", tempOrderProductNode.getId());
                tempOrderProductItemData.put("productId", ((Map<String, Object>) tempOrderProductItem.get("productId")).get("value"));
                tempOrderProductItemData.put("addOptionItemId", ((Map<String, Object>)tempOrderProductItem.get("addOptionItemId")).get("value"));
                tempOrderProductItemData.put("quantity", tempOrderProductItem.get("quantity"));

                nodeService.executeNode(tempOrderProductItemData, "tempOrderProductItem", CREATE);
            }
        }
    }


    //    "nodeType=data" getList
    private List<Map<String, Object>> getList(String tid, String searchText) {
        NodeType nodeType = NodeUtils.getNodeType(tid);
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType);
        return getNodeBindingService().getNodeBindingInfo(nodeType.getTypeId()).list(queryContext);
    }

    private void newTempOrder(Map<String, Object> data) throws IOException {
        Node node = nodeService.executeNode(data, "cart", CREATE);
        data.put("cartId", node.getId());
    }


    // batch : 주문 성공 or 일정기간 주문 성사되지 않은 주문서 제거
    public void cleanTempOrder() {

    }

    // 주문성공
    public void addOrder() {

    }
}
