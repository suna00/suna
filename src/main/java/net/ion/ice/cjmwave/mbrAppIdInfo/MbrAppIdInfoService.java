package net.ion.ice.cjmwave.mbrAppIdInfo;

/**
 * Created by leehh on 2017. 10. 11.
 */

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service("mbrAppIdInfoService")
public class MbrAppIdInfoService {

    public static final String MBR_ID = "mbrId";
    public static final String DVC_ID = "dvcId";
    public static final String MBR_APP_ID_INFO = "mbrAppIdInfo";
    public static final String APP_ID = "appId";

    /**
     * 회원 디바이스 정보 저장시 사용
     */
    public void mbrAppIdSave(ExecuteContext context) {
        try {
            Node mbrDvcNode = context.getNode();
            if (StringUtils.isEmpty(mbrDvcNode.getStringValue(MBR_ID)) || StringUtils.isEmpty(mbrDvcNode.getStringValue(DVC_ID))) {
                //skip
                return;
            }

            Map<String, Object> saveData = new LinkedHashMap<>();
            saveData.put(APP_ID, mbrDvcNode.getStringValue(DVC_ID));
            saveData.put(MBR_ID, mbrDvcNode.getStringValue(MBR_ID));
            NodeUtils.getNodeService().executeNode(saveData, MBR_APP_ID_INFO, EventService.SAVE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 비회원 정보 저장시 사용
     */
    public void appIdSave(ExecuteContext context) {
        try {
            Node nonMbrNode = context.getNode();
            if (StringUtils.isEmpty(nonMbrNode.getStringValue(DVC_ID))) {
                //skip
                return;
            }

            Map<String, Object> saveData = new LinkedHashMap<>();
            saveData.put(APP_ID, nonMbrNode.getStringValue(DVC_ID));
            NodeUtils.getNodeService().executeNode(saveData, MBR_APP_ID_INFO, EventService.SAVE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
