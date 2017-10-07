package net.ion.ice.cjmwave.mbrSttusHst;

/**
 * Created by leehh on 2017. 10. 1.
 */

import java.util.List;
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
            Node mbrExistNode = context.getExistNode();
            List<String> changeProperties = context.getChangedProperties();
            Map<String, Object> data = context.getData();
            //context 에 있는 existNode와 상태코드를 비교해서 변경된 경우에만 이력을 쌓아야함
            Map<String, Object> createData = new LinkedHashMap<>();

            if (StringUtils.isEmpty(mbrNode.getId()) || StringUtils.isEmpty(mbrNode.getStringValue("mbrSttusCd"))) {
                //skip
                return;
            }

            if(changeProperties.isEmpty() || changeProperties.size() == 0){
                //skip
                return;
            }

            Boolean sttusChgYn = false;
            for(String pid :changeProperties){
                if("mbrSttusCd".equals(pid)){
                    sttusChgYn = true;
                }
            }

            //create
            String mbrSttusCd = mbrNode.getStringValue("mbrSttusCd");
            String existSttusCd = mbrExistNode.getStringValue("mbrSttusCd");
            if(!sttusChgYn || mbrSttusCd.equals(existSttusCd)){
                //skip
                return;
            }
            if (StringUtils.contains(mbrSttusCd, "mbrSttusCd>")) {
                mbrSttusCd = StringUtils.replace(mbrSttusCd, "mbrSttusCd>", "");
            }
            String sttusChgSbst = "";
            if (data.get("sttusChgSbst") != null && !StringUtils.isEmpty(data.get("sttusChgSbst").toString())) {
                sttusChgSbst = data.get("sttusChgSbst").toString();
            }
            createData.put("mbrId", mbrNode.getId());
            createData.put("mbrSttusCd", mbrSttusCd);
            createData.put("sttusChgSbst", sttusChgSbst);

            NodeUtils.getNodeService().executeNode(createData, "mbrSttusHst", EventService.CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
