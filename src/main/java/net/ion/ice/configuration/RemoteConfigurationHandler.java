package net.ion.ice.configuration;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class RemoteConfigurationHandler implements ConfigurationHandler {


    @Override
    public Collection<Map<String, Object>> getConfigList(String type) {
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
    public void writeConfig(String type, Collection<Map<String, Object>> list) {

    }
}
