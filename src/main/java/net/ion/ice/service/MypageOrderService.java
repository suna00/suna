package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.*;
import net.ion.ice.core.session.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("mypageOrderService")
public class MypageOrderService {

    public static final String orderSheet_TID = "orderSheet";
    public static final String orderProduct_TID = "orderProduct";
    public static final String commonResource_TID = "commonResource";

    @Autowired
    private SessionService sessionService;
    @Autowired
    private NodeBindingService nodeBindingService;


    public ExecuteContext getMypageOrderList(ExecuteContext context) {
        Map<String, Object> session = null;
        try {
            session = sessionService.getSession(context.getHttpRequest());
            String memberNo = JsonUtils.getStringValue(session, "member.memberNo");
            getOrderSheetList(context, memberNo);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return context;
    }

    public ExecuteContext getAdminOrderList(ExecuteContext context) {
        getOrderSheetList(context, "");

        return context;
    }

    public void getOrderSheetList(ExecuteContext context, String memberNo) {
        Map<String, Object> data = context.getData();
        int pageSize = (JsonUtils.getIntValue(data, "pageSize") == 0 ? 10 : JsonUtils.getIntValue(data, "pageSize"));
        int page = JsonUtils.getIntValue(data, "page");
        int currentPage = (page == 0 ? 1 : page);
        String createdFromto = JsonUtils.getStringValue(data, "createdFromto");
        String orderSheetId = JsonUtils.getStringValue(data, "orderSheetId");
        String orderProductId = JsonUtils.getStringValue(data, "orderProductId");
        String orderStatus = JsonUtils.getStringValue(data, "orderStatus");

        String existsQuery = "select group_concat(distinct(orderSheetId)) as inValue from orderProduct where IF(@{productId} = '' ,'1',productId) = IF(@{productId} = '' ,'1',@{productId}) and IF(@{orderStatus} = '' ,'1',orderStatus) = IF(@{orderStatus} = '' ,'1',@{orderStatus})";

        String searchText = "pageSize=" + pageSize +
                "&page=" + page +
                "&sorting=created desc" +
                (orderSheetId != "" ? "&orderSheetId_equals="+orderSheetId : "") +
                (orderProductId != "" || orderStatus != "" ? "&orderSheetId_exists="+existsQuery : "") +
                (createdFromto != "" ? "&created_fromto="+createdFromto : "") +
                (memberNo != "" ? "&memberNo_equals="+memberNo : "");

        List<Map<String, Object>> sheetTotalList = nodeBindingService.list(orderSheet_TID, "");


        NodeType nodeType = NodeUtils.getNodeType(orderSheet_TID);
        QueryContext queryContext = QueryContext.createQueryContextFromText(searchText, nodeType, null);
        queryContext.setData(context.getData());
        NodeBindingInfo nodeBindingInfo = nodeBindingService.getNodeBindingInfo(orderSheet_TID);
        List<Map<String, Object>> sheetList = nodeBindingInfo.list(queryContext);

        for(Map<String, Object> sheet : sheetList){
            List<Map<String, Object>> opList = nodeBindingService.list(orderProduct_TID, "orderSheetId_equals="+ JsonUtils.getStringValue(sheet, "orderSheetId"));
            for(Map<String, Object> op : opList){
                Node product = NodeUtils.getNode("product", JsonUtils.getStringValue(op,"productId"));
                List<Map<String, Object>> mainImages = nodeBindingService.list(commonResource_TID, "contentsId_matching="+ product.getId() + "&tid_matching=product&name_matching=main");
                product.put("referencedMainImage", mainImages);

                Map<String, Object> orderDeliveryPrice = getOrderDeliveryPrice(JsonUtils.getStringValue(op, "orderSheetId"), JsonUtils.getStringValue(op, "orderProductId"));
                op.put("referencedOrderDeliveryPrice", orderDeliveryPrice);
                op.put("functionBtn", getFunctionBtn(orderDeliveryPrice, op, product));
                putReferenceValue("orderProduct", context, op);
            }
            sheet.put("referencedOrderProduct", opList);
            putReferenceValue("orderSheet", context, sheet);
        }

        int pageCount = (int) Math.ceil((double) sheetList.size() / (double) pageSize);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("totalCount", sheetTotalList.size());
        item.put("resultCount", sheetList.size());
        item.put("pageSize", pageSize);
        item.put("pageCount", pageCount);
        item.put("currentPage", currentPage);

        item.put("items", sheetList);
        context.setResult(item);
    }

    public void putReferenceValue(String nodeTypeId, ExecuteContext context, Map<String, Object> op) {
        NodeType nodeType = NodeUtils.getNodeType(nodeTypeId) ;
        for(PropertyType pt : nodeType.getPropertyTypes()){
            if("REFERENCE".equals(pt.getValueType().toString()) && op.get(pt.getPid()) != null){
                op.put(pt.getPid(), NodeUtils.getReferenceValue(context, op.get(pt.getPid()), pt));
            }
        }
    }

    public Map<String, Object> getFunctionBtn(Map<String, Object> orderDeliveryPrice, Map<String, Object> orderProduct, Map<String, Object> product){
        Map<String, Object> map = new LinkedHashMap<>();
        String orderStatus = JsonUtils.getStringValue(orderProduct, "orderStatus");
        String contentsType = JsonUtils.getStringValue(product, "contentsType");
        String deliveryMethod = JsonUtils.getStringValue(product, "deliveryMethod");
        boolean writableReviewYn = (JsonUtils.getIntValue(orderDeliveryPrice, "writableReviewYn") == 0 ? false : true);

        boolean cancelBtn = false;
        boolean exchangeBtn = false;
        boolean returnBtn = false;
        boolean reviewBtn = false;
        boolean trackingViewBtn = false;
        boolean addressChangeBtn = false;

        if("order002".equals(orderStatus) || "order003".equals(orderStatus) || "order004".equals(orderStatus)){
            cancelBtn = true;
        }
        if("goods".equals(contentsType) && ("order005".equals(orderStatus) || "order006".equals(orderStatus))){
            exchangeBtn = true;
        }
        if("goods".equals(contentsType) && ("order005".equals(orderStatus) || "order006".equals(orderStatus))){
            returnBtn = true;
        }
        if("goods".equals(contentsType) && writableReviewYn && ("order005".equals(orderStatus) || "order006".equals(orderStatus) || "order007".equals(orderStatus))){
            reviewBtn = true;
        }
        if("goods".equals(contentsType) && "delivery".equals (deliveryMethod) && ("order005".equals(orderStatus) || "order006".equals(orderStatus) || "order015".equals(orderStatus) || "order016".equals(orderStatus))){
            trackingViewBtn = true;
        }
        if("goods".equals(contentsType) && ("order002".equals(orderStatus) || "order003".equals(orderStatus))){
            addressChangeBtn = true;
        }

        map.put("cancelBtn", cancelBtn);
        map.put("exchangeBtn", exchangeBtn);
        map.put("returnBtn", returnBtn);
        map.put("reviewBtn", reviewBtn);
        map.put("trackingViewBtn", trackingViewBtn);
        map.put("addressChangeBtn", addressChangeBtn);

        return map;
    }

    public Map<String, Object> getOrderDeliveryPrice(String orderSheetId, String orderProductId) {
        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("review").getJdbcTemplate();
        String query = "select a.writableReviewYn, b.*\n" +
                        "from (\n" +
                        "  select\n" +
                        "    IFNULL(MAX(orderDeliveryPriceId), 0) as orderDeliveryPriceId, IFNULL(MAX(if((sendingDate + INTERVAL 30 DAY) >= now(), TRUE, FALSE)), FALSE) as writableReviewYn\n" +
                        "  from orderdeliveryprice\n" +
                        "  WHERE orderSheetId = ? and find_in_set(?, orderProductIds) > 0\n" +
                        ") a LEFT JOIN orderdeliveryprice b\n" +
                        "ON a.orderDeliveryPriceId = b.orderDeliveryPriceId ";
        return jdbcTemplate.queryForMap(query, orderSheetId, orderProductId);
    }

    public ExecuteContext getOrderChangeList(ExecuteContext context) {

        return context;
    }

}
