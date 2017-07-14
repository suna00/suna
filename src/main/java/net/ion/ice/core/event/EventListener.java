package net.ion.ice.core.event;

import net.ion.ice.core.node.Node;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public class EventListener {
    
    private Node eventListenerNode ;
    private Action action;

    public EventListener(Node node) {
        this.eventListenerNode = node ;    
    }

    public Action getAction() {
        if(action == null){
            action = Action.create(eventListenerNode.getStringValue(Action.ACTION_TYPE), eventListenerNode.getStringValue(Action.DATASOURCE), eventListenerNode.getStringValue(Action.ACTION_BODY)) ;
        }
        return action;
    }

    public String getEvent() {
        return StringUtils.substringAfterLast(eventListenerNode.getStringValue(Event.EVENT), "@");
    }

    public String getTid() {
        return StringUtils.substringBefore(eventListenerNode.getStringValue(Event.EVENT), "@");
    }
}
