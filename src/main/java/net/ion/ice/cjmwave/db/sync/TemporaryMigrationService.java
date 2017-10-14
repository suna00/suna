package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.cjmwave.external.mnet.data.MnetDataDumpService;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.NodeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 10. 13..
 */
@Service
public class TemporaryMigrationService {
    //요청한 ID 들에 대해 아래 작업을 수행함
    /*
    * - MSSQL -> MYSQL (원본 및 부가 테이블)
    * - MYSQL -> 노드 생성 (S3 이미지 업로드)
    * */

    private Logger logger = Logger.getLogger(TemporaryMigrationService.class);
    private static final String REPLICA_TID = "msSqlReplication", DBSYNC_TID = "dbSyncProcess";
    private JdbcTemplate ice2Template;

    @Autowired
    NodeService nodeService;

    @Autowired
    DBService dbService;

    @Autowired
    MnetDataDumpService mnetDataDumpService;

    @Autowired
    DBSyncService dbSyncService;

    @PostConstruct
    public void init(){
        try{
            ice2Template = dbService.getJdbcTemplate("cjDb");
        } catch (Exception e) {
            logger.error("Temporary Migration Function is not able at this moment");
        }
    }


    /*
    *
    * */
    private void migrateMSSQLDataToMYSQL(String type, List<String> ids) {
        logger.info("==========================================\n"
                + "Temporary Migration Step #1 :: START\n"
                + type + " is getting started\n" +
                "==========================================");

        try {
            mnetDataDumpService.copyData(type, ids);
        } catch (Exception e) {
            logger.error("Failed in STEP 1", e);
        }

        logger.info("==========================================\n"
                + "Temporary Migration Step #1 :: FINISHED\n"
                + type + " is done\n" +
                "==========================================");
    }

    private void migrateMYSQLDataToNode(String type, List<String> ids) {
        logger.info("==========================================\n"
                + "Temporary Migration Step #2 :: START\n"
                + type + " is getting started\n" +
                "==========================================");

        try{
            dbSyncService.executeForTempData("mnet", type, ids);
        } catch (Exception e) {
            logger.error("Failed in STEP 2", e);
        }

        logger.info("==========================================\n"
                + "Temporary Migration Step #2 :: FINISHED\n"
                + type + " is done\n" +
                "==========================================");
    }


    public void doTemporaryMigration(String type, List<String> ids){
        logger.info("READY SET GO");
        migrateMSSQLDataToMYSQL(type, ids);
        migrateMYSQLDataToNode(type, ids);
        logger.info("DONE");
    }
}
