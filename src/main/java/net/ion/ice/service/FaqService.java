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

@Service("faqService")
public class FaqService {

    @Autowired
    private NodeService nodeService;

    public ExecuteContext createAction(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String topStatus = data.get("topStatus") == null ? "" : data.get("topStatus").toString();
        String topNumber = data.get("topNumber") == null ? "" : data.get("topNumber").toString();

        if (StringUtils.equals(topStatus, "active")) {
            List<Node> nodeList = (List<Node>) NodeQuery.build("frequentlyAskedQuestion").matching("topStatus", "active").matching("topNumber", topNumber).getList();
            for (Node node : nodeList) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("frequentlyAskedQuestionId", node.getBindingValue("frequentlyAskedQuestionId"));
                updateData.put("topStatus", "inactive");
                updateData.put("topNumber", null);

                nodeService.executeNode(updateData, "frequentlyAskedQuestion", EventService.UPDATE);
            }
        }

        nodeService.executeNode(data, "frequentlyAskedQuestion", EventService.CREATE);

        return context;
    }

    public ExecuteContext updateAction(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        String topStatus = data.get("topStatus") == null ? "" : data.get("topStatus").toString();
        String topNumber = data.get("topNumber") == null ? "" : data.get("topNumber").toString();

        if (StringUtils.equals(topStatus, "active")) {
            List<Node> nodeList = (List<Node>) NodeQuery.build("frequentlyAskedQuestion").matching("topStatus", "active").matching("topNumber", topNumber).getList();
            for (Node node : nodeList) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("frequentlyAskedQuestionId", node.getBindingValue("frequentlyAskedQuestionId"));
                updateData.put("topStatus", "inactive");
                updateData.put("topNumber", null);

                nodeService.executeNode(updateData, "frequentlyAskedQuestion", EventService.UPDATE);
            }
        }

        nodeService.executeNode(data, "frequentlyAskedQuestion", EventService.UPDATE);

        return context;
    }
}
