package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("termsService")
public class TermsService {

    @Autowired
    private NodeService nodeService;

    public ExecuteContext updateAction(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String id = data.get("id").toString();
        String paramSiteType = data.get("siteType") == null ? "" : data.get("siteType").toString();
        String paramTermsType = data.get("termsType") == null ? "" : data.get("termsType").toString();
        String paramRequired = data.get("required") == null ? "" : data.get("required").toString();
        String paramTitle = data.get("title") == null ? "" : data.get("title").toString();
        String paramContents = data.get("contents") == null ? "" : data.get("contents").toString();

        Node node = nodeService.getNode("terms", id);
        String currentSiteType = node.getBindingValue("siteType") == null ? "" : node.getBindingValue("siteType").toString();
        String currentTermsType = node.getBindingValue("termsType") == null ? "" : node.getBindingValue("termsType").toString();
        String currentRequired = node.getBindingValue("required") == null ? "" : node.getBindingValue("required").toString();
        String currentTitle = node.getBindingValue("title") == null ? "" : node.getBindingValue("title").toString();
        String currentContents = node.getBindingValue("contents") == null ? "" : node.getBindingValue("contents").toString();

        if (!StringUtils.equals(paramSiteType, currentSiteType) ||
                !StringUtils.equals(paramTermsType, currentTermsType) ||
                !StringUtils.equals(paramRequired, currentRequired) ||
                !StringUtils.equals(paramTitle, currentTitle) ||
                !StringUtils.equals(paramContents, currentContents)) {

            Map<String, Object> historyData = new HashMap<>();
            historyData.put("termsId", id);
            historyData.put("siteType", currentSiteType);
            historyData.put("termsType", currentTermsType);
            historyData.put("required", currentRequired);
            historyData.put("title", currentTitle);
            historyData.put("contents", currentContents);

            nodeService.executeNode(historyData, "termsHistory", EventService.UPDATE);
        }

        nodeService.executeNode(data, "terms", EventService.UPDATE);

        return context;
    }
}
