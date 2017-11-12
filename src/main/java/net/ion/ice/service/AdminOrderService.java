package net.ion.ice.service;

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

        String createdAbove = JsonUtils.getStringValue(data, "created_above");
        String createdBelow = JsonUtils.getStringValue(data, "created_below");

        String vendorType = JsonUtils.getStringValue(data, "vendorType_matching");
        String memberType = JsonUtils.getStringValue(data, "memberType_matching");

        String membershipLevel = JsonUtils.getStringValue(data, "membershipLevel_matching");
        String purchaseDeviceType = JsonUtils.getStringValue(data, "purchaseDeviceType_matching");
        String usePayMethod = JsonUtils.getStringValue(data, "usePayMethod_matching");
        String buyCount = JsonUtils.getStringValue(data, "buyCount_matching");

        String searcheFields = JsonUtils.getStringValue(data, "searchFields");
        String searchValue = JsonUtils.getStringValue(data, "searchValue");

        String orderStatus = JsonUtils.getStringValue(data, "orderStatus");


        List<String> splitOrderStatusList = Arrays.asList(StringUtils.split(orderStatus, ","));
        List<String> inOrderStatusList = new ArrayList<>();
        for (String splitOrderStatus : splitOrderStatusList) {
            inOrderStatusList.add(String.format("'%s'", splitOrderStatus));
        }

        String existsSiteTypeQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderSheet a, member b where a.memberNo = b.memberNo and b.siteType in (" + memberType +") ";

        String existsMembershipLevelQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderSheet a, member b where a.memberNo = b.memberNo and b.membershipLevel in (" + membershipLevel +") ";;

        String existsBuyCountOneQuery = "SELECT orderSheetId as inValue, count(*) buyCount FROM ytn.orderproduct group by orderSheetId having buyCount = 1";
        String existsBuyCountManyQuery = "SELECT orderSheetId as inValue, count(*) buyCount FROM ytn.orderproduct group by orderSheetId having buyCount > 1";

        String existsOrderStatusQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct where orderStatus in ("+StringUtils.join(inOrderStatusList, ",")+")";

//        String existsProductIdQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct where productId like concat(@{productId}, '%') ";
        String existsProductNameQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct a, product b where a.productId = b.productId and b.name like concat('%', @{searchValue}, '%') ";
        String existsVendorQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct a, vendor b where a.vendorId = b.vendorId and b.name like concat('%', @{searchValue}, '%') ";
        String existsMemberNameQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderSheet a, member b where a.memberNo = b.memberNo and b.name like concat('%', @{searchValue}, '%') ";
        String existsMemberIdQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderSheet a, member b where a.memberNo = b.memberNo and b.userId like concat('%', @{searchValue}, '%') ";
        String existsOrderSheetIdQuery = "select orderSheetId as inValue from orderSheet where orderSheetId like concat('%', @{searchValue}, '%') ";
        String existsRecipientQuery = "select group_concat(distinct(orderSheetId)) as inValue from delivery where recipient like concat('%', @{searchValue}, '%') ";
        String existsTrackingNoQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderDeliveryPrice where trackingNo like concat('%', @{searchValue}, '%') ";

        List<String> search = new ArrayList<>();
        search.add("pageSize="+pageSize);
        search.add("page="+currentPage);
        search.add("sorting=created desc");
        search.add("referenceView=memberNo");

        if(StringUtils.isNotEmpty(createdAbove)){
            search.add("created_above="+createdAbove);
        }

        if(StringUtils.isNotEmpty(createdBelow)){
            search.add("created_below="+createdBelow);
        }

        if(StringUtils.isNotEmpty(memberType)){
            search.add("orderSheetId_exists=" + existsSiteTypeQuery);
        }

        if(StringUtils.isNotEmpty(purchaseDeviceType)){
            search.add("purchaseDeviceType_in="+purchaseDeviceType);
        }
        if(StringUtils.isNotEmpty(usePayMethod)){
            search.add("usePayMethod_in="+usePayMethod);
        }
        if(StringUtils.isNotEmpty(membershipLevel)){
            search.add("orderSheetId_exists="+existsMembershipLevelQuery);
        }

        if(StringUtils.isNotEmpty(orderStatus)) {
            search.add("orderSheetId_exists=" + existsOrderStatusQuery);
        }

        if(StringUtils.isNotEmpty(buyCount) && !StringUtils.contains(buyCount, ",")){
            if(buyCount.equals("1")){
                search.add("orderSheetId_exists=" + existsBuyCountOneQuery);
            }else if(buyCount.equals("2")){
                search.add("orderSheetId_exists=" + existsBuyCountManyQuery);
            }
        }

        if(StringUtils.isNotEmpty(searcheFields) && StringUtils.isNotEmpty(searchValue)){
            String searchQuery = null;
            switch(searcheFields){
                case "memberName":{
                    searchQuery = existsMemberNameQuery;
                    break;
                }
                case "memberId":{
                    searchQuery = existsMemberIdQuery;
                    break;
                }
                case "productName":{
                    searchQuery = existsProductNameQuery;
                    break;
                }
                case "orderSheetId":{
                    searchQuery = existsOrderSheetIdQuery;
                    break;
                }
                case "recipient":{
                    searchQuery = existsRecipientQuery;
                    break;
                }
                case "vendor":{
                    searchQuery = existsVendorQuery;
                    break;
                }
                case "trackingNo":{
                    searchQuery = existsTrackingNoQuery;
                    break;
                }
            }
            if(searchQuery != null) {
                search.add("orderSheetId_exists=" + searchQuery);
            }
        }

        String searchText = StringUtils.join(search, "&");

