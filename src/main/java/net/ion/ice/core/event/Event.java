package net.ion.ice.core.event;

import net.ion.ice.core.node.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public class Event {
    public static final String EVENT = "event";

    private Node eventNode;
    private List<EventAction> eventActions;
    private List<EventListener> eventListeners;

    public Event(Node node) {
        this.eventNode = node ;
    }


    public String getEvent() {
        return eventNode.getStringValue(EVENT);
    }


    public void setEventActions(List<Node> eventActionList) {
        eventActions = new ArrayList<>(eventActionList.size()) ;
        for(Node node : eventActionList){
            eventActions.add(new EventAction(node)) ;
        }
    }

    public void setEventListeners(List<Node> eventListenerList) {
        eventListeners = new ArrayList<>(eventListenerList.size()) ;
        for(Node node : eventListenerList){
            eventListeners.add(new EventListener(node)) ;
        }

    }

    public List<EventAction> getEventActions() {
        return eventActions;
    }
}
