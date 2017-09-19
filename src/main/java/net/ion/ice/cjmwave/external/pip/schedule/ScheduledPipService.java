package net.ion.ice.cjmwave.external.pip.schedule;

import net.ion.ice.cjmwave.external.pip.PipApiService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by juneyoungoh on 2017. 9. 13..
 * 72 시간 단위로 execute 되며 이 부분은 net.ion.ice.schedule 에서 읽힘
 */
@Service
public class ScheduledPipService {

    @Autowired
    private PipApiService pipService;
    private final String PLATFORM = "mnetjapan";

    private Logger logger = Logger.getLogger(ScheduledPipService.class);

    public void execute() {
        try{
            boolean save = true;
            pipService.doProgramMigration("type=recent", save);
            pipService.doClipMediaMigration("type=recent&platform=" + PLATFORM, save);
        } catch (Exception e) {
            logger.error("FAILED TO EXECUTE PIP MIGRATION :: ", e);
        }
    }
}
