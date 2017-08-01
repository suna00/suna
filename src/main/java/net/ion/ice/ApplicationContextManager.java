package net.ion.ice;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by jaehocho on 2017. 2. 10..
 */
@Configuration
public class ApplicationContextManager implements ApplicationContextAware{
    private Logger logger = LoggerFactory.getLogger(ApplicationContextManager.class);

    private static ApplicationContext context ;

    @Autowired
    private ApplicationContext ctx;

    public ApplicationContextManager(){
        logger.info("CONSTRUCT Application Context Aware");
        context = this.ctx;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        logger.info("INIT Application Context Aware");
        this.context = applicationContext ;
    }

    public static ApplicationContext getContext(){
        return context ;
    }

    public static Resource getResource(String location){
        if (context == null) return null;
        return context.getResource(location) ;
    }


    public static Resource[] getResources(String location) throws IOException {
        return context.getResources(location) ;
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
