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
     * PG 호출 전에 검증하는 Method.
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

            boolean duplicated = duplication(couponIds.values());// 쿠폰 아이디 중복 체크


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
                context.setResult(CommonService.getResult("O0004"));      // 검증성공
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 결제 후 최종적으로 한번 더 검증하여 주문서를 생성하는 Method.
     */
    public String addOrderSheet(Map<String, Object> responseMap) {
        String bSucc;
        Map<String, Object> storeOrderSheet = new HashMap<>();
        Map<String, Object> storeOrderProduct = new HashMap<>();
        Map<String, Object> storeOrderProductItem = new HashMap<>();
        Map<String, Object> storeOrderDeliveryPrice = new HashMap<>();

        Map<String, Object> tempOrder = nodeBindingService.getNodeBindingInfo("tempOrder").retrieve(String.valueOf(responseMap.get("ordrIdxx")));
        List<Map<String, Object>> tempOrderDeliveryPriceList = nodeBindingService.list("tempOrderDeliveryPrice", "tempOrderId_in=".concat(String.valueOf(responseMap.get("ordrIdxx"))));

        double totalProductPrice = 0;
        double totalDeliveryPrice = Double.parseDouble(String.valueOf(tempOrderDeliveryPriceList.get(0).get("deliveryPrice")));
        double totalDiscountPrice;
        double totalOrderPrice;
        double couponDiscountPrice = 0;
        double totalPaymentPrice = Double.parseDouble(String.valueOf(responseMap.get("amount")));
        double totalWelfarePoint = Double.parseDouble(String.valueOf(responseMap.get("useWelfarepoint")));
        double totalYPoint = Double.parseDouble(String.valueOf(responseMap.get("useYPoint")));

        Map<String, Object> summaryResponse = getSummary((String) responseMap.get("memberNo"));

        double useableYPoint = (double) ((Map<String, Object>) summaryResponse.get("item")).get("useableYPoint");
        double useableWelfarepoint = (double) ((Map<String, Object>) summaryResponse.get("item")).get("useableWelfarepoint");

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
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Object> couponResponse = getCoupon((String) responseMap.get("memberNo"), (String) responseMap.get("ordrIdxx"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) couponResponse.get("items"); //상품 정보

        boolean duplicated = duplication(couponIds.values()); // 쿠폰 아이디 중복 체크 메소드

        /**
         * productPrice - couponDiscountPrice 가격을 모두 더하여 totalProductPrice 값을 만든다.
         * orderProduct 생성
         * */
        for (Map<String, Object> item : items) {
            double orderPrice = (double) item.get("orderPrice");

            storeOrderProduct.put("orderSheetId", responseMap.get("ordrIdxx"));                              //주문서 아이디
            storeOrderProduct.put("productId", ((Map) item.get("productId")).get("value"));                  //상품 아이디
            storeOrderProduct.put("baseOptionItemId", ((Map) item.get("baseOptionItemId")).get("value"));    //기본옵션 아이템 아이디
            storeOrderProduct.put("baseOptionItemName", ((Map) item.get("baseOptionItemId")).get("label"));  //기본옵션 아이템명
            storeOrderProduct.put("quantity", item.get("quantity"));                                         //수량
            storeOrderProduct.put("salePrice", item.get("salePrice"));                                       //판매가
            storeOrderProduct.put("baseAddPrice", item.get("baseAddPrice"));                                 //기본옵션추가금액
            storeOrderProduct.put("productPrice", item.get("productPrice"));                                 //상품금액
            storeOrderProduct.put("totalAddOptionPrice", item.get("totalAddOptionPrice"));                   //추가옵션금액
            storeOrderProduct.put("orderPrice", item.get("orderPrice"));                                     //주문금액
            storeOrderProduct.put("orderStatus", responseMap.get("orderStatus"));                            //주문상태
            storeOrderProduct.put("vendorId", item.get("vendorId"));                                         //벤더사 아이디
            storeOrderProduct.put("purchasePhoneNo", "");                                                    //유가증권 구매 전화번호


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

            /**
             * orderProductItem 생성
             * */
            logger.info("123");

            List<Map<String, Object>> tempOrderProductItemList = nodeBindingService.list("tempOrderProductItem", "tempOrderProductId_in=".concat((String) item.get("tempOrderProductId")));

            for (Map<String, Object> tempOrderProductItem : tempOrderProductItemList) {
                storeOrderProductItem.put("orderSheetId", responseMap.get("ordrIdxx"));
                storeOrderProductItem.put("orderProductId", orderProductNode.getId());
                storeOrderProductItem.put("productId", tempOrderProductItem.get("productId"));
                storeOrderProductItem.put("addOptionItemId", tempOrderProductItem.get("addOptionItemId"));
                storeOrderProductItem.put("addOptionItemName", tempOrderProductItem.get(""));
                storeOrderProductItem.put("quantity", tempOrderProductItem.get(""));
                storeOrderProductItem.put("addOptionPrice", tempOrderProductItem.get(""));

                nodeService.executeNode(storeOrderProductItem, "orderProductItem", CREATE);
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
        storeOrderSheet.put("orderSheetId", tempOrder.get("tempOrderId"));   //주문서 번호
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
        saveDelivery(responseMap);


        if (totalOrderPrice != totalPaymentPrice && !duplicated) {
            bSucc = "false";
            return bSucc;
        } else {
            bSucc = "true";
        }
        return bSucc;
    }

    /**
     * 결제 정보를 저장하는 Method.
     */
    public String savePayment(Map<String, Object> responseMap) {
        Node node = (Node) nodeService.executeNode(responseMap, "payment", CREATE);
        logger.info("123123");
        return node.getId();
    }

    /**
     * 주문서 배송지를 저장하는 Method.
     */
    public void saveDelivery(Map<String, Object> responseMap) {
        Map<String, Object> storeRefineDelivery = new HashMap<>();

        storeRefineDelivery.put("orderSheetId", responseMap.get("ordrIdxx"));
        storeRefineDelivery.put("addressName", responseMap.get("addressName"));
        storeRefineDelivery.put("address", responseMap.get("shippingAddress"));
        storeRefineDelivery.put("cellphone", responseMap.get("shippingCellPhone"));
        storeRefineDelivery.put("phone", responseMap.get("shippingPhone"));
        storeRefineDelivery.put("deliveryMemo", responseMap.get("deliveryMemo"));
        storeRefineDelivery.put("postCode", responseMap.get("postCode"));
        storeRefineDelivery.put("recipient", responseMap.get("recipient"));
        storeRefineDelivery.put("deliveryType", responseMap.get("deliveryType"));

        nodeService.executeNode(storeRefineDelivery, "delivery", CREATE);
        logger.info("saveDelivery end");
    }


    /**
     * PG return 데이터를 저장하는  Method.(리턴 값을 가공하지 JsonString 으로 저장, 일종의 Backup Data)
     */
    public void savePgResponse(Map<String, Object> responseMap, String paymentId) {
        Map<String, Object> storePg = new HashMap<>();

        String JsonString = JsonUtils.toJsonString(responseMap);
        String orderSheetId = String.valueOf(responseMap.get("ordrIdxx"));

        storePg.put("paymentId", paymentId);
        storePg.put("orderSheetId", orderSheetId);
        storePg.put("jsonResponse", JsonString);

        nodeService.executeNode(storePg, "pg", CREATE);

    }

    /**
     * 임시 주문서 생성 Method
     */
    private void saveTempOrder(ExecuteContext context) throws IOException {
        Map<String, Object> data = context.getData();
        Map<String, Object> storeTempOrder = new HashMap<>();
        Map<String, Object> referencedCartDeliveryPrice = null;

        List<String> selectProductIds = Arrays.asList(String.valueOf(data.get("productIds")).split(",")); // 카트에서 선택한 상품 리스트

        String cartId = String.valueOf((JsonUtils.parsingJsonToMap((String) data.get("item"))).get("cartId"));
        storeTempOrder.put("cartId", cartId);

        try {
            referencedCartDeliveryPrice = JsonUtils.parsingJsonToMap((String) data.get("referencedCartDeliveryPrice"));

        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Map<String, Object>> cartDeliveryPriceList = (List<Map<String, Object>>) referencedCartDeliveryPrice.get("items");

        Node tempOrderNode = (Node) nodeService.executeNode(storeTempOrder, "tempOrder", CREATE);

        makeTempOrderProductData(tempOrderNode.getId(), cartDeliveryPriceList, selectProductIds); // 임시 주문서 상품목록 Maker
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("tempOrderId", tempOrderNode.getId());
        context.setResult(CommonService.getResult("O0001", extraData)); // 성공 시
    }

    private void makeTempOrderProductData(Object tempOrderId, List<Map<String, Object>> cartDeliveryPriceList, List<String> selectProductIds) {
        for (Map<String, Object> cartDeliveryPrice : cartDeliveryPriceList) {
            Map<String, Object> storeTempOrderDeliveryPrice = new HashMap<>();
            List<String> tempOrderProductIds = new ArrayList<>();
            storeTempOrderDeliveryPrice.put("tempOrderId", tempOrderId);
            storeTempOrderDeliveryPrice.put("vendorId", ((Map<String, Object>) cartDeliveryPrice.get("vendorId")).get("value"));
            storeTempOrderDeliveryPrice.put("deliveryPrice", cartDeliveryPrice.get("deliveryPrice"));
            storeTempOrderDeliveryPrice.put("bundleDeliveryYn", ((Map<String, Object>) cartDeliveryPrice.get("bundleDeliveryYn")).get("value"));
            storeTempOrderDeliveryPrice.put("deliveryMethod", cartDeliveryPrice.get("deliveryMethod"));
            storeTempOrderDeliveryPrice.put("deliveryPriceType", cartDeliveryPrice.get("deliveryPriceType"));
            storeTempOrderDeliveryPrice.put("hopeDeliveryDate", cartDeliveryPrice.get("hopeDeliveryDate"));

            /**
             * 선택한 상품이 없을 땐 상품 전체를 선택했다고 가정하고 카트 전체 상품 아이디를 가져온다.
             */
            if (selectProductIds.size() == 0) {
                List<Map<String, String>> cartProductIds = (List<Map<String, String>>) cartDeliveryPrice.get("cartProductIds");
                for (Map<String, String> ccartProductId : cartProductIds) {
                    selectProductIds.add(ccartProductId.get("value"));
                }
            }

            List<Map<String, Object>> referencedCartProduct = (List<Map<String, Object>>) cartDeliveryPrice.get("referencedCartProduct");

            for (Map<String, Object> cartProduct : referencedCartProduct) {

                Map<String, Object> storeTempOrderProduct = new HashMap<>();

                Map<String, Object> calc = (Map<String, Object>) cartProduct.get("calculateItem");

                /**
                 * 선택상품목록과 장바구니상품목록을 비교하면서 넣어준다.
                 */
                for (String selectProductId : selectProductIds) {
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
                            storeTempOrderProductItem.put("productId", ((Map<String, Object>) tempOrderProductItem.get("productId")).get("value"));
                            storeTempOrderProductItem.put("addOptionItemId", ((Map<String, Object>) tempOrderProductItem.get("addOptionItemId")).get("value"));
                            storeTempOrderProductItem.put("quantity", tempOrderProductItem.get("quantity"));

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
    private boolean duplication(Collection<Object> ids) {
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
     * 주문 성공시 임시 주문서를 삭제하는 Method.
     * 일정 시간이 지난 주문서도 삭제 한다.
     */
    public void removeTempOrder() {

    }

}
