package net.ion.ice.cjmwave.faq;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.response.JsonResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import net.ion.ice.IceRuntimeException;

/**
 * Created by leehh on 2017. 9. 7.
 */

@Service("faqService")
public class FaqService {

    public void retvNumUpdate(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String faqSeq = data.get("faqSeq").toString();
        if (data == null || StringUtils.isEmpty(faqSeq)) {
            throw new IceRuntimeException("faqSeq Parameter is Null") ;
        }

        Node node = NodeUtils.getNode("faq", faqSeq);
        if (node == null) {
            throw new IceRuntimeException("Node is Null : faqSeq="+faqSeq) ;
        }
        int retvNum = node.getIntValue("retvNum") + 1;
        Map<String, Object> updateData = new LinkedHashMap<>();
        updateData.put("faqSeq", faqSeq);
        updateData.put("retvNum", retvNum);
        Node result = (Node)NodeUtils.getNodeService().executeNode(updateData, "faq", EventService.UPDATE) ;

        context.setResult(result);
    }
}
