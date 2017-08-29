package net.ion.ice.cjmwave.vote;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventBroker;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
        //Map<String, Object> voteItems = JsonUtils.parsingJsonToMap(_data.get("voteItems").toString());


        //여기다 이제 투표기본정보, 제한, 항목 노드에 각각 저장을 .....

        //1. 투표기본정보 저장
        nodeService.executeNode(voteInfo, "voteBasInfo", "save");



    }


}


