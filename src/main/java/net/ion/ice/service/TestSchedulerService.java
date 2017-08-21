package net.ion.ice.service;

import org.apache.log4j.Logger;

import java.util.Date;

/**
 * Created by juneyoungoh on 2017. 8. 21..
 */
public class TestSchedulerService {
    Logger logger = Logger.getLogger(TestSchedulerService.class);

    public void doA() {
        logger.info("doA :: " + String.valueOf(new Date()));
    }

    public void doB() {
        logger.info("doB :: " + String.valueOf(new Date()));
    }
}