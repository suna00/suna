package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("couponService")
public class CouponService {
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String SAVE = "save";
    public static final String PATTERN = "yyyyMMddHHmmss";
    public static final String unlimitedDate = "99991231235959";

    @Autowired
    private NodeService nodeService ;
    protected CommonService common;


    public ExecuteContext download(ExecuteContext context) {
        Map<String, Object> data = new LinkedHashMap<>(context.getData());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN);
        LocalDateTime now = LocalDateTime.now();

        String[] params = { "memberNo","couponTypeId" };
        if (common.requiredParams(context, data, params)) return context;

        Node couponType = nodeService.getNode("couponType", data.get("couponTypeId").toString());
        if(couponType == null){
            common.setErrorMessage(context, "V0001");
            return context;
        }

        if("createPeriodType>limit".equals(couponType.getValue("createPeriodType"))){
            LocalDateTime start = LocalDateTime.parse(couponType.getValue("createStartDate").toString(), formatter);
            LocalDateTime end = LocalDateTime.parse(couponType.getValue("createEndDate").toString(), formatter);

            if(!(start.isBefore(now) && end.isAfter(now))){
                common.setErrorMessage(context, "V0004");
                return context;
            }
        }

        if("limitYn>limit".equals(couponType.getValue("limitedQuantityType")) && "0".equals(couponType.getValue("remainingQuantity"))){
            common.setErrorMessage(context, "V0002");
            return context;
        }

        if("limitYn>limit".equals(couponType.getValue("samePersonQuantityType"))){
            List<Node> list = nodeService.getNodeList("coupon", "memberNo_matching="+data.get("memberNo")+"&couponTypeId_matching="+data.get("couponTypeId"));
            if(list.size() > 0){
                int count = (int) couponType.getValue("samePersonQuantity");
                if(list.size() >= count){
                    common.setErrorMessage(context, "V0003");
                    return context;
                }
            }
        }

        data.putAll(couponType);

        String endDate = unlimitedDate;
        if("limitYn>limit".equals(couponType.getValue("validePeriodType"))){
            LocalDateTime after = now.plusDays(Integer.parseInt(couponType.getValue("validePeriod").toString()));
            endDate = after.format(formatter);
        }

        data.put("publishedDate", now.format(formatter));
        data.put("startDate", now.format(formatter));
        data.put("endDate", endDate);
        data.put("couponStatus", "n");

        Object result = nodeService.executeNode(data, "coupon", SAVE);
        context.setResult(result);

        if("limitYn>limit".equals(couponType.getValue("limitedQuantityType"))){
            couponType.put("remainingQuantity", Integer.parseInt(couponType.getValue("remainingQuantity").toString()) - 1);
            nodeService.executeNode(couponType, "couponType", UPDATE);
        }

        return context;
    }

}
