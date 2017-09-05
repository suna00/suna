package net.ion.ice.cjmwave.faq;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.response.JsonResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import net.ion.ice.IceRuntimeException;

@Service("faqService")
public class FaqService {
    @Autowired
    private NodeService nodeService;

    public void retvNumUpdate(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        if (data == null || StringUtils.isEmpty(data.get("faqSeq").toString())) {
            throw new IceRuntimeException("faqSeq Parameter is Null") ;
        }
        String faqSeqStr = data.get("faqSeq").toString();
        Node node = nodeService.getNode("faq", faqSeqStr);
        if (node == null) {
            throw new IceRuntimeException("Node is Null : faqSeq="+faqSeqStr) ;
        }
        int retvNum = node.getIntValue("retvNum") + 1;
        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("faqSeq", faqSeqStr);
        updateData.put("retvNum", retvNum);
        ExecuteContext updateContext = ExecuteContext.makeContextFromMap(updateData, "faq", EventService.UPDATE);
        updateContext.execute();

        context.setResult(updateContext.getNode());
    }
}
