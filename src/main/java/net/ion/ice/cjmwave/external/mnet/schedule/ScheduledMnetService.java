package net.ion.ice.cjmwave.external.mnet.schedule;

import net.ion.ice.cjmwave.db.sync.DBSyncService;
import net.ion.ice.cjmwave.external.mnet.data.MnetDataDumpService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Created by juneyoungoh on 2017. 9. 19..
 * 주기연동을 함
 * 나중에 스케쥴러에서 인젝트해서 주기 연동 호출
 */
@Service
public class ScheduledMnetService {

    private Logger logger = Logger.getLogger(ScheduledMnetService.class);

    @Autowired
    DBSyncService dbSyncService;

    @Autowired
    private MnetDataDumpService mnetDataDumpService;

    public void execute(String type, Date provided) {
        try{
            logger.info("MnetDataDumpService.execute :: " + type);

            System.out.println("###############################");
            System.out.println(type + " :: Start copy Data :: " + new Date());
            System.out.println("###############################");

            //신규 증분에 대한 MSSQL to MySQL 마이그레이션 수행
            mnetDataDumpService.copyData(type, provided);
            // 이전 실행시간 히스토리에서 가져와서 파라미터로 수행한다
            // dbSyncProcess 의 주기적 쿼리를 실행하도록 처리

            System.out.println("###############################");
            System.out.println(type + " :: Start generate Node :: " + new Date());
            System.out.println("###############################");
            dbSyncService.executeForNewData("mnet", type, provided);
        } catch (Exception e) {
            logger.error("FAILED TO EXECUTE MNET MIGRATION :: ", e);
        }
    }


    public void executeAuto() {
        try{
            System.out.println("SCHEDULING MNET MIGRATION EXECUTED");
            String type = "all";
            Date provided = null;
            logger.info("MnetDataDumpService.execute :: " + type);
            //신규 증분에 대한 MSSQL to MySQL 마이그레이션 수행
            mnetDataDumpService.copyData(type, provided);
            // 이전 실행시간 히스토리에서 가져와서 파라미터로 수행한다
            // dbSyncProcess 의 주기적 쿼리를 실행하도록 처리
            dbSyncService.executeForNewData("mnet", type, provided);
        } catch (Exception e) {
            logger.error("FAILED TO EXECUTE MNET MIGRATION :: ", e);
        }
    }
};
