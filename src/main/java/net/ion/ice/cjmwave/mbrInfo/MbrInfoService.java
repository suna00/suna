package net.ion.ice.cjmwave.mbrInfo;

import java.text.ParseException;
import java.util.List;

import net.ion.ice.cjmwave.errMsgInfo.ErrMsgUtil;
import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by leehh on 2017. 9. 25.
 */

@Service("mbrInfoService")
public class MbrInfoService {
    private ErrMsgUtil errMsgUtil = new ErrMsgUtil();
    public void chkMbr(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();


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
            System.out.println("######chkMbr :"+anode);
            throw new ApiException("405", "Information that meets the conditions already exists.");
        }
    }

    public void semiMbrJoin(ExecuteContext context){
        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

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
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        Node anode = null;
        try {
            anode = NodeUtils.getNodeService().read("mbrInfo", snsTypeCd + ">" + snsKey);

            String mbrSttusCd = anode.getStringValue("mbrSttusCd");
            if("mbrSttusCd>2".equals(mbrSttusCd) || "mbrSttusCd>3".equals(mbrSttusCd)){
                throw new ApiException("412", errMsgUtil.getErrMsg(context,"412"));
            }
            String mbrDivCd = anode.getStringValue("mbrDivCd");
            if("mbrDivCd>2".equals(mbrDivCd)){
                throw new ApiException("411", errMsgUtil.getErrMsg(context,"411"));
            }
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }

        //update
        data.put("snsTypeCd", snsTypeCd);
        data.put("snsKey", snsKey);
        data.put("mbrDivCd", "2");
        data.put("sttusChgSbst", "정회원 전환");
        Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
        context.setResult(result);
    }

    public void updLoginInfo(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        Node anode = null;
        try {
            if(StringUtils.contains(snsTypeCd,"snsTypeCd>")){
                snsTypeCd = StringUtils.replace(snsTypeCd,"snsTypeCd>","");
            }
            anode = NodeUtils.getNodeService().read("mbrInfo", snsTypeCd + ">" + snsKey);
            if (anode != null && !anode.isEmpty()) {
                //node에 최종수정일 디바이스 정보 등 수정
                Map<String, Object> updateData = new LinkedHashMap<>();
                updateData.put("id", snsTypeCd + ">" + snsKey);
                updateData.put("lastLoginDt", new Date());
                Node result = (Node) NodeUtils.getNodeService().executeNode(updateData, "mbrInfo", EventService.UPDATE);
                Object imgUrlObj = result.get("imgUrl");
                String imgName = "";
                if(imgUrlObj instanceof FileValue) {
                    FileValue fileValue = (FileValue) imgUrlObj;
                    if (fileValue != null) {
                        imgName = fileValue.getFileName();
                    }
                }
                result.put("imgFileName", imgName);
                context.setNodeData(result);
            }
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }
    }

    public void rejoin(ExecuteContext context){
        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        if (data.get("infoOttpAgreeYn") == null || StringUtils.isEmpty(data.get("infoOttpAgreeYn").toString())) {
            throw new ApiException("400", "Required Parameter : infoOttpAgreeYn");
        }
        String infoOttpAgreeYn = data.get("infoOttpAgreeYn").toString();
        if(!"true".equals(infoOttpAgreeYn)){
            throw new ApiException("413", errMsgUtil.getErrMsg(context,"413"));
        }

        Node anode = null;
        try {
            anode = NodeUtils.getNodeService().read("mbrInfo", snsTypeCd + ">" + snsKey);

            String mbrSttusCd = anode.getStringValue("mbrSttusCd");
            Date rtrmmbDate = null;
            if(anode.get("rtrmmbDate") != null){
                try {
                    rtrmmbDate = DateUtils.parseDate(anode.getStringValue("rtrmmbDate"), "yyyyMMddHHmmss","yyyy/MM/dd HH:mm:ss");
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            if(!"mbrSttusCd>3".equals(mbrSttusCd)){
                throw new ApiException("414", errMsgUtil.getErrMsg(context,"414"));
            }
            if(rtrmmbDate == null){
                throw new ApiException("415", errMsgUtil.getErrMsg(context,"415"));
            }

            List<Node> dclaNodeList = null;
            try{
                dclaNodeList = NodeUtils.getNodeService().getNodeList("dclaSetupMng","setupTypeCd_matching=4&sorting=dclaSetupSeq desc&limit=1");
            }catch (Exception e){
                e.printStackTrace();
            }

            if(dclaNodeList == null || dclaNodeList.isEmpty()){
                throw new ApiException("416", errMsgUtil.getErrMsg(context,"416"));
            }
            Node dclaNode = dclaNodeList.get(0);
            int setupBaseDayNum = dclaNode.getIntValue("setupBaseDayNum");

            Date rejoinAbleDate = DateUtils.addDays(rtrmmbDate,setupBaseDayNum);
            System.out.println("rejoinAbleDate:"+rejoinAbleDate);
            Date current = new Date();
            System.out.println("current:"+current);
            if(current.getTime() < rejoinAbleDate.getTime()){
                throw new ApiException("417", String.format(errMsgUtil.getErrMsg(context,"417"),setupBaseDayNum));
            }

            //update
            data.put("snsTypeCd", snsTypeCd);
            data.put("snsKey", snsKey);
            data.put("mbrSttusCd", "1");
            data.put("rtrmmbDate", "");
            data.put("sttusChgSbst", "재가입");
            Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
            context.setResult(result);
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }

    }

    public void secession(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();


        Node anode = null;
        try {
            anode = NodeUtils.getNodeService().read("mbrInfo", snsTypeCd + ">" + snsKey);

            String mbrSttusCd = anode.getStringValue("mbrSttusCd");
            if("mbrSttusCd>3".equals(mbrSttusCd)){
                throw new ApiException("418", errMsgUtil.getErrMsg(context,"418"));
            }

            //update
            data.put("snsTypeCd", snsTypeCd);
            data.put("snsKey", snsKey);
            data.put("mbrSttusCd", "3");
            data.put("rtrmmbDate", new Date());
            data.put("sttusChgSbst", "회원 탈퇴");
            Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
            context.setResult(result);
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        }
    }

    public void dormReles(ExecuteContext context) {
        Map<String, Object> data = context.getData();
        chkSnsParams(data);
        String snsKey = data.get("snsKey").toString();
        String snsTypeCd = data.get("snsTypeCd").toString();

        if (data.get("infoOttpAgreeYn") == null || StringUtils.isEmpty(data.get("infoOttpAgreeYn").toString())) {
            throw new ApiException("400", "Required Parameter : infoOttpAgreeYn");
        }
        String infoOttpAgreeYn = data.get("infoOttpAgreeYn").toString();
        /*if(!"true".equals(infoOttpAgreeYn)){
            throw new ApiException("413", errMsgUtil.getErrMsg(context,"413"));
        }*/

        Node anode = null;
        try {
            anode = NodeUtils.getNodeService().read("mbrInfo", snsTypeCd + ">" + snsKey);

            String mbrSttusCd = anode.getStringValue("mbrSttusCd");
            if("mbrSttusCd>1".equals(mbrSttusCd)){
                throw new ApiException("411", errMsgUtil.getErrMsg(context,"411"));
            }else if("mbrSttusCd>3".equals(mbrSttusCd)){
                throw new ApiException("418", errMsgUtil.getErrMsg(context,"418"));
            }

            //update
            data.put("snsTypeCd", snsTypeCd);
            data.put("snsKey", snsKey);
            data.put("mbrSttusCd", "1");
            data.put("dormTrtDate", "");
            data.put("sttusChgSbst", "휴면 해제");
            Node result = (Node) NodeUtils.getNodeService().executeNode(data, "mbrInfo", EventService.UPDATE);
            context.setResult(result);
        } catch (NotFoundNodeException e) {
            throw new ApiException("404", "Not Found Member");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void chkSnsParams(Map<String, Object> data) {

        if (data.get("snsTypeCd") == null || StringUtils.isEmpty(data.get("snsTypeCd").toString())) {
            throw new ApiException("400", "Required Parameter : snsTypeCd");
        } else if (data.get("snsKey") == null || StringUtils.isEmpty(data.get("snsKey").toString())) {
            throw new ApiException("400", "Required Parameter :snsKey");
        }
    }

}
