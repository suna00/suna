package net.ion.ice.cjmwave.mbrInfo;

import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by leehh on 2017. 9. 25.
 */

@Service("mbrInfoService")
public class MbrInfoService {

    public void chkMbr(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();
        if (StringUtils.isEmpty(snsTypeCd)) {
            throw new ApiException("400", "Required Parameter : snsTypeCd");
        } else if (StringUtils.isEmpty(snsKey)) {
            throw new ApiException("400", "Required Parameter : snsKey");
        }

        Node anode = null;
        try {
            anode = NodeUtils.getNodeService().read("mbrInfo", snsTypeCd + ">" + snsKey);
        } catch (NotFoundNodeException e) {

        }

        if (anode == null || anode.isEmpty()) {
            Map<String, Object> resultDate = new LinkedHashMap<>();
            resultDate.put("chkResult", true);
            context.setResult(resultDate);
        } else {
            throw new ApiException("405", "Information that meets the conditions already exists.");
        }
    }

    public void semiMbrJoin(ExecuteContext context){
        Map<String, Object> data = context.getData();
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();
        if (StringUtils.isEmpty(snsTypeCd)) {
            throw new ApiException("400", "Required Parameter : snsTypeCd");
        } else if (StringUtils.isEmpty(snsKey)) {
            throw new ApiException("400", "Required Parameter : snsKey");
        }

        //create
        data.put("snsTypeCd", snsTypeCd);
        data.put("snsKey", snsKey);
        data.put("mbrDivCd", "1");
        data.put("mbrSttusCd", "1");
        data.put("sttusChgSbst", "준회원 가입");
        data.put("sbscDt", new Date());

        Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.CREATE);
        context.setResult(result);
    }

    public void rglrMbrJoin(ExecuteContext context){
        Map<String, Object> data = context.getData();
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();
        if (StringUtils.isEmpty(snsTypeCd)) {
            throw new ApiException("400", "Required Parameter : snsTypeCd");
        } else if (StringUtils.isEmpty(snsKey)) {
            throw new ApiException("400", "Required Parameter : snsKey");
        }

        Node anode = null;
        try {
            anode = NodeUtils.getNodeService().read("mbrInfo", snsTypeCd + ">" + snsKey);
            String mbrDivCd = anode.getStringValue("mbrDivCd");
            if("mbrDivCd>2".equals(mbrDivCd)){
                throw new ApiException("411", "You are already an active member.");
            }
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }

        //update
        data.put("snsTypeCd", snsTypeCd);
        data.put("snsKey", snsKey);
        data.put("mbrDivCd", "2");
        data.put("mbrSttusCd", "1");
        data.put("sttusChgSbst", "정회원 전환");
        Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
        context.setResult(result);
    }

    public void updLoginInfo(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();
        if (StringUtils.isEmpty(snsTypeCd)) {
            throw new ApiException("400", "Required Parameter : snsTypeCd");
        } else if (StringUtils.isEmpty(snsKey)) {
            throw new ApiException("400", "Required Parameter : snsKey");
        }

        Node anode = null;
        try {
            if(StringUtils.contains(snsTypeCd,"snsTypeCd>")){
                snsTypeCd = StringUtils.replace(snsTypeCd,"snsTypeCd>","");
            }
            anode = NodeUtils.getNodeService().read("mbrInfo", snsTypeCd + ">" + snsKey);
            if (anode != null && !anode.isEmpty()) {
                //node에 최종수정일 디바이스 정보 등 수정해야됨
                Map<String, Object> updateData = new LinkedHashMap<>();
                updateData.put("id", snsTypeCd + ">" + snsKey);
                //String nowDate = DateFormatUtils.format(new Date(), "yyyyMMddHHmmss");//data.get("now").toString()
                updateData.put("lastLoginDt", new Date());
                Node result = (Node) NodeUtils.getNodeService().executeNode(updateData, "mbrInfo", EventService.UPDATE);
                context.setResult(result);
            }
        } catch (NotFoundNodeException e) {
            e.printStackTrace();
        }
    }

}
