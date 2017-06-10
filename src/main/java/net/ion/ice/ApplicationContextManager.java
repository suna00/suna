package net.ion.ice;

import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Created by jaehocho on 2017. 2. 10..
 */
@Component
public class ApplicationContextManager implements ApplicationContextAware{
    private static ApplicationContext context ;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext ;
    }

    public static ApplicationContext getContext(){
        return context ;
    }

    public static Resource getResource(String location){
        return context.getResource(location) ;
    }

    public static HazelcastInstance getHazelcastIntance(){
        return context.getBean(HazelcastInstance.class) ;
    }

    public static <T> T getBean(Class<T> clazz){
        return context.getBean(clazz) ;
    }

    public static Object getBean(String name){
        return context.getBean(name) ;
    }
}
