package net.ion.ice.scheduling;

import net.ion.ice.core.event.Action;
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
        Action action = Action.create(scheduleNode.getStringValue(Action.ACTION_TYPE), scheduleNode.getStringValue(Action.DATASOURCE), scheduleNode.getStringValue(Action.ACTION_BODY)) ;

        logger.info("schedule run : " + scheduleNode.getId());

        action.execute();
    }
}
