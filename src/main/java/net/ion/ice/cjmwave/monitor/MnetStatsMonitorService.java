package net.ion.ice.cjmwave.monitor;


import net.ion.ice.core.data.DBService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by juneyoungoh on 2017. 10. 25..
 */
@Service
public class MnetStatsMonitorService {

    @Autowired
    DBService dbService;

    private Logger logger = Logger.getLogger(MnetStatsMonitorService.class);
    private ConcurrentHashMap<String, IpAddress> IpAddressMap = new ConcurrentHashMap<>();
    private JdbcTemplate authReplicaTemplate;


    @PostConstruct
    public void init(){
        try{
            authReplicaTemplate = dbService.getJdbcTemplate("authReplica");
        } catch (Exception e) {
            logger.error("Stat Monitoring function disabled, since :: ", e);
        }
    }




}
