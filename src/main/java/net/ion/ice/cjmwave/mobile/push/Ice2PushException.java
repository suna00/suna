package net.ion.ice.cjmwave.mobile.push;

/**
 * Created by juneyoungoh on 2017. 9. 29..
 */
public class Ice2PushException extends Exception {
    public Ice2PushException(String msg) {
        super(msg);
    }
    public Ice2PushException(String msg, Exception e) {
        super(msg, e);
    }
}
