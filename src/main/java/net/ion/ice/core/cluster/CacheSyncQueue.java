package net.ion.ice.core.cluster;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by leehh on 2017. 11. 3.
 */

public class CacheSyncQueue extends Thread{
    private Logger logger = LoggerFactory.getLogger(CacheSyncQueue.class);

    private BlockingQueue<CacheMessage> queue = new ArrayBlockingQueue<CacheMessage>(10000) ;

    public void put(CacheMessage msg){
        try{
            this.queue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        while(true){
            try{
                CacheMessage msg = this.queue.take();
                logger.info("retry cache : " + msg);
                try {
                    Map<String, Object> data = ClusterUtils.callNode(msg.getServer(), msg.getTypeId(), msg.getId());
                    if (data != null) {
                        Node node = new Node(data);
                        NodeUtils.getInfinispanService().cacheNode(node);
                    } else if (msg.getRetry() < 3) {
                        msg.incrementRetry();
                        put(msg);
                    }
                }catch (Exception e){
                    if (msg.getRetry() < 3) {
                        msg.incrementRetry();
                        put(msg);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
