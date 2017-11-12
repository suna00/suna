package net.ion.ice.service;

import net.ion.ice.core.api.ApiUtils;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.context.ReadContext;
import net.ion.ice.core.data.DBQuery;
import net.ion.ice.core.data.DBService;
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
import org.springframework.jdbc.core.JdbcTemplate;
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

    public static List<String> orderListStatus = Arrays.asList(new String[]{"order001", "order002", "order003", "order004", "order005", "order006"});

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
        List<String> inSiteTypeList = getInValueList(memberType);

        String membershipLevel = JsonUtils.getStringValue(data, "membershipLevel_matching");
        List<String> inMembershipLevelList = getInValueList(membershipLevel);

        String purchaseDeviceType = JsonUtils.getStringValue(data, "purchaseDeviceType_matching");
        List<String> inpurchaseDeviceType = getInValueList(purchaseDeviceType);

        String usePayMethod = JsonUtils.getStringValue(data, "usePayMethod_matching");
        List<String> inUsePayMethod = getInValueList(usePayMethod);

        String buyCount = JsonUtils.getStringValue(data, "buyCount_matching");

        String searcheFields = JsonUtils.getStringValue(data, "searchFields");
        String searchValue = JsonUtils.getStringValue(data, "searchValue");

        String orderStatus = JsonUtils.getStringValue(data, "orderStatus");

        if(StringUtils.isEmpty(orderStatus)){
            orderStatus = "order001,order002,order003,order004,order005,order006";
        }
        List<String> inOrderStatus = getInValueList(orderStatus);

        List<String> searchListQuery = new ArrayList<>();
        List<Object> searchListValue = new ArrayList<>();

        if(StringUtils.isNotEmpty(createdAbove)){
            searchListQuery.add("b.created >= ?");
            searchListValue.add(createdAbove);
        }

        if(StringUtils.isNotEmpty(createdBelow)){
            searchListQuery.add("b.created <= ?");
            searchListValue.add(createdBelow);
        }

        if(StringUtils.isNotEmpty(memberType)){
            searchListQuery.add("c.siteType in (" + StringUtils.join(inSiteTypeList, ",") + ")");
        }
        if(StringUtils.isNotEmpty(membershipLevel)){
            searchListQuery.add("c.membershipLevel in (" + StringUtils.join(inMembershipLevelList, ",") + ")");
        }

        if(StringUtils.isNotEmpty(purchaseDeviceType)){
            searchListQuery.add("purchaseDeviceType in ("+ StringUtils.join(inpurchaseDeviceType, ",") + ")");
        }

        if(StringUtils.isNotEmpty(usePayMethod)){
            searchListQuery.add("usePayMethod in ("+ StringUtils.join(inUsePayMethod, ",") + ")");
        }

        if(StringUtils.isNotEmpty(buyCount) && !StringUtils.contains(buyCount, ",")){
            if(buyCount.equals("1")){
                searchListQuery.add("b.orderSheetId in (SELECT orderSheetId FROM orderproduct group by orderSheetId having count(*) = 1)");
            }else if(buyCount.equals("2")){
                searchListQuery.add("b.orderSheetId in (SELECT orderSheetId FROM orderproduct group by orderSheetId having count(*) > 1)");
            }
        }

        if(StringUtils.isNotEmpty(searcheFields) && StringUtils.isNotEmpty(searchValue)){
            switch(searcheFields){
                case "memberName":{
                    searchListQuery.add("c.name like concat('%', ?, '%')");
                    searchListValue.add(searchValue);
                    break;
                }
                case "memberId":{
                    searchListQuery.add("c.userId like concat('%', ?, '%')");
                    searchListValue.add(searchValue);
                    break;
                }
                case "productName":{
                    searchListQuery.add("a.name like concat('%', ?, '%')");
                    searchListValue.add(searchValue);
                    break;
                }
                case "orderSheetId":{
                    searchListQuery.add("b.orderSheetId like concat('%', ?, '%')");
                    searchListValue.add(searchValue);
                    break;
                }
                case "recipient":{
                    searchListQuery.add("e.recipient like concat('%', ?, '%')");
                    searchListValue.add(searchValue);
                    break;
                }
                case "vendor":{
                    searchListQuery.add("d.name like concat('%', ?, '%')");
                    searchListValue.add(searchValue);
                    break;
                }
                case "trackingNo":{
                    searchListQuery.add("b.orderSheetId in (SELECT orderSheetId FROM orderdeliveryprice where trackingNo like concat('%', ?, '%')");
                    searchListValue.add(searchValue);
                    break;
                }
            }
        }
        String baseSql = "select %s from orderProduct a, orderSheet b, member c, vendor d, delivery e where a.orderSheetId = b.orderSheetId and b.memberNo = c.memberNo and a.vendorId = d.vendorId and b.orderSheetId = e.orderSheetId %s %s";

        String countSql =  String.format(baseSql, "a.orderStatus, count(*) as cnt ", searchListQuery.size()>0 ?  " and " : "" +  StringUtils.join(searchListQuery.toArray(), " AND "), " group by a.orderStatus");

        NodeType nodeType = NodeUtils.getNodeType(orderSheet_TID);
        JdbcTemplate jdbcTemplate = DBService.getJdbc(nodeType.getDsId());

        List<Map<String, Object>> counts = jdbcTemplate.queryForList(countSql, searchListValue.toArray());

        if(StringUtils.isNotEmpty(orderStatus)) {
            searchListQuery.add("a.orderStatus in (" + StringUtils.join(inOrderStatus, ",") + ")");
        }

        String totalCountSql =  String.format(baseSql, "count(*) as totalCount", " and " + StringUtils.join(searchListQuery.toArray(), " AND "), "");
        Map<String, Object> totalCount = jdbcTemplate.queryForMap(totalCountSql, searchListValue.toArray());


        String listSelect = "b.* ";
        String listSql =  String.format(baseSql, listSelect, " and " + StringUtils.join(searchListQuery.toArray(), " AND "), " group by b.orderSheetId order by b.created desc limit ? offset ?");
        searchListValue.add(pageSize);
        searchListValue.add((currentPage - 1) * pageSize);

        List<Map<String, Object>> orderList = jdbcTemplate.queryForList(listSql, searchListValue.toArray());

        for (Map<String, Object> order : orderList) {
            Timestamp created = order.get("created") == null ? null : (Timestamp) order.get("created");
            if (created != null) order.put("created", FastDateFormat.getInstance("yyyyMMddHHmmss").format(created.getTime()));
            Long memberNoValue = order.get("memberNo") == null ? null : (Long) order.get("memberNo");
            if (memberNoValue == null) {
                order.put("memberItem", new HashMap<>());
            } else {
                Node memberNode = nodeService.getNode("member", memberNoValue.toString());
                if (memberNode == null) {
                    order.put("memberItem", new HashMap<>());
                } else {
                    Map<String, Object> memberItem = new HashMap<>();
                    NodeType memberNodeType = NodeUtils.getNodeType("member");
                    List<PropertyType> memberPropertyList = new ArrayList<>(memberNodeType.getPropertyTypes());
                    for (PropertyType memberProperty : memberPropertyList) {
                        if (!StringUtils.equals(memberProperty.getPid(), "password")) {
                            memberItem.put(memberProperty.getPid(), NodeUtils.getResultValue(context, memberProperty, memberNode));
                        }
                    }
                    order.put("memberItem", memberItem);
                }
            }
            List<Map<String, Object>> deliveryList = nodeBindingService.list(orderDeliveryPrice_TID, "orderSheetId_equals=" + JsonUtils.getStringValue(order, "orderSheetId"));
            for (Map<String, Object> dp : deliveryList) {
                String opIds = (String) dp.get("orderProductIds");
                List<Node> ops = new ArrayList<>();
                for(String opId : StringUtils.split(opIds, ",")){
                    Node op = NodeUtils.getNode("orderProduct", opId);
                    Node product = NodeUtils.getNode("product", op.getStringValue("productId"));
                    op.put("product", product.toDisplay(context));
                    op.put("functionBtn", mypageOrderService.getFunctionBtn(dp, op, product));
                    ops.add(op.toDisplay(context));
//                    List<Map<String, Object>> mainImages = nodeBindingService.list(mypageOrderService.commonResource_TID, "contentsId_matching=" + product.getId() + "&tid_matching=product&name_matching=main");
//                    op.put("referencedMainImage", mainImages);
//                    commonService.putReferenceValue("orderProduct", context, op);
                }
                dp.put("orderProducts", ops);
            }
            order.put("deliveryProducts", deliveryList);
            commonService.putReferenceValue("orderSheet", context, order);
        }
        long totalCnt = (long) totalCount.get("totalCount");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("totalCount", totalCnt);
        item.put("resultCount", orderList.size());
        item.put("pageSize", pageSize);
        item.put("pageCount", totalCnt / pageSize + (totalCnt % pageSize > 0? 1: 0));
        item.put("currentPage", currentPage);

        item.put("items", orderList);

        item.put("counts", counts);
        context.setResult(item);

        return context;
    }

    private List<String> getInValueList(String value) {
        List<String> inValueList = new ArrayList<>();
        for (String val : StringUtils.split(value, ",")) {
            inValueList.add(String.format("'%s'", val));
        }
        return inValueList;
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
/*

//        List<String> inOrderStatusList = getInValueList(orderStatus);

        String existsSiteTypeQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderSheet a, member b where a.memberNo = b.memberNo and b.siteType in (" + StringUtils.join(inSiteTypeList, ",") +") ";

        String existsMembershipLevelQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderSheet a, member b where a.memberNo = b.memberNo and b.membershipLevel in (" + StringUtils.join(inMembershipLevelList, ",") +") ";;

        String existsBuyCountOneQuery = "select group_concat(orderSheetId) as inValue from (SELECT orderSheetId, count(*) buyCount FROM ytn.orderproduct group by orderSheetId having buyCount = 1) a";
        String existsBuyCountManyQuery = "select group_concat(orderSheetId) as inValue from (SELECT orderSheetId, count(*) buyCount FROM ytn.orderproduct group by orderSheetId having buyCount > 1) a";

        String existsOrderStatusQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct where orderStatus like @{orderStatus}";

//        String existsProductIdQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct where productId like concat(@{productId}, '%') ";
        String existsProductNameQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct a, product b where a.productId = b.productId and b.name like concat('%', @{searchValue}, '%') ";
        String existsVendorQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct a, vendor b where a.vendorId = b.vendorId and b.name like concat('%', @{searchValue}, '%') ";
        String existsMemberNameQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderSheet a, member b where a.memberNo = b.memberNo and b.name like concat('%', @{searchValue}, '%') ";
        String existsMemberIdQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderSheet a, member b where a.memberNo = b.memberNo and b.userId like concat('%', @{searchValue}, '%') ";
        String existsOrderSheetIdQuery = "select orderSheetId as inValue from orderSheet where orderSheetId like concat('%', @{searchValue}, '%') ";
        String existsRecipientQuery = "select group_concat(distinct(orderSheetId)) as inValue from delivery where recipient like concat('%', @{searchValue}, '%') ";
        String existsTrackingNoQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderDeliveryPrice where trackingNo like concat('%', @{searchValue}, '%') ";

 */
}
