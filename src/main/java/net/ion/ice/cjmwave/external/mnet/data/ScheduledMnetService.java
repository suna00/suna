package net.ion.ice.cjmwave.external.mnet.data;

import net.ion.ice.cjmwave.db.sync.DBSyncService;
import net.ion.ice.core.data.DBService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

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


    public void execute(String type) {
        try{
            // 이전 실행시간 히스토리에서 가져와서 파라미터로 수행한다
            // dbSyncProcess 의 주기적 쿼리를 실행하도록 처리
            String [] mnetTargets = {
                "migAlbumPart", "migAlbumMultiPart"
                , "migArtistPart", "migArtistMultiPart"
                , "migMusicVideoPart", "migMusicVideoMultiPart"
                , "migSongPart", "migSongMultiPart"
                , "migChartMstPart", "migChartLstPart"
            };

            switch (type.toUpperCase()) {
                case "ALL" :
                        for(String mnetTarget : mnetTargets) {
                            dbSyncService.executeForNewData("mnet", mnetTarget);
                        }
                    break;
                default:
                        dbSyncService.executeForNewData("mnet", type);
                    break;
            }
        } catch (Exception e) {
            logger.error("FAILED TO EXECUTE MNET MIGRATION :: ", e);
        }
    }
};
