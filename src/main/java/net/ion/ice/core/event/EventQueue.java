package net.ion.ice.core.event;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by jaeho on 2017. 7. 14..
 */
public class EventQueue extends Thread{
    private BlockingQueue<EventListener> queue = new LinkedBlockingQueue<>() ;

    public int size() {
        return queue.size() ;
    }

    public void run(){
        while(true){
            try {
                EventListener el = queue.take() ;
                Action action = (Action) el.getAction();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
