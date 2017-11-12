package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

@Service("couponMembershipService")
public class CouponMembershipService {

    @Autowired
    private NodeService nodeService;

    public void saveEvent(ExecuteContext context) {
        Map<String, Object> paramData = new LinkedHashMap<>(context.getData());
        String couponNumber = paramData.get("couponNumber") == null ? "" : paramData.get("couponNumber").toString();
        String affiliateMallId = paramData.get("affiliateMallId") == null ? "" : paramData.get("affiliateMallId").toString();

        if (StringUtils.isEmpty(couponNumber)) {
            Node affiliateMallNode = nodeService.read("affiliateMall", affiliateMallId);
            if (affiliateMallNode != null) {
                String businessCode = affiliateMallNode.getBindingValue("businessCode").toString();
                couponNumber = businessCode+" "+getRandomCode()+" "+getRandomCode()+" "+getRandomCode();
                paramData.put("couponNumber", couponNumber);
            }
        }

        nodeService.executeNode(paramData, "couponMembership", EventService.SAVE);
    }

    private int getRandomCode() {
        Random random = new Random();
        int result = random.nextInt(10000)+1000;
        if (result>10000) result = result - 1000;
        return result;
    }
}
