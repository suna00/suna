package net.ion.ice.cjmwave.monitor;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by juneyoungoh on 2017. 10. 25..
 */
public class IpAddress implements Serializable {
    private String ip;
    private Date created;

    public IpAddress(String ip, Date created) {
        this.ip = ip;
        this.created = created;
    }

    public Date getCreated (){
        return this.created;
    }

    public String getIp(){
        return this.ip;
    }
}
