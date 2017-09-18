package net.ion.ice.cjmwave.external.mnet.schedule;

import net.ion.ice.cjmwave.db.sync.DBSyncService;
import net.ion.ice.cjmwave.external.mnet.data.MnetDataDumpService;
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

    @Autowired
    private MnetDataDumpService mnetDataDumpService;

    private static Logger logger = Logger.getLogger(ScheduleMnetService.class);

    /*
    * MSSQL - MySQL 이 중간에 실패하더라도
    * 신규 변경분에 대해 MySQL - Node 는 작업이 이루어짐
    * */
    public void executeScheduledMigration(String target){
        // MSSQL 에서 MYSQL 으로 옮기는 작업
        logger.info("Migration schedule :: Step1");
        mnetDataDumpService.copyData(target);
        logger.info("Migration schedule :: Step2");
        // MYSQL 에서 노드로 옮기는 작업 -
        // 증분 테이블이 별도로 있던지, 아니면 이미 처리된 부분을 삭제하던지
        // 아니면 레플리카 테이블에 마지막 update 날짜를 기록하던지
    }
}
