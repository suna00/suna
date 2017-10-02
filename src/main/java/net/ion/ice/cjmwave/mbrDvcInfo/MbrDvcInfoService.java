package net.ion.ice.cjmwave.mbrDvcInfo;

import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by leehh on 2017. 10. 1.
 */

@Service("mbrDvcInfoService")
public class MbrDvcInfoService {
    public void mbrDvcInfoSave(ExecuteContext context) {
        try {
            Map<String, Object> data = context.getData();
            String snsKey = data.get("snsKey").toString();
            String snsTypeCd = data.get("snsTypeCd").toString();
            if (StringUtils.isEmpty(snsTypeCd) || StringUtils.isEmpty(snsKey)) {
                //skip
                return;
            }

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
