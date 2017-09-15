package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.nntp.Article;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.ion.ice.core.node.NodeUtils.getNodeBindingService;

@Service("orderService")
public class OrderService {

    private static Logger logger = LoggerFactory.getLogger(OrderService.class);

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";

    @Autowired
    private NodeService nodeService;
    @Autowired
    private Environment environment;

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

    public void savePayment() {
        Map<String, Object> payment = new HashMap<>();

        String orderId = "";
        String memberNo = "";
        String paymentStatus = "";
        String paymentDate = "";
        String paymentPrice = "";
        String paymentType = "";
        String creditCard = "";
        String cardNo = "";
        String installment = "";
        String bank = "";
        String accountNo = "";
        String accountOwner = "";
        String virtualAccountBank = "";
        String virtualAccountNo = "";
        String virtualAccountOwner = "";
        String virtualAccountDepositLimitDate = "";

        payment.put("orderId", orderId);
        payment.put("memberNo", memberNo);
        payment.put("paymentStatus", paymentStatus);
        payment.put("paymentDate", paymentDate);
        payment.put("paymentPrice", paymentPrice);
        payment.put("paymentType", paymentType);
        payment.put("creditCard", creditCard);
        payment.put("cardNo", cardNo);
        payment.put("installment", installment);
        payment.put("bank", bank);
        payment.put("accountNo", accountNo);
        payment.put("accountOwner", accountOwner);
        payment.put("virtualAccountBank", virtualAccountBank);
        payment.put("virtualAccountNo", virtualAccountNo);
        payment.put("virtualAccountOwner", virtualAccountOwner);
        payment.put("virtualAccountDepositLimitDate", virtualAccountDepositLimitDate);

        nodeService.executeNode(payment, "payment", CREATE);

    }

    public void savePgResponse(Map<String, Object> data) {
        Map<String, Object> pg = new HashMap<>();

        String JsonString = JsonUtils.toJsonString(data);
        String paymentId = "";
        String purchaseOrderId = "";

        pg.put("paymentId", paymentId);
        pg.put("purchaseOrderId", purchaseOrderId);
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
