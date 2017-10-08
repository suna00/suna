package net.ion.ice.cjmwave.errMsgInfo;

/**
 * Created by leehh on 2017. 10. 7.
 */

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service("errMsgInfoService")
public class ErrMsgInfoService {
    public String getErrMsg(String errCd){
        String returnErrMsg = null;
        //default langCd=eng
        List<Node> errMsgList = NodeUtils.getNodeService().getNodeList("errMsgInfo", "langCd=eng&errCd="+errCd);

        if(errMsgList != null && !errMsgList.isEmpty()){
            Node infoNode = errMsgList.get(0);
            Object errMsg = infoNode.get("errMsg");
            if(errMsg instanceof Map){
                Map<String, Object> msgMap = (Map<String, Object>)errMsg;
                returnErrMsg = msgMap.get("eng").toString();
            }else{
                returnErrMsg = errMsg.toString();
            }
        }

        return returnErrMsg;
    }

    public String getErrMsg(String errCd, String langCd){
        String returnErrMsg = null;

        //param langCd
        List<Node> errMsgList = NodeUtils.getNodeService().getNodeList("errMsgInfo", "langCd="+langCd+"&errCd="+errCd);
        if(errMsgList != null && !errMsgList.isEmpty()){
            Node infoNode = errMsgList.get(0);
            Object errMsg = infoNode.get("errMsg");
            if(errMsg instanceof Map){
                Map<String, Object> msgMap = (Map<String, Object>)errMsg;
                returnErrMsg = msgMap.get(langCd).toString();
            }else{
                returnErrMsg = errMsg.toString();
            }
        }

        return returnErrMsg;
    }
}
