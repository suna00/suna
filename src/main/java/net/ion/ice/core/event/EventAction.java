package net.ion.ice.core.event;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public class EventAction {
    private Node eventActionNode ;
    private Action action;

    public EventAction(Node node) {
        this.eventActionNode = node ;
    }


    public void execute(ExecuteContext executeContext) {

    }

    public Action getAction() {
        if(action == null){
            action = Action.create(eventActionNode.getStringValue(Action.ACTION_TYPE), eventActionNode.getStringValue(Action.DATASOURCE), eventActionNode.getStringValue(Action.ACTION_BODY)) ;
        }
        return action;
    }

    public String getEvent() {
        return StringUtils.substringAfterLast(eventActionNode.getStringValue(Event.EVENT), "@");
    }

    public String getTid() {
        return StringUtils.substringBeforeLast(eventActionNode.getStringValue(Event.EVENT), "@");
    }

    public Object getId() {
        return eventActionNode.getId();
    }
}
