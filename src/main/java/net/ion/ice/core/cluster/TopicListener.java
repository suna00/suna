package net.ion.ice.core.cluster;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import net.ion.ice.core.context.Context;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class TopicListener implements MessageListener<String>{
    private Logger logger = LoggerFactory.getLogger(TopicListener.class);

    private ClusterConfiguration clusterConfiguration ;


    private String uuid ;

    private CacheSyncQueue cacheSyncQueue ;

    public TopicListener(ClusterConfiguration clusterConfiguration) {
        this.clusterConfiguration = clusterConfiguration ;
        this.uuid = clusterConfiguration.getHazelcast().getCluster().getLocalMember().getUuid() ;
        this.cacheSyncQueue = new CacheSyncQueue();
        this.cacheSyncQueue.setDaemon(true);
        this.cacheSyncQueue.start();
    }


    @Override
    public void onMessage(Message<String> message) {
//        logger.info(message.getPublishingMember().getUuid() + "=" + clusterConfiguration.getHazelcast().getCluster().getLocalMember().getUuid());

        if(uuid.equals(message.getPublishingMember().getUuid())){
            return ;
        }
        String msg = message.getMessageObject() ;
        String[] msgs = StringUtils.split(msg, "::") ;
        String event = msgs[0] ;
        String typeId = msgs[1] ;
        String id = msgs[2] ;
        if(msgs.length == 4){
            id = id + "::" + msgs[3] ;
        }else if(msgs.length == 5){
            id = id + "::" + msgs[3] + "::" + msgs[4];
        }
        logger.info("{} Cache Sync : {}.{} ", event, typeId, id);

        try {
            if ("delete".equals(event)) {
                NodeUtils.getInfinispanService().deleteNode(typeId, id);
            } else {
                Map<String, Object> data = ClusterUtils.callNode(message.getPublishingMember(), typeId, id) ;
                if(data != null) {
                    Node node = new Node(data);
                    NodeUtils.getInfinispanService().cacheNode(node);
                }else{
                    this.cacheSyncQueue.put(new CacheMessage(message.getPublishingMember(), typeId, id));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            if (!"delete".equals(event)) {
                this.cacheSyncQueue.put(new CacheMessage(message.getPublishingMember(), typeId, id));
            }
        }
    }
}
