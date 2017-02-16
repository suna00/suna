package net.ion.ice.configuration;

import net.ion.ice.CoreConfig;
import net.ion.ice.json.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
public class LocalConfigurationHandler implements ConfigurationHandler {
    private File configRoot  ;

    public LocalConfigurationHandler(){
        configRoot = new File((String) CoreConfig.getConfigValue("configuration.path")) ;
        if(!configRoot.exists()){
            configRoot.mkdirs() ;
        }
    }

    @Override
    public Collection<Map<String, Object>> getConfigList(String type) throws IOException {
        File configDir = getConfigTypeDir(type);

        List<File> files =Arrays.stream(configDir.listFiles())
                .filter(file -> file.isFile() && file.getName().endsWith(".json"))
                .sorted((f1, f2) -> f2.getName().compareTo(f1.getName()))
                .limit(1).collect(Collectors.toList());

        if(files.size() == 0) {
            return new ArrayList<Map<String, Object>>();
        }

        Collection<Map<String, Object>> result = JsonUtils.parsingJsonFileToList(files.get(0)) ;
        return result ;
    }

    private File getConfigTypeDir(String type) {
        File configDir = new File(configRoot, type) ;
        if(!configDir.exists()){
            configDir.mkdirs() ;
        }
        return configDir;
    }

    @Override
    public boolean checkLock(String type) throws IOException {
        File configDir = getConfigTypeDir(type);
        File lockFile = new File(configDir, type + ".lock") ;
        if(lockFile.exists()) {
            if((System.currentTimeMillis() - lockFile.lastModified()) > (30 * 1000 * 60)){
                lockFile.delete() ;
            }else{
                for (int i=0; i<5; i++){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if(!lockFile.exists()){
                        break ;
                    }
                }
            }
            if(lockFile.exists()){
                return false;
            }
        }
        return lockFile.createNewFile() ;
    }

    @Override
    public void releaseLock(String type) {
        File configDir = getConfigTypeDir(type);
        File lockFile = new File(configDir, type + ".lock") ;
        lockFile.delete() ;
    }

    @Override
    public void writeConfig(String type, Collection<Map<String, Object>> list) throws IOException {
        File configDir = getConfigTypeDir(type);
        File configFile = new File(configDir, System.currentTimeMillis() + ".json") ;
        JsonUtils.writeJsonFile(configFile, list);
    }
}
