package net.ion.ice.core.event;

import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.cluster.ClusterService;
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
import java.util.List;
import java.util.Map;

/**
 * Created by jaeho on 2017. 7. 11..
 */
@Service("eventService")
public class EventService {

    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String SAVE = "save";
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";

    public static final String EVENT = "event";

    public static final String EVENT_ACTION = "eventAction";


    @Autowired
    private NodeService nodeService ;

    @Autowired
    private InfinispanRepositoryService infinispanService ;

    @Autowired
    private ClusterService clusterService ;

    @Autowired
    private EventBroker eventBroker ;

    public void createNodeType(ExecuteContext context){
        Node node = context.getNode() ;
        if(NodeType.NODE.equals(node.getStringValue(NodeType.REPOSITORY_TYPE)) || NodeType.DATA.equals(node.getStoreValue(NodeType.REPOSITORY_TYPE))){
            Node createEventNode = createEvent(node, CREATE);
            Node updateEventNode = createEvent(node, UPDATE);
            Node deleteEventNode = createEvent(node, DELETE);
            Node allEventNode = createEvent(node, ALL_EVENT);

            if(StringUtils.isNotEmpty(node.getStringValue("tableName"))){
                createEventAction(createEventNode, CREATE);
                createEventAction(updateEventNode, UPDATE);
                createEventAction(deleteEventNode, DELETE);
            }
        }
    }

    private Node createEvent(Node node, String event) {
        Map<String, Object> eventData = new LinkedHashMap<>() ;
        eventData.put("tid", node.getId()) ;
        eventData.put(EVENT, event) ;
        eventData.put("eventName", event + " " + node.get("typeName")) ;

        return (Node) nodeService.executeNode(eventData, EVENT, SAVE);
    }

    private Node createEventAction(Node eventNode, String event) {
        Map<String, Object> eventActionData = new LinkedHashMap<>() ;
        eventActionData.put(EVENT, eventNode.getId()) ;
        eventActionData.put("action", DELETE.equals(event) ? "deleteDatabase" : "saveDatabase") ;
        eventActionData.put("actionType", "service") ;
        eventActionData.put("actionBody", DELETE.equals(event) ? "nodeBindingService.delete" : "nodeBindingService.execute") ;
        eventActionData.put("order", 1) ;

        return (Node) nodeService.executeNode(eventActionData, EVENT_ACTION, SAVE);
    }

    public void execute(ExecuteContext executeContext) {
        if(!executeContext.isExecute()) return  ;

        NodeType nodeType = executeContext.getNodeType() ;

        Event event = nodeType.getEvent(executeContext.getEvent()) ;

        if((event == null || !event.isNoneExecute()) && nodeType.isNode() && executeContext.getNode() != null) {
            infinispanService.execute(executeContext) ;
            clusterService.cache(executeContext) ;
            if(executeContext.getResult() == null) {
                executeContext.setResult(executeContext.getNode());
            }
        }

        if(event == null) {
            return ;
        }else if(!event.isNoneExecute() && !nodeType.isNode()){
            executeContext.getNode().toStore();
        }

        Event allEvent = nodeType.getEvent(ALL_EVENT) ;

        try {
            executeEventAction(executeContext, event);
            executeEventAction(executeContext, allEvent);
        }catch(IceRuntimeException e){
            infinispanService.execute(executeContext.makeRollbackContext()) ;
            throw e ;
        }

        executeEventListener(executeContext, event);
        executeEventListener(executeContext, allEvent);

    }

    private void executeEventListener(ExecuteContext executeContext, Event event) {
        if(event == null || event.getEventListeners() == null || event.getEventListeners().size() == 0){
            return ;
        }

        for(EventListener listener : event.getEventListeners()){
            eventBroker.putEvent(listener, executeContext);
        }
    }

    private void executeEventAction(ExecuteContext executeContext, Event event) {
        if(event == null || event.getEventActions() == null || event.getEventActions().size() == 0){
            return ;
        }

        for(EventAction eventAction : event.getEventActions()){
            Action action = eventAction.getAction() ;
            action.execute(executeContext) ;
        }
    }
}
