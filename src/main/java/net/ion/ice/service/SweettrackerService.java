package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("sweettrackerService")
public class SweettrackerService {
    public Logger logger = LoggerFactory.getLogger(SweettrackerService.class);

    public static final String orderDeliveryPrice = "orderDeliveryPrice";
    public static final String orderProduct = "orderProduct";
    public static final String deliveryTrackingInfo = "deliveryTrackingInfo";
    @Autowired
    private NodeBindingService nodeBindingService ;
    @Autowired
    private NodeService nodeService;

    public static final Map<String, String> deliveryStatusMap;
    static
    {
        deliveryStatusMap = new HashMap();
        deliveryStatusMap.put("1", "order005"); //배송 준비
        deliveryStatusMap.put("2", "order005"); //집화 완료
        deliveryStatusMap.put("3", "order005"); //배송 진행중
        deliveryStatusMap.put("4", "order005"); //지점 도착
        deliveryStatusMap.put("5", "order005"); //배송 출발
        deliveryStatusMap.put("6", "order006"); //배송 완료
    }

    // 운송장 추적 요청 API :https://dev-tracking-api.sweettracker.net/add_invoice


    // 운송장 추적정보 수신API(Callback_URL)
    public ExecuteContext getData(ExecuteContext context){
        logger.info("-----START SweettrackerService ");
        Map<String, Object> data = context.getData();
        try{
            String[] params = { "fid", "invoice_no","level" };
            for(String str : params){
                if(data.get(str) == null){
                    data.put("success", false);
                    data.put("message", "fail-required param " + str);
                    context.setResult(data);
                    logger.info("----- SweettrackerService context : " + context);
                    return context;
                }
            }

            List<Map<String, Object>> listById = nodeBindingService.list(orderDeliveryPrice, "orderDeliveryPriceId_equals=" + data.get("fid"));
            if(listById.size() == 0){
                data.put("success", false);
                data.put("message", "fail-invalid fid");
                context.setResult(data);
                logger.info("----- SweettrackerService context : " + context);
                return context;
            }else{
                for(Map<String, Object> map : listById){

                    createDeliveryTrackingInfo(data);

                    if(!JsonUtils.getStringValue(map, "trackingNo").equals(JsonUtils.getStringValue(data, "invoice_no"))){
                        data.put("success", false);
                        data.put("message", "fail-The invoice_no of fid does not match trackingNo.");
                        context.setResult(data);
                        logger.info("----- SweettrackerService context : " + context);
                        return context;
                    }else{
                        String orderStatusByLevel = updateOrderProductStatus(data, map);
                        if(!JsonUtils.getStringValue(map, "deliveryStatus").equals(orderStatusByLevel)){
                            updateDeliveryPriceStatus(map, orderStatusByLevel, orderDeliveryPrice);
                        }
                    }
                }

            }

            data.put("success", true);
            data.put("message", "success");
            context.setResult(data);
            logger.info("----- SweettrackerService context : " + context);
            return context;

        }catch (Exception e){
            e.printStackTrace();

            data.put("success", false);
            data.put("message", "fail-" + e.getMessage());
            context.setResult(data);
            logger.info("----- SweettrackerService context : " + context);
            return context;
        }

    }

    public void createDeliveryTrackingInfo(Map<String, Object> data) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("orderDeliveryPriceId", JsonUtils.getStringValue(data,"fid"));
        info.put("trackingNo",JsonUtils.getStringValue(data,"invoice_no"));
        info.put("level",JsonUtils.getIntValue(data,"level"));
        info.put("timeTrans",JsonUtils.getStringValue(data,"time_trans"));
        info.put("timeSweet",JsonUtils.getStringValue(data,"time_sweet"));
        info.put("location",JsonUtils.getStringValue(data,"where"));
        info.put("telnoOffice",JsonUtils.getStringValue(data,"telno_of f ice"));
        info.put("telnoMan",JsonUtils.getStringValue(data,"telno_man"));
        info.put("details",JsonUtils.getStringValue(data,"details"));
        info.put("recvAddr",JsonUtils.getStringValue(data,"recv_addr"));
        info.put("recvName",JsonUtils.getStringValue(data,"recv_name"));
        info.put("sendName",JsonUtils.getStringValue(data,"send_name"));
        nodeService.executeNode(info, deliveryTrackingInfo, CommonService.CREATE);
    }

    public void updateDeliveryPriceStatus(Map<String, Object> map, String orderStatusByLevel, String orderDeliveryPrice) {
        map.put("deliveryStatus", orderStatusByLevel);
        map.put("changed", new Date());
        nodeService.executeNode(map, orderDeliveryPrice, CommonService.UPDATE);
    }

    public String updateOrderProductStatus(Map<String, Object> data, Map<String, Object> map) {
        List<Map<String, Object>> orderProducts = nodeBindingService.list(orderProduct, "orderProductId_in=" + map.get("orderProductIds"));
        String orderStatusByLevel = deliveryStatusMap.get(data.get("level"));
        for(Map<String, Object> op : orderProducts){
//            if(checkUpdateYn(orderStatusByLevel, op)){
                op.put("orderStatus", orderStatusByLevel);
                op.put("changed", new Date());
                nodeService.executeNode(op, orderProduct, CommonService.UPDATE);
                data.put("orderStatus", orderStatusByLevel);
//            }
        }
        return orderStatusByLevel;
    }

    //# order004,상품준비중
    //# order005,배송중
    //# order014,교환상품 준비중
    //# order015,교환배송중
    private boolean checkUpdateYn(String orderStatusByLevel, Map<String, Object> op) {
        if(orderStatusByLevel.equals(JsonUtils.getStringValue(op, "orderStatus"))) return false;
        String orderStatus = JsonUtils.getStringValue(op, "orderStatus");
        if(!("order004".equals(orderStatus)
                || "order005".equals(orderStatus)
                || "order014".equals(orderStatus)
                || "order015".equals(orderStatus))) return false;
        return true;
    }
}
