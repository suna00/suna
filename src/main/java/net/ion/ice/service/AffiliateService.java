package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("affiliateService")
public class AffiliateService {

    @Autowired
    private NodeService nodeService;


    public ExecuteContext saveEvent(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        Map<String, Object> affiliateData = new HashMap<>();
        Map<String, Object> userData = new HashMap<>();
        userData.put("userGroupId", "affiliate");

        for (String key : data.keySet()) {
            if (StringUtils.equals(key, "user")) {
                List<Map<String, Object>> userDataList = (List<Map<String, Object>>) data.get("user");
                for (Map<String, Object> userDataMap : userDataList) {
                    userData.putAll(userDataMap);
                }
            } else {
                affiliateData.put(key, data.get(key));
            }
        }

        Node affiliateNode = (Node) nodeService.executeNode(affiliateData, "affiliate", EventService.SAVE);
        String affiliateId = affiliateNode.getBindingValue("affiliateId").toString();
        Node userNode = (Node) nodeService.executeNode(userData, "user", EventService.SAVE);
        String userId = userNode.getBindingValue("userId").toString();

        Map<String, Object> affiliateMappingData = new HashMap<>();
        affiliateMappingData.put("affiliateId", affiliateId);
        affiliateMappingData.put("userId", userId);
        nodeService.executeNode(affiliateMappingData, "affiliate", EventService.SAVE);

        Map<String, Object> userMappingData = new HashMap<>();
        userMappingData.put("userId", userId);
        userMappingData.put("affiliateId", affiliateId);
        nodeService.executeNode(userMappingData, "user", EventService.SAVE);

        context.setResult(affiliateNode);

        return context;
    }
}
