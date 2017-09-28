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

    /*
    * 다국어는 단독으로 증분을 판별할 수 없기 때문에 별도 실행 할 수 없음
    * */
    private String [] mnetExecuteIds = {
            "albumPart"
            , "artistPart"
            , "musicVideoPart"
            , "songPart"
    };


    public void execute(String type, Date provided) {
        try{
            logger.info("MnetDataDumpService.execute :: " + type);

            //신규 증분에 대한 MSSQL to MySQL 마이그레이션 수행
            logger.info("Migration schedule :: Step1");
            mnetDataDumpService.copyData(type, provided);


            logger.info("Migration schedule :: Step2");
            // 이전 실행시간 히스토리에서 가져와서 파라미터로 수행한다
            // dbSyncProcess 의 주기적 쿼리를 실행하도록 처리
            switch (type) {
                case "all" :
                    for(String executeId : mnetExecuteIds) {
                        dbSyncService.executeForNewData("mnet", executeId, provided);
                    }
                    break;
                case "album" :
                    dbSyncService.executeForNewData("mnet", "albumPart", provided);
                    break;
                case "artist" :
                    dbSyncService.executeForNewData("mnet", "artistPart", provided);
                    break;
                case "song" :
                    dbSyncService.executeForNewData("mnet", "songPart", provided);
                    break;
                case "mv" :
                    dbSyncService.executeForNewData("mnet", "musicVideoPart", provided);
                    break;
                case "chart" :
                    dbSyncService.executeForNewData("mnet", "mcdChartBasInfoPart", provided);
                    dbSyncService.executeForNewData("mnet", "mcdChartStatsPart", provided);
                    break;
                default:
                    logger.info("Could not find appropriate type for migration");
                    break;
            }

        } catch (Exception e) {
            logger.error("FAILED TO EXECUTE MNET MIGRATION :: ", e);
        }
    }
};
