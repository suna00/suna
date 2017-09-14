package net.ion.ice.cjmwave.contsRetvHst;

import java.util.LinkedHashMap;
import java.util.List;

import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("contsRetvService")
public class ContsRetvService {
    public void hitNumUpdate(ExecuteContext context) {
        Node targetNode = context.getNode();
        if (targetNode == null) {
            throw new IceRuntimeException("Node is Null") ;
        }
        int hitNum = targetNode.getIntValue("hitNum") + 1;
        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("id", targetNode.getId());
        updateData.put("hitNum", hitNum);
        Node result = (Node)NodeUtils.getNodeService().executeNode(updateData, context.getNodeType().getTypeId(), EventService.UPDATE) ;

        context.setResult(result);
    }

    public void insert(ExecuteContext context) {
        NodeType nodeType = context.getNodeType();
        if (nodeType == null) throw new IceRuntimeException("nodeType is Null");

        List<String> idablePids = nodeType.getIdablePIds();
        if (idablePids.isEmpty()) throw new IceRuntimeException("idablePids is Null");
        //System.out.println("ContsRetvService NodeType:" + nodeType.getTypeId() + ", idablePids size:" + idablePids.size());

        String makeIdValue = makeIdValue(idablePids, context.getNode());
        if (StringUtils.isEmpty(makeIdValue)) throw new IceRuntimeException("idablePids is Null");
        //System.out.println("ContsRetvService makeIdValue:" + makeIdValue);
        //data insert
        Map<String, Object> insertData = new HashMap<>();
        insertData.put("tid", nodeType.getTypeId());
        insertData.put("contsId", makeIdValue);
        Node result = (Node)NodeUtils.getNodeService().executeNode(insertData, "contsRetvHst", EventService.CREATE) ;
    }

    private String makeIdValue(List<String> idablePids, Node node) {
        String returnVal = "";

        for (int i = 0; i < idablePids.size(); i++) {
            String paramStr = idablePids.get(i);

            if (node != null && StringUtils.isNotEmpty(node.getStringValue(paramStr))) {
                if (i == 0) {
                    returnVal = node.getStringValue(paramStr);
                } else {
                    returnVal += ">" + node.getStringValue(paramStr);
                }
            } else {
                returnVal = "";
                break;
            }
        }
        return returnVal;
    }
}
