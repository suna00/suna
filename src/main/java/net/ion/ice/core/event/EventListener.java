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
        return action;
    }

    public String getEvent() {
        return StringUtils.substringAfterLast(eventListenerNode.getStringValue(Event.EVENT), "@");
    }

}
