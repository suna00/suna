package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.ApplicationContextManager;
import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * Created by juneyoungoh on 2017. 10. 1..
 * artist, album, song, musicVideo
 */
public abstract class ParallelDBSyncExecutor implements Runnable, Serializable {

    public DBSyncService dbSyncService;
    public String executeId;
    public DBProcessStorage storage;
    private boolean isRun = false;

    private Logger logger = Logger.getLogger(ParallelDBSyncExecutor.class);

    public ParallelDBSyncExecutor(String executeId){
        this.executeId = executeId;
        this.dbSyncService = (DBSyncService) ApplicationContextManager.getBean("DBSyncService");
        this.storage = (DBProcessStorage) ApplicationContextManager.getBean("DBProcessStorage");
    }

    public boolean isRun(){
        return this.isRun;
    }

    public abstract void action() throws Exception;

    @Override
    public void run() {
        if(!storage.isAbleToRun(executeId)) {
            logger.info("Runnable with [ " + executeId + " ] is already running. Ignore this request");
            return;
        } else {
            logger.info("Runnable with [ " + executeId + " ] is getting started. Accept this request");
            storage.addProcess(executeId, this);
        }
        try{
            this.isRun = true;
            action();
            this.isRun = false;
        } catch (Exception e) {
            logger.error("PARALLEL DB SYNC EXECUTOR ERROR :: " );
            logger.error(e);
            logger.error("TURNING STATUS TERMINATED");
            this.isRun = false;
        }
    }
};