package net.ion.ice.core.cluster;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import net.ion.ice.core.context.Context;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.node.Node;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class TopicListener implements MessageListener<String>{
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
    public void onMessage(Message<String> message) {
//        logger.info(message.getPublishingMember().getUuid() + "=" + clusterConfiguration.getHazelcast().getCluster().getLocalMember().getUuid());

        if(uuid.equals(message.getPublishingMember().getUuid())){
           return ;
        }

        try {
            String msg = message.getMessageObject() ;
            String[] msgs = StringUtils.split(msg, "::") ;
            String event = msgs[0] ;
            String typeId = msgs[1] ;
            String id = msgs[2] ;
            logger.info("{} Cache Sync : {}.{} ", event, typeId, id);
            if ("delete".equals(event)) {
                infinispanRepositoryService.deleteNode(typeId, id);
            } else {
                Map<String, Object> data = ClusterUtils.callNode(message.getPublishingMember(), typeId, id) ;
                Node node = new Node(data) ;
                infinispanRepositoryService.cacheNode(node);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
