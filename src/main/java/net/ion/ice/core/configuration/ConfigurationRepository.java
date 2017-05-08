package net.ion.ice.core.configuration;

import net.ion.ice.core.CoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
@Component
public class ConfigurationRepository {
    @Autowired
    private CoreConfig config ;

    public enum Mode {LOCAL, REMOTE, CLUSTER} ;

    private Mode mode ;

    private ConfigurationHandler handler ;


    private void initRepository() {
        if(this.handler != null) return ;

        if(config.getConfigValue("configuration.mode") == null){
            mode = Mode.LOCAL ;
        }else {
            mode = Mode.valueOf(config.getConfigValue("configuration.mode").toString().toUpperCase()) ;
        }

        this.handler = null ;
        switch (mode){
            case LOCAL :
                this.handler = new LocalConfigurationHandler();
                break;
            case REMOTE:
                Collection<String> configServers = (Collection<String>) config.getConfigValue("configuration.servers");
                Collection<String> ipAddress = ConfigurationUtils.getIpAddress() ;
                for(String configServer : configServers){
                    for(String ip : ipAddress){
                        if(configServer.equals(ip)){
                            this.handler = new LocalConfigurationHandler();
                            break ;
                        }
                    }
                    if(this.handler != null) break ;
                    if(configServer.equals(CoreConfig.getHostName())){
                        this.handler = new LocalConfigurationHandler() ;
                        break;
                    }
                }
                if(this.handler == null) {
                    this.handler = new RemoteConfigurationHandler();
                }
                break;
            case CLUSTER:
                this.handler = new ClusterConfigurationHandler();
                break;

            default:
                this.handler = new LocalConfigurationHandler() ;
        }
    }


    public Collection<Map<String,Object>> getConfigList(String type) {
        initRepository();
        try {
            return handler.getConfigList(type) ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writeConfig(String type, Map<String, Object> configValue){
        initRepository();

        try {
            if(!handler.checkLock(type)){
                return ;
            }
        } catch (IOException e) {
        }
        try {
            Collection<Map<String,Object>> list = handler.getConfigList(type) ;
            boolean isUpdate = false ;
            Collection<Map<String, Object>> target = new ArrayList<Map<String, Object>>();
            for(Map<String, Object> item : list){
                if(item.get("id").equals(configValue.get("id"))){
                    target.add(configValue) ;
                    isUpdate = true ;
                }else{
                    target.add(item) ;
                }
            }
            if(!isUpdate){
                target.add(configValue) ;
            }

            handler.writeConfig(type, target) ;
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            handler.releaseLock(type) ;
        }
    }


    public void removeConfig(String type, Map<String, Object> configValue) {
        initRepository();

        try {
            if(!handler.checkLock(type)){
                return ;
            }
        } catch (IOException e) {
        }
        try {
            Collection<Map<String,Object>> list = handler.getConfigList(type) ;
            Collection<Map<String, Object>> target = new ArrayList<Map<String, Object>>();
            for(Map<String, Object> item : list){
                if(item.get("id").equals(configValue.get("id"))){
                    continue ;
                }else{
                    target.add(item) ;
                }
            }
            handler.writeConfig(type, target) ;
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            handler.releaseLock(type) ;
        }

    }


    public String getMode(){
        return mode.toString() ;
    }
}
