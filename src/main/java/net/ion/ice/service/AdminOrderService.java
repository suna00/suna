package net.ion.ice.service;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.api.ApiUtils;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.context.ReadContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.*;
import net.ion.ice.core.session.SessionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "sweettracker")
@Service("adminOrderService")
public class AdminOrderService {

    private static Logger logger = LoggerFactory.getLogger(AdminOrderService.class);

    public static final String orderSheet_TID = "orderSheet";
    public static final String orderProduct_TID = "orderProduct";
    public static final String orderDeliveryPrice_TID = "orderDeliveryPrice";
    public static final String delivery_TID = "delivery";
    public static final String deliveryTrackingInfo_TID = "deliveryTrackingInfo";

    public static final String commonResource_TID = "commonResource";

    public static final String orderChange_TID = "orderChange";
    public static final String orderChangeProduct_TID = "orderChangeProduct";

    @Autowired
    private SessionService sessionService;
    @Autowired
    private NodeBindingService nodeBindingService;
    @Autowired
    private NodeService nodeService;
    CommonService commonService;

    @Autowired
    private MypageOrderService mypageOrderService;

    private String trackingApi;
    private String tier;
    private String key;
    private String callbackUrl;


    public String getTrackingApi() {
        return trackingApi;
    }

    public void setTrackingApi(String trackingApi) {
        this.trackingApi = trackingApi;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }


    //주문리스트
    public ExecuteContext getAdminOrderList(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        int pageSize = (JsonUtils.getIntValue(data, "pageSize") == 0 ? 10 : JsonUtils.getIntValue(data, "pageSize"));
        int page = JsonUtils.getIntValue(data, "page");
        int currentPage = (page == 0 ? 1 : page);
        String createdFromto = JsonUtils.getStringValue(data, "createdFromto");
        String orderSheetId = JsonUtils.getStringValue(data, "orderSheetId");
        String productId = JsonUtils.getStringValue(data, "productId");
        String orderStatus = JsonUtils.getStringValue(data, "orderStatus");
        List<String> splitOrderStatusList = Arrays.asList(StringUtils.split(orderStatus, ","));
        List<String> inOrderStatusList = new ArrayList<>();
        for (String splitOrderStatus : splitOrderStatusList) {
            inOrderStatusList.add(String.format("'%s'", splitOrderStatus));
        }

        String existsQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct where IF(@{productId} = '' ,'1',productId) = IF(@{productId} = '' ,'1',@{productId}) and orderStatus in ("+StringUtils.join(inOrderStatusList, ",")+")";

        List<String> search = new ArrayList<>();
        search.add("pageSize="+pageSize);
        search.add("page="+currentPage);
        search.add("sorting=created desc");
        search.add("referenceView=memberNo");
        if (!StringUtils.isEmpty(orderSheetId)) search.add("orderSheetId_equals="+orderSheetId);
        if (!StringUtils.isEmpty(orderStatus)) search.add("orderSheetId_exists="+existsQuery);
        if (!StringUtils.isEmpty(createdFromto)) search.add("created_fromto="+createdFromto);

        String searchText = StringUtils.join(search, "&");

//        String searchText = "pageSize=" + pageSize +
//                "&page=" + currentPage +
//                "&sorting=created desc" +
//                (orderSheetId != "" ? "&orderSheetId_equals=" + orderSheetId : "") +
//                (productId != "" || orderStatus != "" ? "&orderSheetId_exists=" + existsQuery : "") +
//                (createdFromto != "" ? "&created_fromto=" + createdFromto : "") +
//                (memberNo != "" ? "&memberNo_equals=" + memberNo : "");

        List<Map<String, Object>> sheetTotalList = nodeBindingService.list(orderSheet_TID, "");


        NodeType nodeType = NodeUtils.getNodeType(orderSheet_TID);
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType, null);
        queryContext.setData(context.getData());
        NodeBindingInfo nodeBindingInfo = nodeBindingService.getNodeBindingInfo(orderSheet_TID);
        List<Map<String, Object>> sheetList = nodeBindingInfo.list(queryContext);

        ReadContext readContext = new ReadContext();

