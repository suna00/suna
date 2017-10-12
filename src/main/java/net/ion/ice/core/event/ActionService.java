package net.ion.ice.core.event;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.IceRuntimeException;
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
        initService();
        try {
            method.invoke(service, executeContext) ;
        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
            if(e.getTargetException() instanceof IceRuntimeException){
                throw (IceRuntimeException) e.getTargetException() ;
            }
        } catch (Exception e) {
            if(e instanceof IceRuntimeException){
                throw (IceRuntimeException) e ;
            }
            throw new IceRuntimeException("ACTION execute Exception : " + e.getMessage(), e) ;
        }
    }

    @Override
    public void execute() {
        initService();
        try {
            method.invoke(service) ;
        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
            if(e.getTargetException() instanceof IceRuntimeException){
                throw (IceRuntimeException) e.getTargetException() ;
            }
        } catch (Exception e) {
            if(e instanceof IceRuntimeException){
                throw (IceRuntimeException) e ;
            }
            throw new IceRuntimeException("ACTION execute Exception : " + e.getMessage(), e) ;
        }
    }

    private void initService() {
        if(service == null){
            service = ApplicationContextManager.getBean(serviceName) ;
        }

        if(method == null){
            for (Method _method : service.getClass().getMethods()) {
                if (methodName.equals(_method.getName())) {
                    if(_method.getParameterTypes().length == 1 && Context.class.isAssignableFrom(_method.getParameterTypes()[0])){
                        //기존
                        this.method = _method;
                        break;
                    }
//                    else {
//                        //스케쥴러용 추가분
//                        this.method = _method;
//                        break;
//                    }
                }
            }
        }
        if(method == null){
            throw new IceRuntimeException("Not Found ACTION Service : " + serviceName + "." + methodName) ;
        }
    }

    public ActionService(String datasource, String actionBody) {
        super(datasource, actionBody);
        this.serviceName = StringUtils.substringBeforeLast(actionBody, ".") ;
        this.methodName = StringUtils.substringAfterLast(actionBody, ".") ;
    }
}
