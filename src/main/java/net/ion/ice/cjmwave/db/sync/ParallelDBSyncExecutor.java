package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.ApplicationContextManager;

/**
 * Created by juneyoungoh on 2017. 10. 1..
 * artist, album, song, musicVideo
 */
public abstract class ParallelDBSyncExecutor extends Thread {

    public DBSyncService dbSyncService;
    public String executeId;
    public DBProcessStorage storage;



    public ParallelDBSyncExecutor(String executeId){
        this.executeId = executeId;
        this.dbSyncService = (DBSyncService) ApplicationContextManager.getBean("DBSyncService");
        this.storage = (DBProcessStorage) ApplicationContextManager.getBean("DBProcessStorage");
    }


    public void executeMigration(){
        Thread check = storage.getProcess(executeId);
        if(check != null) {
            System.out.println("Thread with [ " + executeId + " ] is already exists. Ignore this request");
            return;
        } else {
            System.out.println("Thread with [ " + executeId + " ] is getting started. Accept this request");
            storage.addProcess(executeId, this);
        }
        this.run();
    }
};