        for (Map<String, Object> sheet : sheetList) {
            Timestamp created = sheet.get("created") == null ? null : (Timestamp) sheet.get("created");
            if (created != null) sheet.put("created", FastDateFormat.getInstance("yyyyMMddHHmmss").format(created.getTime()));
            Long memberNoValue = sheet.get("memberNo") == null ? null : (Long) sheet.get("memberNo");
            if (memberNoValue == null) {
                sheet.put("memberItem", new HashMap<>());
            } else {
                Node memberNode = nodeService.getNode("member", memberNoValue.toString());
                if (memberNode == null) {
                    sheet.put("memberItem", new HashMap<>());
                } else {
                    Map<String, Object> memberItem = new HashMap<>();
                    NodeType memberNodeType = NodeUtils.getNodeType("member");
                    List<PropertyType> memberPropertyList = new ArrayList<>(memberNodeType.getPropertyTypes());
                    for (PropertyType memberProperty : memberPropertyList) {
                        if (!StringUtils.equals(memberProperty.getPid(), "password")) {
                            memberItem.put(memberProperty.getPid(), NodeUtils.getResultValue(readContext, memberProperty, memberNode));
                        }
                    }
                    sheet.put("memberItem", memberItem);
                }
            }

            List<Map<String, Object>> opList = nodeBindingService.list(orderProduct_TID, "orderSheetId_equals=" + JsonUtils.getStringValue(sheet, "orderSheetId"));
            for (Map<String, Object> op : opList) {
                Node product = NodeUtils.getNode("product", JsonUtils.getStringValue(op, "productId"));
                List<Map<String, Object>> mainImages = nodeBindingService.list(mypageOrderService.commonResource_TID, "contentsId_matching=" + product.getId() + "&tid_matching=product&name_matching=main");
                op.put("referencedMainImage", mainImages);

                Map<String, Object> orderDeliveryPrice = mypageOrderService.getOrderDeliveryPrice(JsonUtils.getStringValue(op, "orderSheetId"), JsonUtils.getStringValue(op, "orderProductId"));
                op.put("referencedOrderDeliveryPrice", orderDeliveryPrice);
                op.put("functionBtn", mypageOrderService.getFunctionBtn(orderDeliveryPrice, op, product));
                commonService.putReferenceValue("orderProduct", context, op);
            }
            sheet.put("referencedOrderProduct", opList);
            commonService.putReferenceValue("orderSheet", context, sheet);
        }

        int pageCount = (int) Math.ceil((double) sheetTotalList.size() / (double) pageSize);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("totalCount", sheetTotalList.size());
        item.put("resultCount", sheetList.size());
        item.put("pageSize", pageSize);
        item.put("pageCount", pageCount);
        item.put("currentPage", currentPage);

        item.put("items", sheetList);
        context.setResult(item);

