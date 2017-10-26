package net.ion.ice.cjmwave.mbrDvcInfo;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
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

    public static final String SNS_TYPE_CD = "snsTypeCd";
    public static final String SNS_KEY = "snsKey";
    public static final String MBR_DVC_INFO = "mbrDvcInfo";
    public static final String DVC_ID = "dvcId";
    public static final String MBR_ID = "mbrId";

    public void mbrDvcInfoSave(ExecuteContext context) {
        try {
            Map<String, Object> data = context.getData();
            Map<String, Object> createData = new LinkedHashMap<>();
            if (data.get(SNS_TYPE_CD) == null || StringUtils.isEmpty(data.get(SNS_TYPE_CD).toString())) {
                //skip
                return;
            } else if (data.get(SNS_KEY) == null || StringUtils.isEmpty(data.get(SNS_KEY).toString())) {
                //skip
                return;
            }
            String snsKey = data.get(SNS_KEY).toString();
            String snsTypeCd = data.get(SNS_TYPE_CD).toString();

            if(StringUtils.contains(snsTypeCd,"snsTypeCd>")){
                snsTypeCd = StringUtils.replace(snsTypeCd,"snsTypeCd>","");
            }

            if (data.get(DVC_ID) != null) {
                //creates
                createData.put(MBR_ID, snsTypeCd + ">" + snsKey);
                createData.put(DVC_ID, data.get(DVC_ID));
                createData.put("dvcNm", data.get("dvcNm"));
                createData.put("osCd", data.get("osCd"));
                createData.put("osVer", data.get("osVer"));
                createData.put("adId", data.get("adId"));
                NodeUtils.getNodeService().executeNode(createData, MBR_DVC_INFO, EventService.SAVE);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
