package net.ion.ice.core.event;

import net.ion.ice.core.context.ExecuteContext;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public abstract class Action {
    public static final String ACTION_TYPE = "actionType";
    public static final String DATASOURCE = "datasource" ;
    public static final String ACTION_BODY = "actionBody";

    public abstract void execute(ExecuteContext executeContext);


    public enum ActionType {service, update, select, call, function}

    protected String datasource ;
    protected String actionBody ;


    public Action(String datasource, String actionBody) {
        this.datasource = datasource ;
        this.actionBody = actionBody ;
    }


    public static Action create(String actionType, String datasource, String actionBody) {
        ActionType _actionType = ActionType.valueOf(actionType) ;

        switch(_actionType){
            case service:
                return new ActionService(datasource, actionBody) ;
            case update:
                return new ActionUpdate(datasource, actionBody) ;
            default:
                break ;
        }

        return null ;
    }
}
