package net.ion.ice.core.event;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public static final String DELETE = "delete";
    public static final String ALL_EVENT = "allEvent";

    public static final String EVENT = "event";

    public static final String EVENT_ACTION = "eventAction";


    @Autowired
    private NodeService nodeService ;

    @Autowired
    private InfinispanRepositoryService infinispznService ;


    @Autowired
    private EventBroker eventBroker ;

    public void createNodeType(ExecuteContext context){
        Node node = context.getNode() ;
        if(NodeType.NODE.equals(node.getStringValue(NodeType.REPOSITORY_TYPE)) || NodeType.NODE.equals(node.getStoreValue(NodeType.REPOSITORY_TYPE))){
            Node eventNode = createEvent(node, CREATE);
//            createEventAction(eventNode, CREATE) ;
            eventNode = createEvent(node, UPDATE);
//            createEventAction(eventNode, UPDATE) ;
            eventNode = createEvent(node, DELETE);
//            createEventAction(eventNode, DELETE) ;
            eventNode = createEvent(node, ALL_EVENT);
        }
    }

    private Node createEvent(Node node, String event) {
        Map<String, Object> eventData = new LinkedHashMap<>() ;
        eventData.put("tid", node.getId()) ;
        eventData.put(EVENT, event) ;
        eventData.put("eventName", event + " " + node.get("typeName")) ;

        return nodeService.executeNode(eventData, EVENT, CREATE) ;
    }

    private Node createEventAction(Node eventNode, String event) {
        Map<String, Object> eventActionData = new LinkedHashMap<>() ;
        eventActionData.put(EVENT, eventNode.getId()) ;
        eventActionData.put("action", DELETE.equals(event) ? "removeInfinispan" : "putInfinispan") ;
        eventActionData.put("actionType", "service") ;
        eventActionData.put("actionBody", DELETE.equals(event) ? "infinispanService.remove" : "infinispanService.execute") ;
        eventActionData.put("order", 1) ;

        return nodeService.executeNode(eventActionData, EVENT_ACTION, CREATE) ;
    }

    public void execute(ExecuteContext executeContext) {
        NodeType nodeType = executeContext.getNodeType() ;
        if(nodeType.isNode()) {
            infinispznService.execute(executeContext) ;
        }

        Event event = nodeType.getEvent(executeContext.getEvent()) ;
        if(event == null){
            return  ;
        }

        Event allEvent = nodeType.getEvent(ALL_EVENT) ;

        executeEventAction(executeContext, event);
        executeEventAction(executeContext, allEvent);

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
