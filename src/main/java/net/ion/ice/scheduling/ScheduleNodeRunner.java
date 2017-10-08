package net.ion.ice.scheduling;

import net.ion.ice.core.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleNodeRunner implements Runnable{
    private static Logger logger = LoggerFactory.getLogger(ScheduleNodeRunner.class);

    private Node scheduleNode ;


    public ScheduleNodeRunner(Node scheduleNode) {
        this.scheduleNode = scheduleNode ;
    }

    @Override
    public void run() {
        logger.info("schedule run : " + scheduleNode.getId());
    }
}
