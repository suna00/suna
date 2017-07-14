package net.ion.ice.core.event;

import net.ion.ice.core.context.ExecuteContext;

/**
 * Created by jaeho on 2017. 7. 14..
 */
public class EventPublish {
    private EventListener eventListener ;
    private ExecuteContext executeContext ;

    public EventPublish(EventListener eventListener, ExecuteContext executeContext) {
        this.eventListener = eventListener ;
        this.executeContext = executeContext ;
    }


    public EventListener getEventListener() {
        return eventListener;
    }

    public ExecuteContext getExecuteContext() {
        return executeContext;
    }
}
