package net.ion.ice.cjmwave.vote;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventBroker;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 7. 11..
 */
@Service("voteService")
public class VoteService {

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";

    public static final String EVENT = "event";

    public static final String EVENT_ACTION = "eventAction";

    //public static final String TYPEID = "contsCtgry";


    @Autowired
    private NodeService nodeService ;

    @Autowired
    private InfinispanRepositoryService infinispanService ;

    @Autowired
    private EventBroker eventBroker ;

    public void selCreateEvent(ExecuteContext context) throws IOException {

        Map<String, Object> _data = context.getData();


        Map<String, Object> voteInfo = JsonUtils.parsingJsonToMap(_data.get("voteInfo").toString());
        List<Map<String,Object>> termRstrtnArray = JsonUtils.parsingJsonToList(_data.get("termRstrtnArray").toString());
        List<Map<String,Object>> voteItemArray = JsonUtils.parsingJsonToList(_data.get("voteItemArray").toString());


        //여기다 이제 투표기본정보, 제한, 항목 노드에 각각 저장을 .....

        //1. 투표기본정보 저장- 파일저장있음
        Node voteBasInfo = nodeService.executeNode(voteInfo, "voteBasInfo", "save");

        //2. 저장된 투표기본정보 seq를 가지고 기간제한정보 array 저장
        if(termRstrtnArray.size() > 0){
            for(Map termRstrtnInfo : termRstrtnArray){
                Map newTermRstrtnInfo = new HashMap();
                newTermRstrtnInfo.put("voteSeq",voteBasInfo.getId());
                newTermRstrtnInfo.put("rstrtnStDate",termRstrtnInfo.get("st"));
                newTermRstrtnInfo.put("rstrtnFnsDate",termRstrtnInfo.get("dt"));
                newTermRstrtnInfo.put("rstrtnCnt",termRstrtnInfo.get("cnt"));
                nodeService.executeNode(newTermRstrtnInfo, "votePredRstrtnInfo", "save");
            }
        }

        //3. 저장된 투표기본정보 seq를 가지고 투표항목 array 저장 - 각각 파일저장있음
        if(voteItemArray.size() > 0){
            for(Map voteItemInfo : voteItemArray){
                voteItemInfo.put("voteSeq",voteBasInfo.getId());
                nodeService.executeNode(voteItemInfo, "voteItemInfo", "save");
            }
        }


    }


}


