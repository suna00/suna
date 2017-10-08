package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.ApplicationContextManager;

/**
 * Created by juneyoungoh on 2017. 10. 1..
 * artist, album, song, musicVideo
 */
public abstract class ParallelDBSyncExecutor implements Runnable {

    public DBSyncService dbSyncService;
    public String executeId;
    public DBProcessStorage storage;
    private boolean isRun = false;

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
            System.out.println("Runnable with [ " + executeId + " ] is already running. Ignore this request");
            return;
        } else {
            System.out.println("Runnable with [ " + executeId + " ] is getting started. Accept this request");
            storage.addProcess(executeId, this);
        }
        try{
            action();
            this.isRun = true;
        } catch (Exception e) {
            System.out.println("PARALLEL DB SYNC EXECUTOR ERROR :: " );
            e.printStackTrace();
            System.out.println("TURNING STATUS TERMINATED");
            this.isRun = false;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.isRun = false;
    }
};