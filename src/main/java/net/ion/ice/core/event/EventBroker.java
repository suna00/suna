package net.ion.ice.core.event;

import net.ion.ice.core.context.ExecuteContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jaeho on 2017. 7. 14..
 */
@Component
public class EventBroker {

    private List<EventQueue> queues = new ArrayList<EventQueue>();

    @PostConstruct
    public void initQueue(){
        for(int i=0; i < 3; i++){
            EventQueue queue = new EventQueue() ;
            queue.start() ;
//            queue.setDaemon(true);

            queues.add(queue) ;
        }
    }


    public void putEvent(EventListener eventListener, ExecuteContext executeContext){

        EventPublish eventPublish = new EventPublish(eventListener, executeContext);

        int fqSize = queues.get(0).size() ;
        int sqSize = queues.get(1).size() ;
        int tqSize = queues.get(2).size() ;

        try {
            if(fqSize <= sqSize && fqSize <= tqSize){
                queues.get(0).putEventPublish(eventPublish) ;
            }else if(sqSize <= tqSize){
                queues.get(1).putEventPublish(eventPublish) ;
            }else{
                queues.get(2).putEventPublish(eventPublish) ;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
