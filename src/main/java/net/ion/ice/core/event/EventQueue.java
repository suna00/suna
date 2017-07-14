package net.ion.ice.core.event;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by jaeho on 2017. 7. 14..
 */
public class EventQueue extends Thread{
    private BlockingQueue<EventPublish> queue = new LinkedBlockingQueue<>() ;

    public int size() {
        return queue.size() ;
    }

    public void run(){
        while(true){
            try {
                EventPublish eventPublish = queue.take() ;
                EventListener el =eventPublish.getEventListener() ;
                Action action = el.getAction();
                action.execute(eventPublish.getExecuteContext());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void putEventPublish(EventPublish eventPublish) throws InterruptedException {
        queue.put(eventPublish);
    }
}
