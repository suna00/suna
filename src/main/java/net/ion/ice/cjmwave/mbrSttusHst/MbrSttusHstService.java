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

    public static final String MBR_STTUS_CD = "mbrSttusCd";
    public static final String STTUS_CHG_SBST = "sttusChgSbst";
    public static final String MBR_ID = "mbrId";
    public static final String MBR_STTUS_HST = "mbrSttusHst";

    public void sttusHstCreate(ExecuteContext context) {
        try {
            Node mbrNode = context.getNode();
            Node mbrExistNode = context.getExistNode();
            List<String> changeProperties = context.getChangedProperties();
            Map<String, Object> data = context.getData();

            Map<String, Object> createData = new LinkedHashMap<>();

            if (StringUtils.isEmpty(mbrNode.getId()) || StringUtils.isEmpty(mbrNode.getStringValue(MBR_STTUS_CD))) {
                //skip
                return;
            }
            String mbrSttusCd = mbrNode.getStringValue(MBR_STTUS_CD);
            if (StringUtils.contains(mbrSttusCd, "mbrSttusCd>")) {
                mbrSttusCd = StringUtils.replace(mbrSttusCd, "mbrSttusCd>", "");
            }
            String sttusChgSbst = "";
            if (data.get(STTUS_CHG_SBST) != null && !StringUtils.isEmpty(data.get(STTUS_CHG_SBST).toString())) {
                sttusChgSbst = data.get(STTUS_CHG_SBST).toString();
            }

            if (mbrExistNode != null) {//mbrUpdate
                //context 에 있는 existNode와 상태코드를 비교해서 변경된 경우에만 이력을 쌓아야함
                if (changeProperties == null || changeProperties.isEmpty() || changeProperties.size() == 0) {
                    //skip
                    return;
                }

                Boolean sttusChgYn = false;
                for (String pid : changeProperties) {
                    if (MBR_STTUS_CD.equals(pid)) {
                        sttusChgYn = true;
                    }
                }

                //create
                String existSttusCd = mbrExistNode.getStringValue(MBR_STTUS_CD);
                if (!sttusChgYn || mbrSttusCd.equals(existSttusCd)) {
                    //skip
                    return;
                }

            }
            createData.put(MBR_ID, mbrNode.getId());
            createData.put(MBR_STTUS_CD, mbrSttusCd);
            createData.put(STTUS_CHG_SBST, sttusChgSbst);

            NodeUtils.getNodeService().executeNode(createData, MBR_STTUS_HST, EventService.CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
