package net.ion.ice.core.event;

import net.ion.ice.core.node.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public class Event implements Serializable{
    public static final String EVENT = "event";
    public static final String NONE_EXECUTE = "noneExecute";

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

    public List<EventListener> getEventListeners() {
        return eventListeners;
    }

    public void addEventAction(EventAction eventAction) {
        if(eventActions == null){
            eventActions = new ArrayList<>() ;
        }
        boolean exist = false ;
        for(EventAction action : eventActions){
            if(action.getId().equals(eventAction.getId())){
                action = eventAction ;
                exist = true;
                break ;
            }
        }
        if(!exist) {
            eventActions.add(eventAction);
        }
    }

    public void addEventListener(EventListener eventListener) {
        if(eventListeners == null){
            eventListeners = new ArrayList<>() ;
        }
        boolean exist = false ;
        for(EventListener listener : eventListeners){
            if(listener.getId().equals(eventListener.getId())){
                listener = eventListener ;
                exist = true;
                break ;
            }
        }
        if(!exist) {
            eventListeners.add(eventListener);
        }
    }

    public String getId() {
        return eventNode.getId();
    }

    public boolean isNoneExecute(){
        return eventNode.getBooleanValue(NONE_EXECUTE) ;
    }
}
