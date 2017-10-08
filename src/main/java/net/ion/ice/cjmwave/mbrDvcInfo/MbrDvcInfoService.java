package net.ion.ice.cjmwave.mbrDvcInfo;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by leehh on 2017. 10. 1.
 */

@Service("mbrDvcInfoService")
public class MbrDvcInfoService {
    public void mbrDvcInfoSave(ExecuteContext context) {
        try {
            Map<String, Object> data = context.getData();
            if (data.get("snsTypeCd") == null || StringUtils.isEmpty(data.get("snsTypeCd").toString())) {
                //skip
                return;
            } else if (data.get("snsKey") == null || StringUtils.isEmpty(data.get("snsKey").toString())) {
                //skip
                return;
            }
            String snsKey = data.get("snsKey").toString();
            String snsTypeCd = data.get("snsTypeCd").toString();

            if(StringUtils.contains(snsTypeCd,"snsTypeCd>")){
                snsTypeCd = StringUtils.replace(snsTypeCd,"snsTypeCd>","");
            }

            if (data.get("dvcId") != null) {
                //creates
                data.put("mbrId", snsTypeCd + ">" + snsKey);
                NodeUtils.getNodeService().executeNode(data, "mbrDvcInfo", EventService.SAVE);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