        return context;
    }


    // 주문취소교환반품 리스트
    public void getAdminChangeList(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        int pageSize = (JsonUtils.getIntValue(data, "pageSize") == 0 ? 10 : JsonUtils.getIntValue(data, "pageSize"));
        int page = JsonUtils.getIntValue(data, "page");
        int currentPage = (page == 0 ? 1 : page);
        String createdFromto = JsonUtils.getStringValue(data, "createdFromto");
        String orderSheetId = JsonUtils.getStringValue(data, "orderSheetId");
        String productId = JsonUtils.getStringValue(data, "productId");
        String changeType = JsonUtils.getStringValue(data, "changeType");

        String existsQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderChangeProduct where IF(@{productId} = '' ,'1',productId) = IF(@{productId} = '' ,'1',@{productId}) ";

        String searchText = "pageSize=" + pageSize +
                "&page=" + currentPage +
                "&sorting=created desc" +
                (orderSheetId != "" ? "&orderSheetId_equals=" + orderSheetId : "") +
                (changeType != "" ? "&changeType_equals=" + changeType : "") +
                (productId != "" ? "&orderSheetId_exists=" + existsQuery : "") +
                (createdFromto != "" ? "&created_fromto=" + createdFromto : "");

        List<Map<String, Object>> changeTotalList = nodeBindingService.list(orderChange_TID, "");


        NodeType nodeType = NodeUtils.getNodeType(orderChange_TID);
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType, null);
        queryContext.setData(context.getData());
        NodeBindingInfo nodeBindingInfo = nodeBindingService.getNodeBindingInfo(orderChange_TID);
        List<Map<String, Object>> changeList = nodeBindingInfo.list(queryContext);

        for (Map<String, Object> change : changeList) {
            List<Map<String, Object>> opList = nodeBindingService.list(orderChangeProduct_TID, "orderChangeId_equals=" + JsonUtils.getStringValue(change, "orderChangeId"));
            for (Map<String, Object> op : opList) {
                Node product = NodeUtils.getNode("product", JsonUtils.getStringValue(op, "productId"));
                List<Map<String, Object>> mainImages = nodeBindingService.list(commonResource_TID, "contentsId_matching=" + product.getId() + "&tid_matching=product&name_matching=main");
                op.put("referencedMainImage", mainImages);
//                Map<String, Object> orderDeliveryPrice = mypageOrderService.getOrderDeliveryPrice(JsonUtils.getStringValue(op, "orderSheetId"), JsonUtils.getStringValue(op, "orderProductId"));
//                op.put("referencedOrderDeliveryPrice", orderDeliveryPrice);
//                op.put("functionBtn", mypageOrderService.getFunctionBtn(orderDeliveryPrice, op, product));
                commonService.putReferenceValue("orderProduct", context, op);
            }
            change.put("referencedOrderChangeProduct", opList);
            commonService.putReferenceValue("orderSheet", context, change);
        }

        int pageCount = (int) Math.ceil((double) changeList.size() / (double) pageSize);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("totalCount", changeTotalList.size());
        item.put("resultCount", changeList.size());
        item.put("pageSize", pageSize);
        item.put("pageCount", pageCount);
        item.put("currentPage", currentPage);

        item.put("items", changeList);
        context.setResult(item);
    }


    // N개 주문서ID check 상태변경 버튼 클릭
    public ExecuteContext multiDeliveryStatusChangeTarget(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String[] params = {"orderSheetIds"};
        if (CommonService.requiredParams(context, data, params)) return context;

        String orderSheetIds = JsonUtils.getStringValue(data, "orderSheetIds");

        List<Map<String, Object>> orderDeliveryPriceList = nodeBindingService.list(orderDeliveryPrice_TID, "orderSheetId_in=" + orderSheetIds);
        List<Map<String, Object>> orderProductList = nodeBindingService.list(orderProduct_TID, "orderSheetId_in=" + orderSheetIds);

        List<Map<String, Object>> list = deliveryStatusChangeTarget(context, orderSheetIds, orderDeliveryPriceList, orderProductList);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("items", list);
        context.setResult(item);

        return context;
    }

    // 특정 배송정보만 상태변경 버튼 클릭
    public ExecuteContext singleDeliveryStatusChangeTarget(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String[] params = {"orderSheetId", "orderDeliveryPriceId"};
        if (CommonService.requiredParams(context, data, params)) return context;

        String orderSheetId = JsonUtils.getStringValue(data, "orderSheetId");
        String orderDeliveryPriceId = JsonUtils.getStringValue(data, "orderDeliveryPriceId");

        List<Map<String, Object>> orderDeliveryPriceList = nodeBindingService.list(orderDeliveryPrice_TID, "orderDeliveryPriceId_equals=" + orderDeliveryPriceId);
        List<Map<String, Object>> orderProductList = nodeBindingService.list(orderProduct_TID, "orderSheetId_equals=" + orderSheetId);

        List<Map<String, Object>> list = deliveryStatusChangeTarget(context, orderSheetId, orderDeliveryPriceList, orderProductList);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("items", list);
        context.setResult(item);

        return context;
    }

    // 배송 상태변경 대상 리스트
    public List<Map<String, Object>> deliveryStatusChangeTarget(ExecuteContext context, String orderSheetIds, List<Map<String, Object>> orderDeliveryPriceList, List<Map<String, Object>> orderProductList) {

        List<Map<String, Object>> list = new ArrayList<>();
        for (String orderSheetId : StringUtils.split(orderSheetIds, ",")) {
            Map<String, Object> map = new LinkedHashMap<>();
            Node orderSheet = NodeUtils.getNode(orderSheet_TID, orderSheetId);
            commonService.putReferenceValue(orderSheet_TID, context, orderSheet);

            List<Map<String, Object>> dList = new ArrayList<>();
            for (Map<String, Object> orderDeliveryPrice : orderDeliveryPriceList) {
                Map<String, Object> dMap = new LinkedHashMap<>();
                dMap.putAll(orderDeliveryPrice);

                List<Map<String, Object>> pList = new ArrayList<>();
                for (Map<String, Object> orderProduct : orderProductList) {
                    Map<String, Object> pMap = new LinkedHashMap<>();
                    pMap.putAll(orderProduct);

                    String orderProductIds = ",".concat(JsonUtils.getStringValue(dMap, "orderProductIds")).concat(",");
                    if (orderProductIds.contains(JsonUtils.getStringValue(pMap, "orderProductId"))) {
                        commonService.putReferenceValue(orderProduct_TID, context, pMap);
                        pList.add(pMap);
                    }
                }
                commonService.putReferenceValue(orderDeliveryPrice_TID, context, dMap);
                dMap.put("referencedOrderProduct", pList);
                dList.add(dMap);
            }
            orderSheet.put("referencedOrderDeliveryPrice", dList);
            map.putAll(orderSheet);
            list.add(map);
        }

        return list;
    }

    /*
            order004,상품준비중
            order005,배송중
            order006,배송완료

           {
              "updateDeliveryStatusList": [
                {
                  "orderDeliveryPriceId": 4,
                  "trackingNo": "504254237701",
                  "deliveryStatus": "order004",
                  "deliveryEnterPriseId": 6,
                  "sendingDate": "20171031000000",
                },
                {
                  "orderDeliveryPriceId": 6,
                  "trackingNo": "604254237702",
                  "deliveryStatus": "order004",
                  "deliveryEnterPriseId": 6,
                  "sendingDate": "20171031000000",
                }
              ]
            }

    */
    // 배송 상태변경 처리
    public ExecuteContext updateDeliveryStatus(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String[] params = {"updateDeliveryStatusList"};
        if (CommonService.requiredParams(context, data, params)) return context;

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new LinkedHashMap<>();
        try {
            list = JsonUtils.parsingJsonToList(JsonUtils.getStringValue(data, "updateDeliveryStatusList"));
            if (list.size() > 0) {
                for (Map<String, Object> map : list) {
                    Node node = NodeUtils.getNode(orderDeliveryPrice_TID, JsonUtils.getStringValue(map, "orderDeliveryPriceId"));
                    node.putAll(map);
                    Map<String, Object> result = sweettrackerTrackingApi(map);
                    if(JsonUtils.getBooleanValue(result, "success")){
                        setDeliveryCompleteDate(node.getStringValue("deliveryStatus"), node);
                        nodeService.executeNode(node, orderDeliveryPrice_TID, CommonService.UPDATE);

                        List<Map<String, Object>> orderProductList = nodeBindingService.list(orderProduct_TID, "orderProductId_in=" + node.get("orderProductIds"));
                        updateOrderStatus(orderProductList, JsonUtils.getStringValue(map, "deliveryStatus"));

                    }else{
                        //스윗트래커 실패
                        item.putAll((Map<? extends String, ?>) CommonService.getResult("S0004"));
                        item.put("sweettracker", result);
                        context.setResult(item);
                        return context;
                    }
                }
                item.putAll((Map<? extends String, ?>) CommonService.getResult("S0002"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            item.putAll((Map<? extends String, ?>) CommonService.getResult("S0003"));
        }

        context.setResult(item);

        return context;
    }

    public Map<String, Object> sweettrackerTrackingApi(Map<String, Object> map) throws IOException {

        //스윗트래커 콜백 이력 있으면 전송안함.
        List<Map<String, Object>> deliveryTrackingInfo = nodeBindingService.list(deliveryTrackingInfo_TID, "orderDeliveryPriceId_equals=" + JsonUtils.getStringValue(map, "orderDeliveryPriceId") + "&trackingNo_equals" + JsonUtils.getStringValue(map, "trackingNo")) ;
        if(deliveryTrackingInfo.size() > 0){
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return result;
        }

        String deliveryEnterpriseId = JsonUtils.getStringValue(map, "deliveryEnterpriseId");
        Node deliveryEnterprise = NodeUtils.getNode("deliveryEnterprise", deliveryEnterpriseId);
        if (deliveryEnterprise.get("sweettrackerId") == null) return null;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("num", JsonUtils.getStringValue(map, "trackingNo"));
        data.put("code", deliveryEnterprise.get("sweettrackerId"));
        data.put("fid", JsonUtils.getStringValue(map, "orderDeliveryPriceId"));
        data.put("callback_url", this.callbackUrl);
        data.put("tier", this.tier);
        data.put("key", this.key);
        String resultStr = ApiUtils.callApiMethod(this.trackingApi, data, 5000, 10000, ApiUtils.POST);
        logger.info("sweettrackerTrackingApi - "+resultStr);
        return JsonUtils.parsingJsonToMap(resultStr);
    }

    //    주문상태변경(입금확인,상품준비중처리,배송완료 처리)
    public ExecuteContext updateOrderStatus(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String[] params = {"orderSheetIds", "orderStatus"};
        if (CommonService.requiredParams(context, data, params)) return context;

        String status = JsonUtils.getStringValue(data, "orderStatus");
        List<Map<String, Object>> orderProductList = nodeBindingService.list(orderProduct_TID, "orderSheetId_in=" + JsonUtils.getStringValue(data, "orderSheetIds"));
        updateOrderStatus(orderProductList, status); //order003,결제완료 / order004,상품준비중 / order006,배송완료
        if("order006".equals(status)){
            // orderDeliveryPrice deliveryStatus 업뎃
            List<Map<String, Object>> orderDeliveryPriceList = nodeBindingService.list(orderDeliveryPrice_TID, "orderSheetId_in=" + JsonUtils.getStringValue(data, "orderSheetIds"));
            for(Map<String, Object> map : orderDeliveryPriceList){
                map.put("deliveryStatus", status);
                setDeliveryCompleteDate(status, map);
                nodeService.executeNode(map, orderDeliveryPrice_TID, CommonService.UPDATE);
            }
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.putAll((Map<? extends String, ?>) CommonService.getResult("S0002"));
        context.setResult(item);

        return context;
    }

    public Map<String, Object> setDeliveryCompleteDate(String status, Map<String, Object> map) {
        if("order006".equals(status)){
            map.put("completeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }
        return map;
    }

    // 주문 상태변경 처리
    public void updateOrderStatus(List<Map<String, Object>> orderProductList, String deliveryStatus) {
        for (Map<String, Object> map : orderProductList) {
            map.put("orderStatus", deliveryStatus);
            nodeService.executeNode(map, orderProduct_TID, CommonService.UPDATE);
        }
    }


    //주문변경신청 상태 변경(취소교환반품)
    public ExecuteContext updateOrderChangeStatus(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String[] params = {"orderChangeId", "orderChangeStatus"};
        if (CommonService.requiredParams(context, data, params)) return context;

//
//# order009,취소완료
//# order016,교환완료
//# order021,반품완료




        return context;
    }


    //배송주문조회
    public ExecuteContext deliveryTrackingInfoView(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String[] params = {"orderDeliveryPriceId"};
        if (CommonService.requiredParams(context, data, params)) return context;

        Node node = NodeUtils.getNode(orderDeliveryPrice_TID, JsonUtils.getStringValue(data, "orderDeliveryPriceId"));
        Map<String, Object> orderDeliveryPrice = new LinkedHashMap<>();
        orderDeliveryPrice = commonService.putReferenceValue(orderDeliveryPrice_TID, context, node);
        List<Map<String, Object>> deliveryAddress = nodeBindingService.list(delivery_TID, "deliveryType_equals=deliveryAddress&orderSheetId_equals=" + orderDeliveryPrice.get("orderSheetId"));
        if(deliveryAddress.size() > 0){
            orderDeliveryPrice.put("delivery", commonService.putReferenceValue(delivery_TID, context, deliveryAddress.get(0)));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        List<Map<String, Object>> deliveryTrackingList = nodeBindingService.list(deliveryTrackingInfo_TID, "orderDeliveryPriceId_equals=" + orderDeliveryPrice.get("orderDeliveryPriceId"));
        for(Map<String, Object> map : deliveryTrackingList){
            list.add(commonService.putReferenceValue(deliveryTrackingInfo_TID, context, map));
        }
        orderDeliveryPrice.put("statusList", list);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("item", orderDeliveryPrice);
        context.setResult(item);

        return context;
    }
}
