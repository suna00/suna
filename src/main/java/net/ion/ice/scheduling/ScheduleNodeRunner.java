package net.ion.ice.scheduling;

import com.hazelcast.core.HazelcastInstance;
import net.ion.ice.core.event.Action;
import net.ion.ice.core.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;

public class ScheduleNodeRunner implements Runnable{
    private static Logger logger = LoggerFactory.getLogger(ScheduleNodeRunner.class);

    private Node scheduleNode ;
    HazelcastInstance hazelcastInstance;

    public ScheduleNodeRunner(Node scheduleNode, HazelcastInstance hazelcastInstance) {
        this.scheduleNode = scheduleNode ;
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public void run() {
        logger.info("schedule run : " + scheduleNode.getId() + " with Hazelcast :: " + hazelcastInstance);
        Lock clusterScheduleLock = hazelcastInstance.getLock(scheduleNode.getId());
        clusterScheduleLock.lock();
        logger.info("Hazelcast Lock :: " + String.valueOf(clusterScheduleLock));

        try{
            Action action = Action.create(scheduleNode.getStringValue(Action.ACTION_TYPE), scheduleNode.getStringValue(Action.DATASOURCE), scheduleNode.getStringValue(Action.ACTION_BODY)) ;
            action.execute();
        } catch (Exception e) {
            e.printStackTrace();
            // 아마도 여기서 쓰레드 죽여야 하지 않을까.
        } finally {
            logger.info("Release Hazelcast Lock :: " + String.valueOf(clusterScheduleLock));
            clusterScheduleLock.unlock();
        }
    }
}