//        String searchText = "pageSize=" + pageSize +
//                "&page=" + currentPage +
//                "&sorting=created desc" +
//                (orderSheetId != "" ? "&orderSheetId_equals=" + orderSheetId : "") +
//                (productId != "" || orderStatus != "" ? "&orderSheetId_exists=" + existsQuery : "") +
//                (createdFromto != "" ? "&created_fromto=" + createdFromto : "") +
//                (memberNo != "" ? "&memberNo_equals=" + memberNo : "");


        NodeType nodeType = NodeUtils.getNodeType(orderSheet_TID);
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType, null);
        queryContext.setData(context.getData());

        NodeBindingInfo nodeBindingInfo = nodeBindingService.getNodeBindingInfo(orderSheet_TID);
        List<Map<String, Object>> sheetList = nodeBindingInfo.list(queryContext);

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
                            memberItem.put(memberProperty.getPid(), NodeUtils.getResultValue(queryContext, memberProperty, memberNode));
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

//        int pageCount = (int) Math.ceil((double) sheetTotalList.size() / (double) pageSize);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("totalCount", queryContext.getResultSize());
        item.put("resultCount", sheetList.size());
        item.put("pageSize", queryContext.getPageSize());
        item.put("pageCount", queryContext.getResultSize() / queryContext.getPageSize() + (queryContext.getResultSize() % queryContext.getPageSize() > 0? 1: 0));
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
        String productName = JsonUtils.getStringValue(data, "productName");
        String vendor = JsonUtils.getStringValue(data, "vendor");
        String changeType = JsonUtils.getStringValue(data, "changeType");

        String existsProductIdQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderChangeProduct where productId like concat(@{productId}, '%') ";
        String existsProductNameQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderChangeProduct a, product b where a.productId = b.productId and b.name like concat('%', @{productName}, '%') ";
        String existsVendorQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderChangeProduct a, vendor b where a.vendorId = b.vendorId and b.name like concat('%', @{vendor}, '%') ";

        String searchText = "pageSize=" + pageSize +
                "&page=" + currentPage +
                "&sorting=created desc" +
                (StringUtils.isNotEmpty(orderSheetId) ? "&orderSheetId_equals=" + orderSheetId : "") +
                (StringUtils.isNotEmpty(changeType) ? "&changeType_equals=" + changeType : "") +
                (StringUtils.isNotEmpty(productId) ? "&orderSheetId_exists=" + existsProductIdQuery : "") +
                (StringUtils.isNotEmpty(productName) ? "&orderSheetId_exists=" + existsProductNameQuery : "") +
                (StringUtils.isNotEmpty(vendor) ? "&orderSheetId_exists=" + existsVendorQuery : "") +
                (StringUtils.isNotEmpty(createdFromto) ? "&created_fromto=" + createdFromto : "");

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

    //주문변경신청 상태 변경(취소교환반품)
    public ExecuteContext updateOrderChangeStatus(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String[] params = {"orderSheetId", "orderChangeId", "orderChangeStatus"};
        if (CommonService.requiredParams(context, data, params)) return context;

        String orderChangeStatus = JsonUtils.getStringValue(data, "orderChangeStatus");

        List<Map<String, Object>> orderChangeProductList = nodeBindingService.list(orderChangeProduct_TID, "orderChangeId_equals=" + JsonUtils.getStringValue(data, "orderChangeId"));
        for(Map<String, Object> ocp : orderChangeProductList){
            ocp.put("orderChangeStatus", orderChangeStatus);
            nodeService.executeNode(ocp, orderChangeProduct_TID, CommonService.UPDATE);

            // order009,취소완료/ order016,교환완료/ order021,반품완료
            if("order009".equals(orderChangeStatus) || "order016".equals(orderChangeStatus) || "order021".equals(orderChangeStatus)){
                // 취소교환반품 완료시 주문상품에 반영
                Map<String, Object> op = NodeUtils.getNode(orderProduct_TID, JsonUtils.getStringValue(ocp, "orderProductId"));
                int opQuantity = JsonUtils.getIntValue(op, "quantity");
                int ocpQuantity = JsonUtils.getIntValue(ocp, "quantity");
                double opOrderPrice = JsonUtils.getDoubleValue(op, "orderPrice");
                double ocpOrderPrice = JsonUtils.getDoubleValue(ocp, "orderPrice");
                double opPaymentPrice = JsonUtils.getDoubleValue(op, "paymentPrice");
                double ocpPaymentPrice = JsonUtils.getDoubleValue(ocp, "paymentPrice");

                if(opQuantity == ocpQuantity){
                    op.put("orderStatus", orderChangeStatus);
                }
                //주문변경분 주문상품에 반영
                op.put("quantity", opQuantity - ocpQuantity);
                op.put("orderPrice", opOrderPrice - ocpOrderPrice);
                op.put("paymentPrice", opPaymentPrice - ocpPaymentPrice);
                nodeService.executeNode(op, orderProduct_TID, CommonService.UPDATE);
            }
        }

        if("order009".equals(orderChangeStatus) || "order016".equals(orderChangeStatus) || "order021".equals(orderChangeStatus)){
            Map<String, Object> orderChange = NodeUtils.getNode(orderChange_TID, JsonUtils.getStringValue(data, "orderChangeId"));
            Map<String, Object> orderSheet = NodeUtils.getNode(orderSheet_TID, JsonUtils.getStringValue(data, "orderSheetId"));

            double totalProductPrice = JsonUtils.getDoubleValue(orderSheet, "totalProductPrice");
            double totalDeliveryPrice = JsonUtils.getDoubleValue(orderSheet, "totalDeliveryPrice");
            double totalDiscountPrice = JsonUtils.getDoubleValue(orderSheet, "totalDiscountPrice");
            double totalOrderPrice = JsonUtils.getDoubleValue(orderSheet, "totalOrderPrice");
            double totalPaymentPrice = JsonUtils.getDoubleValue(orderSheet, "totalPaymentPrice");
            double couponDiscountPrice = JsonUtils.getDoubleValue(orderSheet, "couponDiscountPrice");
            double totalWelfarePoint = JsonUtils.getDoubleValue(orderSheet, "totalWelfarePoint");
            double totalYPoint = JsonUtils.getDoubleValue(orderSheet, "totalYPoint");

            double cancelOrderPrice = JsonUtils.getDoubleValue(orderChange, "cancelOrderPrice");
            double cancelProductPrice = JsonUtils.getDoubleValue(orderChange, "cancelProductPrice");
            double cancelDeliveryPrice = JsonUtils.getDoubleValue(orderChange, "cancelDeliveryPrice");
            double deductPrice = JsonUtils.getDoubleValue(orderChange, "deductPrice");
            double addDeliveryPrice = JsonUtils.getDoubleValue(orderChange, "addDeliveryPrice");
            double refundPrice = JsonUtils.getDoubleValue(orderChange, "refundPrice");
            double refundWelfarePoint = JsonUtils.getDoubleValue(orderChange, "refundWelfarePoint");
            double refundYPoint = JsonUtils.getDoubleValue(orderChange, "refundYPoint");
            double refundPaymentPrice = JsonUtils.getDoubleValue(orderChange, "refundPaymentPrice");

            totalProductPrice = totalProductPrice - cancelProductPrice ;
            totalDeliveryPrice = totalDeliveryPrice - cancelDeliveryPrice + addDeliveryPrice ;
            totalOrderPrice = totalOrderPrice - cancelOrderPrice + addDeliveryPrice ;
            totalPaymentPrice = totalOrderPrice - couponDiscountPrice - refundWelfarePoint - refundYPoint ;
            totalWelfarePoint = totalWelfarePoint - refundWelfarePoint ;
            totalYPoint = totalYPoint - refundYPoint ;

            orderSheet.put("totalProductPrice", totalProductPrice);
            orderSheet.put("totalDeliveryPrice", totalDeliveryPrice);
            orderSheet.put("totalOrderPrice", totalOrderPrice);
            orderSheet.put("totalPaymentPrice", totalPaymentPrice);
            orderSheet.put("totalWelfarePoint", totalWelfarePoint);
            orderSheet.put("totalYPoint", totalYPoint);

            nodeService.executeNode(orderSheet, orderSheet_TID, CommonService.UPDATE);

        }
        return context;
    }

}
