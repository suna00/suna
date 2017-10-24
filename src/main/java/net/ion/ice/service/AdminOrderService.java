package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.session.SessionService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("adminOrderService")
public class AdminOrderService {

    private static Logger logger = LoggerFactory.getLogger(MypageOrderService.class);

    public static final String orderSheet_TID = "orderSheet";
    public static final String orderProduct_TID = "orderProduct";
    public static final String orderDeliveryPrice_TID = "orderDeliveryPrice";
    public static final String commonResource_TID = "commonResource";

    @Autowired
    private SessionService sessionService;
    @Autowired
    private NodeBindingService nodeBindingService;
    @Autowired
    private NodeService nodeService;
    CommonService commonService;

    // N개 주문서ID check 상태변경 버튼 클릭
    public ExecuteContext multiDeliveryStatusChangeTarget(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String[] params = { "orderSheetIds" };
        if (CommonService.requiredParams(context, data, params)) return context;

        String orderSheetIds = JsonUtils.getStringValue(data, "orderSheetIds");

        List<Map<String, Object>> orderDeliveryPriceList = nodeBindingService.list(orderDeliveryPrice_TID, "orderSheetId_in="+orderSheetIds);
        List<Map<String, Object>> orderProductList = nodeBindingService.list(orderProduct_TID, "orderSheetId_in="+orderSheetIds);

        List<Map<String, Object>> list = deliveryStatusChangeTarget(context, orderSheetIds, orderDeliveryPriceList, orderProductList);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("items", list);
        context.setResult(item);

        return context;
    }

    // 특정 배송정보만 상태변경 버튼 클릭
    public ExecuteContext singleDeliveryStatusChangeTarget(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String[] params = { "orderSheetId", "orderDeliveryPriceId" };
        if (CommonService.requiredParams(context, data, params)) return context;

        String orderSheetId = JsonUtils.getStringValue(data, "orderSheetId");
        String orderDeliveryPriceId = JsonUtils.getStringValue(data, "orderDeliveryPriceId");

        List<Map<String, Object>> orderDeliveryPriceList = nodeBindingService.list(orderDeliveryPrice_TID, "orderDeliveryPriceId_equals="+orderDeliveryPriceId);
        List<Map<String, Object>> orderProductList = nodeBindingService.list(orderProduct_TID, "orderSheetId_equals="+orderSheetId);

        List<Map<String, Object>> list = deliveryStatusChangeTarget(context, orderSheetId, orderDeliveryPriceList, orderProductList);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("items", list);
        context.setResult(item);

        return context;
    }

    // 배송 상태변경 대상 리스트
    public List<Map<String, Object>> deliveryStatusChangeTarget(ExecuteContext context, String orderSheetIds, List<Map<String, Object>> orderDeliveryPriceList, List<Map<String, Object>> orderProductList) {

        List<Map<String, Object>> list = new ArrayList<>();
        for(String orderSheetId : StringUtils.split(orderSheetIds, ",")){
            Map<String, Object> map = new LinkedHashMap<>();
            Node orderSheet = NodeUtils.getNode(orderSheet_TID, orderSheetId);
            commonService.putReferenceValue(orderSheet_TID, context, orderSheet);

            List<Map<String, Object>> dList = new ArrayList<>();
            for(Map<String, Object> orderDeliveryPrice : orderDeliveryPriceList){
                Map<String, Object> dMap = new LinkedHashMap<>();
                dMap.putAll(orderDeliveryPrice);

                List<Map<String, Object>> pList = new ArrayList<>();
                for(Map<String, Object> orderProduct : orderProductList){
                    Map<String, Object> pMap = new LinkedHashMap<>();
                    pMap.putAll(orderProduct);

                    String orderProductIds = ",".concat(JsonUtils.getStringValue(dMap, "orderProductIds")).concat(",");
                    if(orderProductIds.contains(JsonUtils.getStringValue(pMap, "orderProductId"))){
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

    // 배송 상태변경 처리
    /*
            order004,상품준비중
            order005,배송중
            order006,배송완료

           {
              "changeDeliveryStatusList": [
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
    public ExecuteContext changeDeliveryStatus(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String[] params = { "changeDeliveryStatusList" };
        if (CommonService.requiredParams(context, data, params)) return context;

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item = new LinkedHashMap<>();
        try {
            list = JsonUtils.parsingJsonToList(JsonUtils.getStringValue(data, "changeDeliveryStatusList"));
            if(list.size() > 0){
                for(Map<String, Object> map : list){
                    Node node = NodeUtils.getNode(orderDeliveryPrice_TID, JsonUtils.getStringValue(map, "orderDeliveryPriceId"));
                    node.putAll(map);
                    nodeService.executeNode(node, orderDeliveryPrice_TID, CommonService.UPDATE);
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




}
