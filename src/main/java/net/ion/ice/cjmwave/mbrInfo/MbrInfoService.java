package net.ion.ice.cjmwave.mbrInfo;

import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by leehh on 2017. 9. 25.
 */

@Service("mbrInfoService")
public class MbrInfoService {

    public void chkMbr(ExecuteContext context){
        Map<String, Object> data = context.getData();
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();
        if(StringUtils.isEmpty(snsTypeCd)){
            throw new ApiException("400","Required Parameter : snsTypeCd");
        }else if(StringUtils.isEmpty(snsKey)){
            throw new ApiException("400","Required Parameter : snsKey");
        }

        Node anode = null;
        try {
            anode = NodeUtils.getNodeService().read("mbrInfo", snsTypeCd + ">" + snsKey);
        }catch (NotFoundNodeException e){

        }

        if(anode == null || anode.isEmpty()) {
            Map<String, Object> resultDate = new LinkedHashMap<>();
            resultDate.put("chkResult",true);
            context.setResult(resultDate);
        }else{
            throw new ApiException("405","Information that meets the conditions already exists.");
        }
    }

}
