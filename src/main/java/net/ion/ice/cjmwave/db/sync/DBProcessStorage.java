package net.ion.ice.cjmwave.db.sync;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by juneyoungoh on 2017. 10. 1..
 */
@Component
public class DBProcessStorage {

    private ConcurrentHashMap<String, ParallelDBSyncExecutor> dbSyncProcessStorage;

    @PostConstruct
    public void init(){
        dbSyncProcessStorage = new ConcurrentHashMap<>();
    }

    public ParallelDBSyncExecutor getProcess(String executeId) {
        return dbSyncProcessStorage.get(executeId);
    }

    public void addProcess(String executeId, ParallelDBSyncExecutor task) {
        dbSyncProcessStorage.put(executeId, task);
    }

    public Map<String, ParallelDBSyncExecutor> getProcessStorage(){
        return this.dbSyncProcessStorage;
    }

    public boolean isAbleToRun(String executeId) {
        return (dbSyncProcessStorage.get(executeId) == null || !dbSyncProcessStorage.get(executeId).isRun());
    }
}
