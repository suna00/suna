package net.ion.ice.cjmwave.qna;

import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by leehh on 2017. 9. 29.
 */
@Service("qnaService")
public class QnaService {
    public void fileNmUpdEvent(ExecuteContext context) {
        Map<String, Object> data = context.getData();

        if (data == null || data.get("qstnAtcFilePath") == null) {
            //System.out.println("qstnAtcFilePath is null");
            return;
        }
        Object fileObj = data.get("qstnAtcFilePath");
        if (fileObj instanceof FileValue) {
            FileValue qstnAtcFilePath = (FileValue) data.get("qstnAtcFilePath");
            String fileNm = qstnAtcFilePath.getFileName();

            Node targetNode = context.getNode();
            if (targetNode == null) {
                //System.out.println("targetNode is null");
                return;
            }

            Map<String, Object> updateData = new LinkedHashMap<>();
            updateData.put("qnaSeq", targetNode.getId());
            updateData.put("qstnAtcFileNm", fileNm);
            Node result = (Node) NodeUtils.getNodeService().executeNode(updateData, context.getNodeType().getTypeId(), EventService.UPDATE) ;
            context.setResult(result);
        } else {
            //System.out.println("qstnAtcFilePath is not file format");
            return;
        }
    }
}
