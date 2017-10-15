package net.ion.ice.core.cluster;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import net.ion.ice.core.context.Context;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TopicListener implements MessageListener<CacheMessage>{
    private Logger logger = LoggerFactory.getLogger(TopicListener.class);

    private ClusterConfiguration clusterConfiguration ;

    @Autowired
    private InfinispanRepositoryService infinispanRepositoryService ;


    private String uuid ;
    public TopicListener(ClusterConfiguration clusterConfiguration) {
        this.clusterConfiguration = clusterConfiguration ;
        this.uuid = clusterConfiguration.getHazelcast().getCluster().getLocalMember().getUuid() ;
    }


    @Override
    public void onMessage(Message<CacheMessage> message) {
        logger.info(message.getPublishingMember().getUuid() + "=" + clusterConfiguration.getHazelcast().getCluster().getLocalMember().getUuid());

        if(uuid.equals(message.getPublishingMember().getUuid())){
           return ;
        }

        CacheMessage cacheMessage = message.getMessageObject() ;
        Node node = cacheMessage.getNode() ;

        if("delete".equals(cacheMessage.getEvent())){
            infinispanRepositoryService.deleteNode(node);
            logger.info("Delete Cache Sync : {}.{} ", node.getTypeId(), node.getId() ) ;
        }else {
            infinispanRepositoryService.cacheNode(node);
            logger.info("Cache Sync : {}.{} ", node.getTypeId(), node.getId() ) ;
        }
    }
}
