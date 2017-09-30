package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
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
import java.text.SimpleDateFormat;
import java.util.*;

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

    public void buyItnow(ExecuteContext context) {
        try {
            createTempOrder(context.getData());
            context.setResult(CommonService.getResult("O0001")); // 성공 시
        } catch (Exception e) {
            context.setResult("");
            e.printStackTrace();
        }
    }

    // 임시 주문서 작성
    public void addTempOrder(ExecuteContext context) {
        try {
            createTempOrder(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 사용자의 포인트, 쿠폰갯수를 조회
     */
    public Map<String, Object> getSummary(String memberNo) {
        Map<String, Object> response = new HashMap<>();
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http");
        uriBuilder.setHost("127.0.0.1");
        uriBuilder.setPath("/api/mypage/mainSummary.json");
        uriBuilder.setPort(Integer.parseInt(environment.getProperty("server.port")));
        uriBuilder.addParameter("memberNo", memberNo);

        try {
            response = new RestTemplate().getForObject(uriBuilder.build(), Map.class);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 상품에 적용 가능한 쿠폰을 조회
     */
    public Map<String, Object> getCoupon(String memberNo, String tempOrderId) {
        Map<String, Object> response = new HashMap<>();
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http");
        uriBuilder.setHost("127.0.0.1");
        uriBuilder.setPath("api/coupon/applicable.json");
        uriBuilder.setPort(Integer.parseInt(environment.getProperty("server.port")));
        uriBuilder.addParameter("memberNo", memberNo);
        uriBuilder.addParameter("tempOrderId", tempOrderId);

        try {

            response = new RestTemplate().getForObject(uriBuilder.build(), Map.class);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return response;

    }

    /**
     * PG 결제 승인 요청 전에 검증하는 Method.
     */
    public void verification(ExecuteContext context) {
        try {
            Map<String, Object> data = context.getData();

            double totalPrice = 0;

            Map<String, Object> couponIds = JsonUtils.parsingJsonToMap((String) data.get("usedCoupon"));

            /*포인트*/
            Map<String, Object> summaryResponse = getSummary((String) data.get("memberNo"));

            double useableYPoint = (double) ((Map<String, Object>) summaryResponse.get("item")).get("useableYPoint");
            double useableWelfarepoint = (double) ((Map<String, Object>) summaryResponse.get("item")).get("useableWelfarepoint");
            double useYPoint = Double.parseDouble((String) data.get("useYPoint"));
            double useWelfarepoint = Double.parseDouble((String) data.get("useWelfarepoint"));
            double deliveryPrice = Double.parseDouble((String) data.get("deliveryPrice"));

            double finalPrice = Double.parseDouble((String) data.get("finalPrice"));

            if (useYPoint > useableYPoint && useWelfarepoint > useableWelfarepoint) {
                context.setResult(CommonService.getResult("O0002"));            // 실패
                return;
            }

            /*쿠폰*/
            Map<String, Object> couponResponse = getCoupon((String) data.get("memberNo"), (String) data.get("tempOrderId"));

            List<Map<String, Object>> items = (List<Map<String, Object>>) couponResponse.get("items");

            boolean duplicated = repetitionCheck(couponIds.values());// 쿠폰 아이디 중복 체크


            for (Map<String, Object> item : items) {
                double productPrice = (double) item.get("orderPrice");
                String tempOrderProductId = String.valueOf(item.get("tempOrderProductId"));
                String couponId = String.valueOf(couponIds.get(tempOrderProductId));
                List<Map<String, Object>> applicableCoupons = (List<Map<String, Object>>) item.get("applicableCoupons");
                for (Map<String, Object> applicableCoupon : applicableCoupons) {
                    if (couponId.equals(String.valueOf(applicableCoupon.get("couponId")))) {
                        productPrice = productPrice - (double) applicableCoupon.get("discountPrice");
                    }
                }
                totalPrice = totalPrice + productPrice;
            }
            /**
             * 배송비 체크 로직이 필요하다.
             * 배송비 체크 로직이 필요하다.
             * 배송비 체크 로직이 필요하다.
             * 배송비 체크 로직이 필요하다.
             * 배송비 체크 로직이 필요하다.
             * */
            totalPrice = totalPrice - useYPoint - useWelfarepoint + deliveryPrice;

            if (totalPrice != finalPrice && duplicated) {                       // 최종 가격 검증 & 쿠폰 중복 검증
                context.setResult(CommonService.getResult("O0003"));      // 검증실패
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("ordr_mony", totalPrice);
                context.setResult(CommonService.getResult("O0004", result));      // 검증성공

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Double getFinalPrice(String reqTempOrderId, String memberNo, String reqUseYPoint, String reqUseWelfarepoint, String usedCoupon) {
        double totalPrice = 0;

        try {

            Map<String, Object> couponIds = JsonUtils.parsingJsonToMap(usedCoupon);

            /*포인트*/
            List<Map<String, Object>> tempOrderDeliveryPriceList = nodeBindingService.list("tempOrderDeliveryPrice", "tempOrderId_in=".concat(reqTempOrderId));
            double useYPoint = Double.parseDouble(reqUseYPoint);
            double useWelfarepoint = Double.parseDouble(reqUseWelfarepoint);
            double deliveryPrice = 0;

            for (Map<String, Object> tempOrderDeliveryPrice : tempOrderDeliveryPriceList) {
                deliveryPrice = deliveryPrice + Double.parseDouble(String.valueOf(tempOrderDeliveryPrice.get("deliveryPrice")));
            }


            Map<String, Object> couponResponse = getCoupon(memberNo, reqTempOrderId);

            List<Map<String, Object>> coupons = (List<Map<String, Object>>) couponResponse.get("items");

            for (Map<String, Object> coupon : coupons) {
                double productPrice = (double) coupon.get("orderPrice");
                String tempOrderProductId = String.valueOf(coupon.get("tempOrderProductId"));
                String couponId = String.valueOf(couponIds.get(tempOrderProductId));
                List<Map<String, Object>> applicableCoupons = (List<Map<String, Object>>) coupon.get("applicableCoupons");
                for (Map<String, Object> applicableCoupon : applicableCoupons) {
                    if (couponId.equals(String.valueOf(applicableCoupon.get("couponId")))) {
                        productPrice = productPrice - (double) applicableCoupon.get("discountPrice");
                    }
                }
                totalPrice = totalPrice + productPrice;
            }

            totalPrice = totalPrice - useYPoint - useWelfarepoint + deliveryPrice;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return totalPrice;
    }

    /**
     * 결제 후 최종적으로 한번 더 검증하여 주문서를 생성하는 Method.
     */
    public String createOrderSheet(Map<String, Object> responseMap) {
        String bSucc;
        Map<String, Object> storeOrderSheet = new HashMap<>();
        Map<String, Object> storeOrderDeliveryPrice = new HashMap<>();
        List<String> orderProductIds = new ArrayList<>();

        Map<String, Object> tempOrder = nodeBindingService.getNodeBindingInfo("tempOrder").retrieve(String.valueOf(responseMap.get("ordrIdxx")));
        List<Map<String, Object>> tempOrderDeliveryPriceList = nodeBindingService.list("tempOrderDeliveryPrice", "tempOrderId_in=".concat(String.valueOf(responseMap.get("ordrIdxx"))));

        double totalProductPrice = 0;
        double totalDeliveryPrice = 0;
        double totalDiscountPrice = 0;
        double totalOrderPrice = 0;
        double couponDiscountPrice = 0;
        double totalPaymentPrice = Double.parseDouble(String.valueOf(responseMap.get("amount")));
        double totalWelfarePoint = Double.parseDouble(String.valueOf(responseMap.get("useWelfarepoint")));
        double totalYPoint = Double.parseDouble(String.valueOf(responseMap.get("useYPoint")));

        Map<String, Object> summaryResponse = getSummary((String) responseMap.get("memberNo"));

        double useableYPoint = (double) ((Map<String, Object>) summaryResponse.get("item")).get("useableYPoint");
        double useableWelfarepoint = (double) ((Map<String, Object>) summaryResponse.get("item")).get("useableWelfarepoint");

        for (Map<String, Object> tempOrderDeliveryPrice : tempOrderDeliveryPriceList) {
            totalDeliveryPrice = totalDeliveryPrice + Double.parseDouble(String.valueOf(tempOrderDeliveryPrice.get("deliveryPrice")));
        }


        /**
         * 사용자 포인트를 조회하여 사용 포인트와 체크한다.
         * 사용 포인트 > 보유 포인트 시 bSucc = "true"
         * 사용 포인트 > 보유 포인트 시 bSucc = "false"
         * */
        if (totalYPoint > useableYPoint && totalWelfarePoint > useableWelfarepoint) {
            bSucc = "false";
            return bSucc;
        }

        Map<String, Object> couponIds = null;
        try {
            couponIds = JsonUtils.parsingJsonToMap((String) responseMap.get("usedCoupon"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, Object> couponResponse = getCoupon((String) responseMap.get("memberNo"), (String) responseMap.get("ordrIdxx"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) couponResponse.get("items"); //상품 정보

        boolean duplicated = repetitionCheck(couponIds.values()); // 쿠폰 아이디 중복 체크

        /**
         * productPrice - couponDiscountPrice 가격을 모두 더하여 totalProductPrice 값을 만든다.
         * orderProduct 생성
         * */
        for (Map<String, Object> item : items) {
            double orderPrice = (double) item.get("orderPrice");

            Map<String, Object> storeOrderProduct = new HashMap<>();

            storeOrderProduct.put("orderSheetId", responseMap.get("ordrIdxx"));                                     //주문서 아이디
            storeOrderProduct.put("productId", JsonUtils.getValue(item, "productId.value"));                   //상품 아이디
            storeOrderProduct.put("baseOptionItemId", JsonUtils.getValue(item, "baseOptionItemId.value"));     //기본옵션 아이템 아이디
            storeOrderProduct.put("baseOptionItemName", JsonUtils.getValue(item, "baseOptionItemId.label"));   //기본옵션 아이템명
            storeOrderProduct.put("quantity", item.get("quantity"));                                                //수량
            storeOrderProduct.put("salePrice", item.get("salePrice"));                                              //판매가
            storeOrderProduct.put("baseAddPrice", item.get("baseAddPrice"));                                        //기본옵션추가금액
            storeOrderProduct.put("productPrice", item.get("productPrice"));                                        //상품금액
            storeOrderProduct.put("totalAddOptionPrice", item.get("totalAddOptionPrice"));                          //추가옵션금액
            storeOrderProduct.put("orderPrice", item.get("orderPrice"));                                            //주문금액
            storeOrderProduct.put("orderStatus", responseMap.get("orderStatus"));                                   //주문상태
            storeOrderProduct.put("vendorId", item.get("vendorId"));                                                //벤더사 아이디
            storeOrderProduct.put("purchasePhoneNo", "");                                                           //유가증권 구매 전화번호


            String couponId = String.valueOf(couponIds.get("tempOrderProductId"));

            List<Map<String, Object>> applicableCoupons = (List<Map<String, Object>>) item.get("applicableCoupons");
            for (Map<String, Object> applicableCoupon : applicableCoupons) {
                if (couponId.equals(applicableCoupon.get("couponId"))) {
                    storeOrderProduct.put("couponId", applicableCoupon.get("couponId"));                     //쿠폰 아이디
                    storeOrderProduct.put("discountPrice", applicableCoupon.get("discountPrice"));           //쿠폰 할인금액

                    orderPrice = orderPrice - (double) applicableCoupon.get("discountPrice");
                    couponDiscountPrice = couponDiscountPrice + (double) applicableCoupon.get("discountPrice");
                }
            }
            Node orderProductNode = (Node) nodeService.executeNode(storeOrderProduct, "orderProduct", CREATE);

            orderProductIds.add(orderProductNode.getId()); //orderDeliveryPrice | orderProductIds 에 넣기 위하여.
            /**
             * orderProductItem 생성
             * */

            List<Map<String, Object>> tempOrderProductItemList = nodeBindingService.list("tempOrderProductItem", "tempOrderProductId_in=".concat(String.valueOf(item.get("tempOrderProductId"))));
            for (Map<String, Object> tempOrderProductItem : tempOrderProductItemList) {
                Map<String, Object> storeOrderProductItem = new HashMap<>();

                storeOrderProductItem.put("orderSheetId", responseMap.get("ordrIdxx"));
                storeOrderProductItem.put("orderProductId", orderProductNode.getId());
                storeOrderProductItem.put("productId", tempOrderProductItem.get("productId"));
                storeOrderProductItem.put("addOptionItemId", tempOrderProductItem.get("addOptionItemId"));
//                storeOrderProductItem.put("addOptionItemName", tempOrderProductItem.get(""));
                storeOrderProductItem.put("quantity", tempOrderProductItem.get("quantity"));
                storeOrderProductItem.put("addOptionPrice", tempOrderProductItem.get("addOptionPrice"));

                Node orderProductItemNode = (Node) nodeService.executeNode(storeOrderProductItem, "orderProductItem", CREATE);
            }
            totalProductPrice = totalProductPrice + orderPrice;
        }
        totalDiscountPrice = totalYPoint + totalWelfarePoint + couponDiscountPrice; // 총 할인액
        totalOrderPrice = totalProductPrice - totalYPoint - totalWelfarePoint + totalDeliveryPrice; //총 주문금액

        /**
         * 최종으로 totalOrderPrice 와 totalPaymentPrice 을 체크 및 쿠폰 중복 체크.
         * 성공 시, bSucc = "true"
         * 실패 시, bSucc = "false"
         * */


//        List<Map<String, Object>> tempOrderProductList = nodeBindingService.list("tempOrderProduct", "tempOrderId_in=" + tempOrder.get("tempOrderId"));
//
//        for (Map<String, Object> tempOrderProduct : tempOrderProductList) {
//            totalOrderPrice += (double) tempOrderProduct.get("orderPrice");
//        }


//        orderSheet.put("sessionId", tempOrder.get("sessionId"));
        storeOrderSheet.put("orderSheetId", responseMap.get("ordrIdxx"));   //주문서 번호
        storeOrderSheet.put("cartId", tempOrder.get("cartId"));              //카트 아이디
        storeOrderSheet.put("memberNo", tempOrder.get("memberNo"));          //회원번호
        storeOrderSheet.put("siteId", tempOrder.get("siteId"));              //사이트 아이디
        storeOrderSheet.put("totalProductPrice", totalProductPrice);         //총상품가격
        storeOrderSheet.put("totalOrderPrice", totalOrderPrice);             //총주문가격
        storeOrderSheet.put("totalDiscountPrice", totalDiscountPrice);       //총할인액
        storeOrderSheet.put("totalDeliveryPrice", totalDeliveryPrice);       //총배송비
        storeOrderSheet.put("totalPaymentPrice", totalPaymentPrice);         //결제금액
        storeOrderSheet.put("couponDiscountPrice", couponDiscountPrice);     //쿠폰 할인액
        storeOrderSheet.put("totalWelfarePoint", totalWelfarePoint);         //사용한 복지포인트
        storeOrderSheet.put("totalYPoint", totalYPoint);                     //사용한 Y포인트
        storeOrderSheet.put("purchaseaAgreementYn", "y");
        storeOrderSheet.put("purchaseDeviceType", tempOrder.get(""));

        nodeService.executeNode(storeOrderSheet, "orderSheet", CREATE);


        storeOrderDeliveryPrice.put("orderSheetId", responseMap.get("ordrIdxx"));
        storeOrderDeliveryPrice.put("orderProductIds", StringUtils.join(orderProductIds, ","));
        storeOrderDeliveryPrice.put("vendorId", StringUtils.join(orderProductIds, ","));

        boolean saveDelivery = createDelivery(responseMap); // 배송지 저장

        if (totalOrderPrice == totalPaymentPrice && !duplicated && saveDelivery) {
            bSucc = "true";
        } else {
            bSucc = "false";
        }
        return bSucc;
    }

    /**
     * 결제 정보를 저장하는 Method.
     */
    public String createPayment(Map<String, Object> responseMap) {
        Node node = (Node) nodeService.executeNode(responseMap, "payment", CREATE);
        return node.getId();
    }


    /**
     * 주문서 배송지를 저장하는 Method.
     */
    public boolean createDelivery(Map<String, Object> responseMap) {

        boolean result = false;
        Map<String, Object> storeRefineDelivery = new HashMap<>();

        String address = String.valueOf(responseMap.get("shippingAddress")).concat(" ").concat(String.valueOf(responseMap.get("shippingDetailedAddress")));

        storeRefineDelivery.put("orderSheetId", responseMap.get("ordrIdxx"));
        storeRefineDelivery.put("addressName", responseMap.get("addressName"));
        storeRefineDelivery.put("address", address);
        storeRefineDelivery.put("cellphone", responseMap.get("shippingCellPhone"));
        storeRefineDelivery.put("phone", responseMap.get("shippingPhone"));
        storeRefineDelivery.put("deliveryMemo", responseMap.get("deliveryMemo"));
        storeRefineDelivery.put("postCode", responseMap.get("postCode"));
        storeRefineDelivery.put("recipient", responseMap.get("recipient"));
        storeRefineDelivery.put("deliveryType", responseMap.get("deliveryType"));

        nodeService.executeNode(storeRefineDelivery, "delivery", CREATE);

        String myDeliveryAddressId = String.valueOf(responseMap.get("myDeliveryAddressId"));

        if (responseMap.get("addMyDeliveryAddress").equals("on")) {       //주소록 추가

            Map<String, Object> storeMyDeliveryAddress = new HashMap<>();

            storeMyDeliveryAddress.put("memberNo", responseMap.get("memberNo"));
            storeMyDeliveryAddress.put("siteId", responseMap.get("siteId"));
            storeMyDeliveryAddress.put("addressName", responseMap.get("addressName"));
            storeMyDeliveryAddress.put("postCode", responseMap.get("postCode"));
            storeMyDeliveryAddress.put("address", responseMap.get("shippingAddress"));
            storeMyDeliveryAddress.put("detailedAddress", responseMap.get("shippingDetailedAddress"));
            storeMyDeliveryAddress.put("cellphone", responseMap.get("shippingCellPhone"));
            storeMyDeliveryAddress.put("phone", responseMap.get("phone"));
            if (responseMap.get("changeDefaultAddress").equals("on")) {   //기본 배송지

                List<Node> myDeliveryAddressNodeList = nodeService.getNodeList("myDeliveryAddress", "defaultYn_matching=y");
                Node myDeliveryAddressNode = myDeliveryAddressNodeList.get(0);
                myDeliveryAddressNode.put("defaultYn", "n");
                nodeService.updateNode(myDeliveryAddressNode, "myDeliveryAddress");

                storeMyDeliveryAddress.put("defaultYn", "y");

            } else {

                storeMyDeliveryAddress.put("defaultYn", "n");
            }
            nodeService.executeNode(storeMyDeliveryAddress, "myDeliveryAddress", CREATE);

            result = true;

        } else {
            if (responseMap.get("changeDefaultAddress").equals("on")) {   //기본 배송지
                List<Node> myDefaultDeliveryAddressNodeList = nodeService.getNodeList("myDeliveryAddress", "defaultYn_matching=y");
                Node myDefaultDeliveryAddressNode = myDefaultDeliveryAddressNodeList.get(0);
                myDefaultDeliveryAddressNode.put("defaultYn", "n");
                nodeService.updateNode(myDefaultDeliveryAddressNode, "myDeliveryAddress");

                List<Node> myDeliveryAddressNodeList = nodeService.getNodeList("myDeliveryAddress", "myDeliveryAddressId_matching=".concat(myDeliveryAddressId));
                Node myDeliveryAddressNode = myDeliveryAddressNodeList.get(0);
                myDeliveryAddressNode.put("defaultYn", "y");
                nodeService.updateNode(myDeliveryAddressNode, "myDeliveryAddress");
                result = true;
            }
        }
        return result;
    }


    /**
     * PG return 데이터를 저장하는  Method.(리턴 값을 가공하지 JsonString 으로 저장, 일종의 Backup Data)
     */
    public void createPgResponse(Map<String, Object> responseMap, String paymentId) {
        Map<String, Object> storePg = new HashMap<>();

        String JsonString = JsonUtils.toJsonString(responseMap);
        String orderSheetId = String.valueOf(responseMap.get("ordrIdxx"));

        storePg.put("paymentId", paymentId);
        storePg.put("orderSheetId", orderSheetId);
        storePg.put("jsonResponse", JsonString);

        nodeService.executeNode(storePg, "pg", CREATE);

    }

    /**
     *
     */

    private void createTempOrder(Map<String, Object> data) throws IOException {

        Map<String, Object> storeTempOrder = new HashMap<>();
        storeTempOrder.put("tempOrderId", orderNumberGenerator());
        Node tempOrderNode = (Node) nodeService.executeNode(storeTempOrder, "tempOrder", CREATE);

        String tempOrderId = tempOrderNode.getId();

        createTempOrderProduct(tempOrderId, data);
    }

    /**
     * 임시 주문서 생성 Method
     */
    private void createTempOrder(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        Map<String, Object> storeTempOrder = new HashMap<>();
        Map<String, Object> referencedCartDeliveryPrice = null;

        String cartId = String.valueOf((JsonUtils.parsingJsonToMap((String) data.get("item"))).get("cartId"));
        storeTempOrder.put("cartId", cartId);
        storeTempOrder.put("tempOrderId", orderNumberGenerator());

        try {
            referencedCartDeliveryPrice = JsonUtils.parsingJsonToMap((String) data.get("referencedCartDeliveryPrice"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> cartDeliveryPriceList = (List<Map<String, Object>>) referencedCartDeliveryPrice.get("items");

        Node tempOrderNode = (Node) nodeService.executeNode(storeTempOrder, "tempOrder", CREATE);

        createTempOrderProduct(tempOrderNode.getId(), cartDeliveryPriceList, data.get("productIds")); // 임시 주문서 상품목록 Maker
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("tempOrderId", tempOrderNode.getId());
        context.setResult(CommonService.getResult("O0001", extraData)); // 성공 시
    }

    private void createTempOrderProduct(String tempOrderId, Map<String, Object> data) {
        try {
            List<Map<String, Object>> productList = JsonUtils.parsingJsonToList(String.valueOf(data.get("product")));

            for (Map<String, Object> product : productList) {
                String productId = (String) JsonUtils.getValue(product, "productId");
                String baseOptionItemId = (String) JsonUtils.getValue(product, "baseOptionItemId");
                String quantity = (String) JsonUtils.getValue(product, "quantity");
                List<Map<String, Object>> productItemList = (List<Map<String, Object>>) JsonUtils.getValue(product, "productItem");
                List<Map<String, Object>> storeProductItemList = new ArrayList<>();
                Map<String, Object> storeTempOrderProduct = new HashMap<>();

                Node productNode = nodeService.getNode("product", productId);
                Node productBaseOptionItemNode = nodeService.getNode("productOptionItem", String.valueOf(baseOptionItemId));

                double baseAddPrice = (double) productBaseOptionItemNode.get("addPrice");
                double salePrice = (double) productNode.getBindingValue("salePrice");
                double productPrice = baseAddPrice + salePrice;
                double totalAddOptionPrice = 0;

                storeTempOrderProduct.put("tempOrderId", tempOrderId);
                storeTempOrderProduct.put("productId", productId);
                storeTempOrderProduct.put("baseOptionItemId", baseOptionItemId);
                storeTempOrderProduct.put("quantity", quantity);
                storeTempOrderProduct.put("salePrice", salePrice);
                storeTempOrderProduct.put("vendorId", productNode.getBindingValue("vendorId"));


                if (productItemList == null && productItemList.size() != 0) {
                    for (Map<String, Object> productItem : productItemList) {
                        Map<String, Object> storeTempOrderProductItem = new HashMap<>();

                        productId = (String) JsonUtils.getValue(productItem, "productId");
                        String addOptionItemId = (String) JsonUtils.getValue(productItem, "addOptionItemId");
                        quantity = (String) JsonUtils.getValue(productItem, "addOptionItemId");

                        Node productAddOptionItemNode = nodeService.getNode("productOptionItem", String.valueOf(addOptionItemId));

                        double addOptionPrice = (double) productAddOptionItemNode.get("addPrice");

                        storeTempOrderProductItem.put("tempOrderId", tempOrderId);
//                        storeTempOrderProductItem.put("tempOrderProductId", );
                        storeTempOrderProductItem.put("productId", productId);
                        storeTempOrderProductItem.put("addOptionItemId", addOptionItemId);
                        storeTempOrderProductItem.put("quantity", quantity);
                        storeTempOrderProductItem.put("addOptionPrice", addOptionPrice);

                        totalAddOptionPrice += addOptionPrice;

                        storeProductItemList.add(storeTempOrderProductItem);
                    }
                }

                storeTempOrderProduct.put("baseAddPrice", baseAddPrice);
                storeTempOrderProduct.put("productPrice", productPrice);
                storeTempOrderProduct.put("totalAddOptionPrice", totalAddOptionPrice);

                double orderPrice = productPrice + totalAddOptionPrice;

                storeTempOrderProduct.put("orderPrice", orderPrice);

                Node tempOrderProductNode = (Node) nodeService.executeNode(storeTempOrderProduct, "tempOrderProduct", CREATE);

                for (Map<String, Object> storeTempOrderProductItem : storeProductItemList) {
                    storeTempOrderProductItem.put("tempOrderProductId", tempOrderProductNode.getId());
                    nodeService.executeNode(storeTempOrderProductItem, "tempOrderProductItem", CREATE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createTempOrderProduct(Object tempOrderId, List<Map<String, Object>> cartDeliveryPriceList, Object reqSelectCartProductIds) {
        for (Map<String, Object> cartDeliveryPrice : cartDeliveryPriceList) {
            Map<String, Object> storeTempOrderDeliveryPrice = new HashMap<>();
            List<String> tempOrderProductIds = new ArrayList<>();
            storeTempOrderDeliveryPrice.put("tempOrderId", tempOrderId);
            storeTempOrderDeliveryPrice.put("vendorId", JsonUtils.getValue(cartDeliveryPrice, "vendorId.value"));
            storeTempOrderDeliveryPrice.put("deliveryPrice", cartDeliveryPrice.get("deliveryPrice"));
            storeTempOrderDeliveryPrice.put("bundleDeliveryYn", ((Map<String, Object>) cartDeliveryPrice.get("bundleDeliveryYn")).get("value"));
            storeTempOrderDeliveryPrice.put("deliveryMethod", JsonUtils.getValue(cartDeliveryPrice, "deliveryMethod.value"));
            storeTempOrderDeliveryPrice.put("deliveryPriceType", JsonUtils.getValue(cartDeliveryPrice, "deliveryPriceType.value"));
            storeTempOrderDeliveryPrice.put("deliveryDateType", JsonUtils.getValue(cartDeliveryPrice, "deliveryDateType.value"));
            storeTempOrderDeliveryPrice.put("hopeDeliveryDate", cartDeliveryPrice.get("hopeDeliveryDate"));
            storeTempOrderDeliveryPrice.put("scheduledDeliveryDate", cartDeliveryPrice.get("scheduledDeliveryDate"));

            /**
             * 선택한 상품이 없을 땐 상품 전체를 선택했다고 가정하고 카트 전체 상품 아이디를 가져온다.
             */

            List<String> selectCartProductIds = new ArrayList<>();

            if (reqSelectCartProductIds != null) {
                selectCartProductIds = Arrays.asList(String.valueOf(reqSelectCartProductIds).split(",")); // 카트에서 선택한 상품 리스트
            }

            if (selectCartProductIds.size() == 0) {

                List<Map<String, String>> cartProductIds = (List<Map<String, String>>) cartDeliveryPrice.get("cartProductIds");
                for (Map<String, String> cartProductId : cartProductIds) {
                    selectCartProductIds.add(cartProductId.get("value"));
                }
            }

            List<Map<String, Object>> referencedCartProduct = (List<Map<String, Object>>) cartDeliveryPrice.get("referencedCartProduct");

            for (Map<String, Object> cartProduct : referencedCartProduct) {

                Map<String, Object> storeTempOrderProduct = new HashMap<>();

                Map<String, Object> calc = (Map<String, Object>) cartProduct.get("calculateItem");

                /**
                 * 선택상품목록과 장바구니상품목록을 비교하면서 넣어준다.
                 */
                for (String selectProductId : selectCartProductIds) {
                    if (!selectProductId.equals(String.valueOf(calc.get("cartProductId")))) {
                        continue;
                    } else {
                        storeTempOrderProduct.put("tempOrderId", tempOrderId);
                        storeTempOrderProduct.put("productId", calc.get("productId"));
                        storeTempOrderProduct.put("baseOptionItemId", calc.get("baseOptionItemId"));
                        storeTempOrderProduct.put("quantity", calc.get("quantity"));
                        storeTempOrderProduct.put("salePrice", calc.get("salePrice"));
                        storeTempOrderProduct.put("baseAddPrice", calc.get("baseAddPrice"));
                        storeTempOrderProduct.put("productPrice", calc.get("productPrice"));
                        storeTempOrderProduct.put("totalAddOptionPrice", calc.get("totalAddOptionPrice"));
                        storeTempOrderProduct.put("orderPrice", calc.get("orderPrice"));
                        storeTempOrderProduct.put("vendorId", calc.get("vendorId"));

                        Node tempOrderProductNode = (Node) nodeService.executeNode(storeTempOrderProduct, "tempOrderProduct", CREATE);
                        tempOrderProductIds.add(tempOrderProductNode.getId());
                        List<Map<String, Object>> referencedCartProductItem = (List<Map<String, Object>>) cartProduct.get("referencedCartProductItem");

                        for (Map<String, Object> tempOrderProductItem : referencedCartProductItem) {
                            Map<String, Object> storeTempOrderProductItem = new HashMap<>();
                            storeTempOrderProductItem.put("tempOrderId", tempOrderId);
                            storeTempOrderProductItem.put("tempOrderProductId", tempOrderProductNode.getId());
                            storeTempOrderProductItem.put("productId", JsonUtils.getValue(tempOrderProductItem, "productId.value"));
                            storeTempOrderProductItem.put("addOptionItemId", JsonUtils.getValue(tempOrderProductItem, "addOptionItemId.value"));
                            storeTempOrderProductItem.put("quantity", tempOrderProductItem.get("quantity"));
                            storeTempOrderProductItem.put("addOptionPrice", JsonUtils.getValue(tempOrderProductItem, "addOptionItemId.item.addPrice"));

                            nodeService.executeNode(storeTempOrderProductItem, "tempOrderProductItem", CREATE);
                        }
                    }
                }

            }
            storeTempOrderDeliveryPrice.put("tempOrderProductIds", StringUtils.join(tempOrderProductIds.toArray(), ","));
            nodeService.executeNode(storeTempOrderDeliveryPrice, "tempOrderDeliveryPrice", CREATE);
        }
    }

    /**
     * 쿠폰 중복 체크 Method.
     */
    private boolean repetitionCheck(Collection<Object> ids) {
        List<Integer> idList = new ArrayList(ids);
        boolean duplication = false;
        for (int i = 0; i < idList.size(); i++) {
            for (int j = 1; j < idList.size(); j++) {
                if (idList.get(i) == idList.get(j)) {
                    duplication = true;
                }
            }
            if (duplication) {
                break;
            }
        }
        return duplication;
    }

    /**
     * 포인트 차감 Method.
     */
    private boolean pointsDeduction(String memberNo) {
        return false;
    }

    /**
     * 주문번호 생성 Method.
     */
    private String orderNumberGenerator() {
        Random random = new Random();

        String appendNumber1 = String.valueOf(random.nextInt(10));
        String appendNumber2 = String.valueOf(random.nextInt(10));
        String appendNumber3 = String.valueOf(random.nextInt(10));

        String orderNumber = "B";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.KOREA);
        Date currentTime = new Date();
        String time = simpleDateFormat.format(currentTime);

        orderNumber = orderNumber.concat(time).concat(appendNumber1).concat(appendNumber2).concat(appendNumber3);

        return orderNumber;
    }

    private void pointDeduction(String yPoin) {

    }

    /**
     * 주문 성공시 임시 주문서를 삭제하는 Method.
     * 일정 시간이 지난 주문서도 삭제 한다.
     */
    public void removeTempOrder() {

    }

}
