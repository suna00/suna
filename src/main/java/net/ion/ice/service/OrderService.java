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
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private NodeBindingService nodeBindingService;
    @Autowired
    private Environment environment;

    public void directOrder(ExecuteContext context) {
        Map<String, Object> data = context.getData();
    }

    // 임시 주문서 작성
    public void addTempOrder(ExecuteContext context) {
        try {
            saveTempOrder(context);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void verification(ExecuteContext context) {
        URIBuilder uriBuilder;
        Map<String, Object> data = context.getData();
        Map<String, Object> response;

        String[] couponIds = String.valueOf(data.get("couponId")).split(",");

        try {
            /*포인트*/
            uriBuilder = new URIBuilder();
            uriBuilder.setScheme("http");
            uriBuilder.setHost("127.0.0.1");
            uriBuilder.setPath("/api/mypage/mainSummary.json");
            uriBuilder.setPort(Integer.parseInt(environment.getProperty("server.port")));
            uriBuilder.addParameter("memberNo", "77777");

            response = new RestTemplate().getForObject(uriBuilder.build(), Map.class);

            double useableYPoint = (double) ((Map<String, Object>) response.get("item")).get("useableYPoint");
            double useableWelfarepoint = (double) ((Map<String, Object>) response.get("item")).get("useableWelfarepoint");
            double useYPoint = Double.parseDouble((String) data.get("useYPoint"));
            double useWelfarepoint = Double.parseDouble((String) data.get("useWelfarepoint"));
            double deliveryPrice = Double.parseDouble((String) data.get("deliveryPrice"));

            double finalPrice = Double.parseDouble((String) data.get("finalPrice"));

            if (useYPoint > useableYPoint && useWelfarepoint > useableWelfarepoint) {
                context.setResult(CommonService.getResult("O0002"));            // 실패
                return;
            }

            /*쿠폰*/
            uriBuilder = new URIBuilder();
            uriBuilder.setScheme("http");
            uriBuilder.setHost("127.0.0.1");
            uriBuilder.setPath("api/coupon/applicable.json");
            uriBuilder.setPort(Integer.parseInt(environment.getProperty("server.port")));
            uriBuilder.addParameter("memberNo", (String) data.get("memberNo"));
            uriBuilder.addParameter("tempOrderId", (String) data.get("tempOrderId"));

            response = new RestTemplate().getForObject(uriBuilder.build(), Map.class);

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            double totalPrice = 0;

            for (Map<String, Object> item : items) {
                List<Map<String, Object>> applicableCoupons = (List<Map<String, Object>>) item.get("applicableCoupons");
                for (Map<String, Object> applicableCoupon : applicableCoupons) {
                    double productPrice = (double) applicableCoupon.get("productPrice");
                    for (String couponId : couponIds) {
                        if (couponId.equals(applicableCoupon.get("couponId"))) {
                            productPrice = productPrice - (double) applicableCoupon.get("discountPrice");
                        }
                    }
                    totalPrice = totalPrice + productPrice;
                }
            }
            // 배송비 로직 추가 필요            // 배송비 로직 추가 필요            // 배송비 로직 추가 필요            // 배송비 로직 추가 필요
            totalPrice = totalPrice - useYPoint - useWelfarepoint + deliveryPrice;
            if (totalPrice != finalPrice) { // 최종 가격 검증
                context.setResult(CommonService.getResult("O0003")); // 실패
            } else {
                context.setResult(CommonService.getResult("O0004")); // 성공
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // 주문서완료 작성
    public void addOrderSheet(Map<String, Object> responseMap) {
        Map<String, Object> orderSheet = new HashMap<>();
        Map<String, Object> tempOrder = nodeBindingService.getNodeBindingInfo("tempOrder").retrieve(String.valueOf(responseMap.get("ordrIdxx")));

        orderSheet.put("orderSheetId", tempOrder.get("tempOrderId"));
        orderSheet.put("cartId", tempOrder.get("cartId"));
        orderSheet.put("memberNo", tempOrder.get("memberNo"));
        orderSheet.put("siteId", tempOrder.get("siteId"));
        orderSheet.put("sessionId", tempOrder.get("sessionId"));
        orderSheet.put("totalProductPrice", tempOrder.get(""));
        orderSheet.put("totalDeliveryPrice", tempOrder.get(""));
        orderSheet.put("totalDiscountPrice", tempOrder.get(""));
        orderSheet.put("totalOrderPrice", tempOrder.get(""));
        orderSheet.put("totalPaymentPrice", tempOrder.get(""));
        orderSheet.put("couponDiscountPrice", tempOrder.get(""));
        orderSheet.put("totalWelfarePoint", tempOrder.get(""));
        orderSheet.put("totalYPoint", tempOrder.get(""));
        orderSheet.put("purchaseaAgreementYn", tempOrder.get(""));
        orderSheet.put("purchaseDeviceType", tempOrder.get(""));

        Node node = (Node) nodeService.executeNode(orderSheet, "orderSheet", CREATE);

    }

    // 결제 정보
    public String savePayment(Map<String, Object> responseMap) {
        Node node = (Node) nodeService.executeNode(responseMap, "payment", CREATE);
        logger.info("123123");
        return node.getId();
    }

    // 결제 배송지
    public void saveDelivery(Map<String, Object> responseMap) {
        Map<String, Object> refineDeliveryData = new HashMap<>();

        refineDeliveryData.put("orderSheetId", responseMap.get("ordrIdxx"));
        refineDeliveryData.put("addressName", responseMap.get("addressName"));
        refineDeliveryData.put("address", responseMap.get("shippingAddress"));
        refineDeliveryData.put("cellphone", responseMap.get("shippingCellPhone"));
        refineDeliveryData.put("phone", responseMap.get("shippingPhone"));
        refineDeliveryData.put("deliveryMemo", responseMap.get("deliveryMemo"));
        refineDeliveryData.put("postCode", responseMap.get("postCode"));
        refineDeliveryData.put("recipient", responseMap.get("recipient"));
        refineDeliveryData.put("deliveryType", responseMap.get("deliveryType"));

        nodeService.executeNode(refineDeliveryData, "delivery", CREATE);
    }


    //PG Response 저장
    public void savePgResponse(Map<String, Object> responseMap, String paymentId) {
        Map<String, Object> pg = new HashMap<>();

        String JsonString = JsonUtils.toJsonString(responseMap);
        String orderSheetId = String.valueOf(responseMap.get("ordrIdxx"));

        pg.put("paymentId", paymentId);
        pg.put("orderSheetId", orderSheetId);
        pg.put("jsonResponse", JsonString);

        nodeService.executeNode(pg, "pg", CREATE);

    }

    private void saveTempOrder(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        Map<String, Object> tempOrder = new HashMap<>();
        Map<String, Object> referencedCartDeliveryPrice = null;
        String cartId = String.valueOf((JsonUtils.parsingJsonToMap((String) data.get("item"))).get("cartId"));

        tempOrder.put("cartId", cartId);

        Node tempOrderNode = (Node) nodeService.executeNode(tempOrder, "tempOrder", CREATE);
        try {
            referencedCartDeliveryPrice = JsonUtils.parsingJsonToMap((String) data.get("referencedCartDeliveryPrice"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> cartDeliveryPriceList = (List<Map<String, Object>>) referencedCartDeliveryPrice.get("items");
        makeTempOrderProductData(tempOrderNode.getId(), cartDeliveryPriceList);
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("tempOrderId", tempOrderNode.getId());
        context.setResult(CommonService.getResult("O0001", extraData));
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
                tempOrderProductData.put("salePrice", calc.get("salePrice"));
                tempOrderProductData.put("baseAddPrice", calc.get("baseAddPrice"));
                tempOrderProductData.put("productPrice", calc.get("productPrice"));
                tempOrderProductData.put("totalAddOptionPrice", calc.get("totalAddOptionPrice"));
                tempOrderProductData.put("orderPrice", calc.get("orderPrice"));
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
