package net.ion.ice.cjmwave.mbrSttusHst;

/**
 * Created by leehh on 2017. 10. 1.
 */

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service("mbrSttusHstService")
public class MbrSttusHstService {
    public void sttusHstCreate(ExecuteContext context) {
        try {
            Node mbrNode = context.getNode();
            //context 에 있는 existNode와 상태코드를 비교해서 변경된 경우에만 이력을 쌓아야함
            Map<String, Object> data = context.getData();

            if (StringUtils.isEmpty(mbrNode.getId()) || StringUtils.isEmpty(mbrNode.getStringValue("mbrSttusCd"))) {
                //skip
                return;
            }
            //create
            String mbrSttusCd = mbrNode.getStringValue("mbrSttusCd");
            if (StringUtils.contains(mbrSttusCd, "mbrSttusCd>")) {
                mbrSttusCd = StringUtils.replace(mbrSttusCd, "mbrSttusCd>", "");
            }
            data.put("mbrId", mbrNode.getId());
            data.put("mbrSttusCd", mbrSttusCd);

            NodeUtils.getNodeService().executeNode(data, "mbrSttusHst", EventService.CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
