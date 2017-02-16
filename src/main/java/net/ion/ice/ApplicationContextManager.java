package net.ion.ice;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Created by jaehocho on 2017. 2. 10..
 */
public class ApplicationContextManager implements ApplicationContextAware{
    private static ApplicationContext context ;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext ;
    }

    public static ApplicationContext getContext(){
        return context ;
    }


}
