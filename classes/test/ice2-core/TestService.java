package net.ion.ice.core.configuration;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;


/**
 * Created by jaehocho on 2017. 2. 10..
 */

@Service
public class TestService {

    private static Logger logger = LogManager.getLogger(TestService.class) ;

    public TestService(){
        logger.info("Test Service Init : " + System.currentTimeMillis());
    }

    public String toString(){
        return "TEST" ;
    }
}
