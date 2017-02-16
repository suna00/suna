package net.ion.ice.configuration;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public interface ConfigurationHandler {
    Collection<Map<String,Object>> getConfigList(String type) throws IOException;


    boolean checkLock(String type) throws IOException;

    void releaseLock(String type);

    void writeConfig(String type, Collection<Map<String, Object>> list) throws IOException;
}
