package net.ion.ice.core.event;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.context.Context;
import net.ion.ice.core.context.ExecuteContext;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public class ActionService extends Action {

    private String serviceName ;
    private String methodName ;

    private Object service ;
    private Method method ;


    @Override
    public void execute(ExecuteContext executeContext) {
        if(service == null){
            service = ApplicationContextManager.getBean(serviceName) ;
        }

        if(method == null){
            for (Method _method : service.getClass().getMethods()) {
                if (methodName.equals(_method.getName()) && _method.getParameterTypes().length == 1 && Context.class.isAssignableFrom(_method.getParameterTypes()[0])) {
                    this.method = _method ;
                    break;
                }
            }
        }

        try {
            method.invoke(service, executeContext) ;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public ActionService(String datasource, String actionBody) {
        super(datasource, actionBody);
        this.serviceName = StringUtils.substringBeforeLast(actionBody, ".") ;
        this.methodName = StringUtils.substringAfterLast(actionBody, ".") ;
    }
}
