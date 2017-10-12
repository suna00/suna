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
        logger.info("schedule run : " + scheduleNode.getId());
        try{
            Action action = Action.create(scheduleNode.getStringValue(Action.ACTION_TYPE), scheduleNode.getStringValue(Action.DATASOURCE), scheduleNode.getStringValue(Action.ACTION_BODY)) ;
            action.execute();
        } catch (Exception e) {
            e.printStackTrace();
            // 아마도 여기서 쓰레드 죽여야 하지 않을까.
        }
    }
}
