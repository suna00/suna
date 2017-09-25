package net.ion.ice.core.cluster;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import net.ion.ice.core.context.Context;
import net.ion.ice.core.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicListener implements MessageListener<Node>{
    private Logger logger = LoggerFactory.getLogger(TopicListener.class);


    @Override
    public void onMessage(Message<Node> message) {
        Node node = message.getMessageObject() ;
        logger.info(node.toString() + " : " + message.getPublishingMember());
    }
}
