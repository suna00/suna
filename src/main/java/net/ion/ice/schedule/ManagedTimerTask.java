package net.ion.ice.schedule;

import java.util.Date;
import java.util.TimerTask;

/**
 * Created by juneyoungoh on 2017. 8. 21..
 */
public abstract class ManagedTimerTask extends TimerTask {

    private boolean running = false;
    private Date lastExecution = new Date();

    @Override
    public void run() {
        this.running = true;
    }

    @Override
    public boolean cancel() {
        this.running = false;
        return super.cancel();
    }

    public boolean isRunning (){
        return this.running;
    }

    public Date getLastExecution (){
        return this.lastExecution;
    }
}
