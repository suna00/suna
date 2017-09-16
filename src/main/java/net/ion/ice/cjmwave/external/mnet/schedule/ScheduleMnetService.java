package net.ion.ice.cjmwave.external.mnet.schedule;

import net.ion.ice.cjmwave.db.sync.DBSyncService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by juneyoungoh on 2017. 9. 14..
 * 전체 프로세스에 대한 에러 처리는 이 서비스를 사용하는 구현체에서 할 것
 */
@Service
public class ScheduleMnetService {
    // 주기적으로 도는 인터페이스는 단순함
    // 쿼리 날릴 때 날짜는 MIG_HISTORY 테이블에서 mig_type 이 SCHEDULE 인 것의 날짜정렬 limit 1을 가져와서 처리함

    /*
    작업을 수행하기 위해서
    DBSyncService 가 필요함
    * */

    @Autowired
    private DBSyncService dbSyncService;

    private static Logger logger = Logger.getLogger(ScheduleMnetService.class);

    public void executeScheduledMigration(String target){
    }
}
