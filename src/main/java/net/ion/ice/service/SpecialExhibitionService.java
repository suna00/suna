package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.data.bind.NodeBindingUtils;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service("specialExhibitionService")
public class SpecialExhibitionService {

    @Autowired
    private NodeService nodeService;

    public ExecuteContext saveEvent(ExecuteContext context) {
        Map<String, Object> paramData = context.getData();
        String specialExhibitionAnker = paramData.get("specialExhibitionAnker") == null ? "" : paramData.get("specialExhibitionAnker").toString();

        Node specialExhibitionNode = (Node) nodeService.executeNode(paramData, "specialExhibition", EventService.SAVE);



        context.setResult(specialExhibitionNode);

        return context;
    }
}
