package net.ion.ice.core.session;

import net.ion.ice.core.cluster.ClusterConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SessionService {
    @Autowired
    private ClusterConfiguration clusterConfiguration;

    public Map<String, Object> getSession(String userToken) {
        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();
        return sessionMap.get(userToken);
    }

    public void putSession(String userToken) {

        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();
        Map<String, Object> data = new HashMap<>();
        sessionMap.put(userToken, data);
    }

    public Object getValue(String userToken, String key){
        Map<String, Map<String, Object>> sessionMap = clusterConfiguration.getSesssionMap();
        return sessionMap.get(userToken).get(key);
    }
}
