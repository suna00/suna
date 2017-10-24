package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeQuery;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("vendorService")
public class VendorService {

    @Autowired
    private NodeService nodeService;


    public ExecuteContext saveEvent(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        Map<String, Object> vendorData = new HashMap<>();
        Map<String, Object> userData = new HashMap<>();
        userData.put("userGroupId", "vendor");

        for (String key : data.keySet()) {
            if (StringUtils.equals(key, "user")) {
                List<Map<String, Object>> userDataList = (List<Map<String, Object>>) data.get("user");
                for (Map<String, Object> userDataMap : userDataList) {
                    userData.putAll(userDataMap);
                }
            } else {
                vendorData.put(key, data.get(key));
            }
        }

        Node vendorNode = (Node) nodeService.executeNode(vendorData, "vendor", EventService.SAVE);
        String vendorId = vendorNode.getBindingValue("vendorId").toString();
        Node userNode = (Node) nodeService.executeNode(userData, "user", EventService.SAVE);
        String userId = userNode.getBindingValue("userId").toString();

        Map<String, Object> vendorMappingData = new HashMap<>();
        vendorMappingData.put("vendorId", vendorId);
        vendorMappingData.put("userId", userId);
        nodeService.executeNode(vendorMappingData, "vendor", EventService.SAVE);

        Map<String, Object> userMappingData = new HashMap<>();
        userMappingData.put("userId", userId);
        userMappingData.put("vendorId", vendorId);
        nodeService.executeNode(userMappingData, "user", EventService.SAVE);

        context.setResult(vendorNode);

        return context;
    }
}
