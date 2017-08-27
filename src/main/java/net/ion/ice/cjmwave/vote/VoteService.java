package net.ion.ice.cjmwave.vote;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventBroker;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

        //여기다 이제 투표기본정보, 제한, 항목 노드에 각각 저장을 .....
        //nodeService.saveNode(_data);

        //Map<String, Object> voteInfo = JsonUtils.parsingJsonToMap(_data.get("voteInfo").toString());

        /*Map<String, Object> voteInfo = JsonUtils.parsingJsonToMap(_data.get("voteInfo").toString());

        //MultiValueMap<String, MultipartFile> multiFileMap =

        Map<String, Object> termRstrtnInfo = JsonUtils.parsingJsonToMap(_data.get("termRstrtnInfo").toString());
        Map<String, Object> voteItems = JsonUtils.parsingJsonToMap(_data.get("voteItems").toString());*/

    }


}


