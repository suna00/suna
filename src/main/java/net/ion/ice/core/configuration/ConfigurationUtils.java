package net.ion.ice.core.configuration;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by jaehocho on 2017. 2. 10..
 */
public class ConfigurationUtils {

    public static List<String> getIpAddress() {
        List<String> ipAddress = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> nienum = NetworkInterface.getNetworkInterfaces();
            while (nienum.hasMoreElements()) {
                NetworkInterface ni = nienum.nextElement();
                Enumeration<InetAddress> kk= ni.getInetAddresses();
                while (kk.hasMoreElements()) {
                    InetAddress inetAddress = kk.nextElement();
                    if (!inetAddress.isLoopbackAddress() &&	!inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress()) {
                        ipAddress.add(inetAddress.getHostAddress().toString());
                    }
                }
            }
            if(ipAddress.size() == 0){
                while (nienum.hasMoreElements()) {
                    NetworkInterface ni = nienum.nextElement();
                    Enumeration<InetAddress> kk= ni.getInetAddresses();
                    while (kk.hasMoreElements()) {
                        InetAddress inetAddress = kk.nextElement();
                        ipAddress.add(inetAddress.getHostAddress().toString());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            if(ipAddress.size() == 0){
                ipAddress.add("127.0.0.1");
            }
        }

        return ipAddress;
    }

    public static String getIp() {
        try {
            Enumeration<NetworkInterface> nienum = NetworkInterface.getNetworkInterfaces();
            while (nienum.hasMoreElements()) {
                NetworkInterface ni = nienum.nextElement();
                Enumeration<InetAddress> kk= ni.getInetAddresses();
                while (kk.hasMoreElements()) {
                    InetAddress inetAddress = kk.nextElement();
                    if (!inetAddress.isLoopbackAddress() &&	!inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress()) {
                        if( (ni.getDisplayName().startsWith("e") && ni.getDisplayName().endsWith("0"))) {
                            return inetAddress.getHostAddress().toString();
                        }
                    }
                }
            }
            while (nienum.hasMoreElements()) {
                NetworkInterface ni = nienum.nextElement();
                Enumeration<InetAddress> kk= ni.getInetAddresses();
                while (kk.hasMoreElements()) {
                    InetAddress inetAddress = kk.nextElement();
                    return inetAddress.getHostAddress().toString();
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
            return "127.0.0.1" ;
        }
        return "127.0.0.1" ;
    }


    public static String getHostName() {
        try{
            return InetAddress.getLocalHost().getHostName() ;
        } catch (UnknownHostException e) {
//            logger.error(e.getMessage(), e);
            return StringUtils.substringBefore(e.getMessage(), ":").trim() ;
        }
    }
}
