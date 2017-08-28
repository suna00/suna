package net.ion.ice.core.cluster;

import com.hazelcast.core.IAtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by jaeho on 2017. 6. 13..
 */
@Service
public class ClusterService {

    @Autowired
    private ClusterConfiguration clusterConfiguration;


    public IAtomicLong getSequence(String sequenceName) {
        IAtomicLong sequence = clusterConfiguration.getIAtomicLong(sequenceName);
        return sequence;
    }

//    public Map<String, Object> getSession(String userToken) {
//        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();
//        return sessionMap.get(userToken);
//    }
//
//    public void putSession(String userToken, Map<String, Object> data) {
//        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();
//        sessionMap.put(userToken, data);
//    }
}
