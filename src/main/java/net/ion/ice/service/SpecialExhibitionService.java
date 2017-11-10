package net.ion.ice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.data.bind.NodeBindingUtils;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeQuery;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service("specialExhibitionService")
public class SpecialExhibitionService {
    private Logger logger = LoggerFactory.getLogger(SpecialExhibitionService.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private ProductService productService;

    private ObjectMapper objectMapper = new ObjectMapper();

    public ExecuteContext saveEvent(ExecuteContext context) {
        Map<String, Object> paramData = context.getData();
        String specialExhibitionAnker = paramData.get("specialExhibitionAnker") == null ? "" : paramData.get("specialExhibitionAnker").toString();

        Node specialExhibitionNode = (Node) nodeService.executeNode(paramData, "specialExhibition", EventService.SAVE);
        String specialExhibitionId = specialExhibitionNode.getBindingValue("specialExhibitionId").toString();

        if (!StringUtils.isEmpty(specialExhibitionAnker)) {
            try {
                JsonNode jsonNode = objectMapper.readValue(specialExhibitionAnker, JsonNode.class);

                for (JsonNode specialExhibitionAnkerJsonNode : jsonNode) {
                    String specialExhibitionAnkerId = specialExhibitionAnkerJsonNode.get("specialExhibitionAnkerId").asText();
                    String name = specialExhibitionAnkerJsonNode.get("name").asText();

                    Map<String, Object> specialExhibitionAnkerData = new HashMap<>();
                    specialExhibitionAnkerData.put("specialExhibitionAnkerId", specialExhibitionAnkerId);
                    specialExhibitionAnkerData.put("specialExhibitionId", specialExhibitionId);
                    specialExhibitionAnkerData.put("name", name);
                    specialExhibitionAnkerData.put("sortOrder", "0");
                    specialExhibitionAnkerData.put("ankerStatus", "y");

                    Node specialExhibitionAnkerNode = (Node) nodeService.executeNode(specialExhibitionAnkerData, "specialExhibitionAnker", EventService.SAVE);
                    specialExhibitionAnkerId = specialExhibitionAnkerNode.getBindingValue("specialExhibitionAnkerId").toString();

                    JsonNode productSaveValueJsonNode = specialExhibitionAnkerJsonNode.get("productSaveValue");

                    for (JsonNode productJsonNode : productSaveValueJsonNode) {
                        if (productJsonNode.get("upperId").isNull()) {
                            String productId = productJsonNode.get("productId").toString();
                            productService.copyProduct(productId, true, specialExhibitionId, specialExhibitionAnkerId);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        context.setResult(specialExhibitionNode);

        return context;
    }
}