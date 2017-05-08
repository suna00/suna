package net.ion.ice.core.configuration;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 16..
 */
public class ClusterConfigurationHandler implements ConfigurationHandler {
    @Override
    public Collection<Map<String, Object>> getConfigList(String type) throws IOException {
        return null;
    }


    @Override
    public boolean checkLock(String type) throws IOException {
        return false;
    }

    @Override
    public void releaseLock(String type) {

    }

    @Override
    public void writeConfig(String type, Collection<Map<String, Object>> list) throws IOException {

    }
}
