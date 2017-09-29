package net.ion.ice.cjmwave.mobile.push;

/**
 * Created by juneyoungoh on 2017. 9. 29..
 */
public interface MobilePushService <T,M,R,Ex extends Exception> {

    public R sendMessageToTargets(T target, M messageObject) throws Ex;
}
