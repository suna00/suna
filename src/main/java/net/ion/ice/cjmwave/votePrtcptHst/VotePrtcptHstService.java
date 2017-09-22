package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leehh on 2017. 9. 20.
 */
@Service("votePrtcptHstService")
public class VotePrtcptHstService {
    private static Logger logger = LoggerFactory.getLogger(VotePrtcptHstService.class);
    private static String[] pidArray = {"sersVoteSeq", "voteSeq", "voteItemSeq", "prtcpMbrId", "connIpAdr"};

    @Autowired
    private NodeService nodeService;


    /**
     * 2017.09.20 leehh
     * 시리즈(MAMA) 투표하기 api에서 사용함
     */
    public void seriesVoting(ExecuteContext context) throws IOException {
        Map<String, String> returnResult = new HashMap<>();//response
        Map<String, Object> data = context.getData();
        if (data.isEmpty()) {
            returnResult.put("validationMessage", "request data is Null");
            context.setResult(returnResult);
            //throw new IceRuntimeException("request data is Null");
            logger.info("request data is Null");
            return;
        }

        if (data.get("voteResult") == null) {
            returnResult.put("validationMessage", "voteResult is Null");
            context.setResult(returnResult);
            logger.info("voteResult is Null");
            //throw new IceRuntimeException("voteResult is Null");
            return;
        }
        String voteResult = data.get("voteResult").toString();
        //logger.info("###request JsonString : " + voteResult);

        //Json parsing
        List<Map<String, Object>> reqJson = JsonUtils.parsingJsonToList(voteResult);
        if (reqJson.isEmpty()) throw new IceRuntimeException("voteResult data is Null");
        //logger.info("###json to map size:" + reqJson.size());

        for (Map<String, Object> voteData : reqJson) {
            //node create
            Node result = (Node) nodeService.executeNode(voteData, "sersVotePrtcptHst", EventService.CREATE);
            context.setResult(result);
        }
        //returnResult.put("insertCnt", reqJson.size()+"");
        //context.setResult(returnResult);
    }
}
