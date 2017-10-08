package net.ion.ice.cjmwave.external.monitor;

import net.ion.ice.cjmwave.db.sync.DBProcessStorage;
import net.ion.ice.cjmwave.db.sync.ParallelDBSyncExecutor;
import net.ion.ice.core.data.DBService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by juneyoungoh on 2017. 10. 2..
 */
@Service
public class MigrationMonitorService {

    private Logger logger = Logger.getLogger(MigrationMonitorService.class);

    @Autowired
    DBProcessStorage dbProcessStorage;

    @Autowired
    DBService dbService;

    private JdbcTemplate ice2template;

    @PostConstruct
    public void init () {
        try{
            ice2template = dbService.getJdbcTemplate("cjDb");
        } catch (Exception e) {
            logger.error("Migration Monitoring is disabled, Since :: " + e.getClass().getName());
        }
    }


    public List<Map<String, Object>> getMigrationHistory() throws Exception {
        String query = "SELECT * FROM MIG_HISTORY ORDER BY MIG_SEQ DESC";
        return ice2template.queryForList(query);
    }

    public List<Map<String, Object>> getFailureHistory() throws Exception {
        String query = "SELECT * FROM NODE_CREATION_FAIL WHERE ISFIXED = 0 ORDER BY SEQ DESC";
        return ice2template.queryForList(query);
    }

    // 프로세스 목록 반환
    public List<Map<String, Object>> getProcessList () throws Exception {
        List<Map<String, Object>> processList = new ArrayList<>();
        Map<String, ParallelDBSyncExecutor> storage = dbProcessStorage.getProcessStorage();
        Iterator<String> iter = storage.keySet().iterator();
        while(iter.hasNext()) {
            String k = iter.next();
            ParallelDBSyncExecutor t = dbProcessStorage.getProcess(k);
            Map<String, Object> infoMap = new HashMap<String, Object>();
            infoMap.put("executeId", k);
            infoMap.put("status", t.isRun() ? "RUNNING" : "TERMINATED");
            processList.add(infoMap);
        }
        return processList;
    }
}
