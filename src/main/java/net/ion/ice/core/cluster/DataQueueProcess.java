package net.ion.ice.core.cluster;

import com.hazelcast.core.IQueue;
import net.ion.ice.core.data.DBService;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

public class DataQueueProcess implements Runnable{
    private static Logger logger = LoggerFactory.getLogger(DataQueueProcess.class) ;

    private final IQueue<JdbcSqlData> queue ;
    private final String dataQueueDs ;
    private JdbcTemplate jdbcTemplate ;

    public DataQueueProcess(IQueue dataQueue, String dataQueueDs) {
        this.queue =dataQueue ;
        this.dataQueueDs = dataQueueDs ;
    }


    @Override
    public void run() {
        logger.info("START DATA QUEUE TAKE ");

        while(true){
            try {
                JdbcSqlData sqlData = queue.take() ;
                if(jdbcTemplate == null){
                    jdbcTemplate = DBService.getJdbc(dataQueueDs) ;
                }
                logger.info("update data queue sql {} {}", sqlData.getSql(), sqlData.getParams());
                jdbcTemplate.update(sqlData.getSql(), sqlData.getParams());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
