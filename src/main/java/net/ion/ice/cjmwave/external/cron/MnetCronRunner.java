package net.ion.ice.cjmwave.external.cron;

import net.ion.ice.cjmwave.external.mnet.schedule.ScheduledMnetService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by juneyoungoh on 2017. 10. 12..
 */
@Component
public class MnetCronRunner {

    @Autowired
    ScheduledMnetService scheduledMnetService;

    private Logger logger = Logger.getLogger(MnetCronRunner.class);

    @Scheduled(cron="0 */10 * * * *")
    public void execute(){
        System.out.println("===========================================");
        System.out.println("EXECUTE SPRING SCHEDULER FOR MNET MIGRATION");
        System.out.println("===========================================");
        scheduledMnetService.execute("all", null);
    }
}
