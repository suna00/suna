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
        Map<String, Object> siteData = new HashMap<>();
        userData.put("userGroupId", "affiliate");

        for (String key : data.keySet()) {
            if (StringUtils.contains(key, "user>")) {
                String[] splitKey = StringUtils.split(key, ">");
                userData.put(splitKey[1], data.get(key));
            } else if (StringUtils.contains(key, "site>")) {
                String[] splitKey = StringUtils.split(key, ">");
                siteData.put(splitKey[1], data.get(key));
            } else {
                affiliateData.put(key, data.get(key));
            }
        }

        Node affiliateNode = (Node) nodeService.executeNode(affiliateData, "affiliate", EventService.SAVE);
        String affiliateId = affiliateNode.getBindingValue("affiliateId").toString();
        Node userNode = (Node) nodeService.executeNode(userData, "user", EventService.SAVE);
        String userId = userNode.getBindingValue("userId").toString();
        Node siteNode = (Node) nodeService.executeNode(siteData, "site", EventService.SAVE);
        String siteId = siteNode.getBindingValue("siteId").toString();

        Map<String, Object> affiliateMappingData = new HashMap<>();
        affiliateMappingData.put("affiliateId", affiliateId);
        affiliateMappingData.put("userId", userId);
        affiliateMappingData.put("siteId", siteId);
        nodeService.executeNode(affiliateMappingData, "affiliate", EventService.SAVE);

        Map<String, Object> userMappingData = new HashMap<>();
        userMappingData.put("userId", userId);
        userMappingData.put("affiliateId", affiliateId);
        nodeService.executeNode(userMappingData, "user", EventService.SAVE);

        Map<String, Object> siteMappingData = new HashMap<>();
        siteMappingData.put("siteId", siteId);
        siteMappingData.put("name", affiliateNode.getBindingValue("name").toString());
        siteMappingData.put("siteType", affiliateNode.getBindingValue("siteType").toString());
        siteMappingData.put("siteStatus", affiliateNode.getBindingValue("affiliateStatus").toString());
        nodeService.executeNode(siteMappingData, "site", EventService.SAVE);

        context.setResult(affiliateNode);

        return context;
    }
}
