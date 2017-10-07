package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("sweettrackerService")
public class SweettrackerService {
    public Logger logger = LoggerFactory.getLogger(SweettrackerService.class);

    public static final String orderDeliveryPrice = "orderDeliveryPrice";
    public static final String orderProduct = "orderProduct";
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


    // 스윗트래커 callback_url
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
                    if(!JsonUtils.getStringValue(map, "trackingNo").equals(JsonUtils.getStringValue(data, "invoice_no"))){
                        data.put("success", false);
                        data.put("message", "fail-The invoice_no of fid does not match trackingNo.");
                        context.setResult(data);
                        logger.info("----- SweettrackerService context : " + context);
                        return context;
                    }else{
                        List<Map<String, Object>> orderProducts = nodeBindingService.list(orderProduct, "orderProductId_in=" + map.get("orderProductIds"));
                        String orderStatusByLevel = deliveryStatusMap.get(data.get("level"));
                        for(Map<String, Object> op : orderProducts){
                            if(checkUpdateYn(orderStatusByLevel, op)){
                                op.put("orderStatus", orderStatusByLevel);
                                op.put("changed", new Date());
                                nodeService.executeNode(op, orderProduct, CommonService.UPDATE);
                                data.put("orderStatus", orderStatusByLevel);
                            }
                        }

                        if(!JsonUtils.getStringValue(map, "deliveryStatus").equals(orderStatusByLevel)){
                            map.put("deliveryStatus", orderStatusByLevel);
                            map.put("changed", new Date());
                            nodeService.executeNode(map, orderDeliveryPrice, CommonService.UPDATE);
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
