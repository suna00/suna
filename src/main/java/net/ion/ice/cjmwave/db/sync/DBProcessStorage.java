package net.ion.ice.cjmwave.db.sync;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by juneyoungoh on 2017. 10. 1..
 */
@Component
public class DBProcessStorage {

    private ConcurrentHashMap<String, Thread> dbSyncProcessStorage;

    @PostConstruct
    public void init(){
        dbSyncProcessStorage = new ConcurrentHashMap<>();
    }

    public Thread getProcess(String executeId) {
        return dbSyncProcessStorage.get(executeId);
    }

    public void addProcess(String executeId, Thread task) {
        dbSyncProcessStorage.put(executeId, task);
    }

    public void killProcess(String executeId) {
        Thread process = getProcess(executeId);
        if(process != null) process.interrupt();
    }

    public boolean isAbleToRun(String executeId) {
        return (dbSyncProcessStorage.get(executeId) == null);
    }
}
