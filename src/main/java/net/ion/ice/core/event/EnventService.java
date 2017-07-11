package net.ion.ice.core.event;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jaeho on 2017. 7. 11..
 */
@Service("eventService")
public class EnventService {

    @Autowired
    private NodeService nodeService ;


    public void createNodeType(ExecuteContext context){
        Node node = context.getNode() ;
        if("node".equals(node.getStringValue(NodeType.REPOSITORY_TYPE)) || "node".equals(node.getStoreValue(NodeType.REPOSITORY_TYPE))){
            Map<String, Object> createEvent = new HashMap<>() ;
            createEvent.put("tid", node.getId()) ;
            createEvent.put("event", "create") ;
            createEvent.put("eventName", "Create " + node.get("typeName")) ;
//            createEvent.put()
        }

    }
}
