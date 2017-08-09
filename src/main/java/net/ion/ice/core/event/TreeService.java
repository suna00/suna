package net.ion.ice.core.event;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.stagemonitor.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by jaeho on 2017. 7. 11..
 */
@Service("treeService")
public class TreeService {

   /* public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";*/

    public static final String EVENT = "event";

    public static final String EVENT_ACTION = "eventAction";


    @Autowired
    private NodeService nodeService ;

    @Autowired
    private InfinispanRepositoryService infinispanService ;


    @Autowired
    private EventBroker eventBroker ;

    public void sortEvent(ExecuteContext context){

        Map<String, Object> _data = new HashMap<>();
        _data = context.getData();

        //Node node = context.getNode() ;
        //changeEvent(node, "change");
    }

    private String changeEvent(Node node, String event) {
        Node newNode = node;
        return "SUCCESS";
    }

}
