package net.ion.ice.core.cluster;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import net.ion.ice.core.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicListener implements MessageListener<Context>{
    private Logger logger = LoggerFactory.getLogger(TopicListener.class);


    @Override
    public void onMessage(Message<Context> message) {
        Context context = message.getMessageObject() ;
        logger.info(context.toString() + " : " + message.getPublishingMember());
    }
}
