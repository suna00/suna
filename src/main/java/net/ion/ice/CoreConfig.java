package net.ion.ice;

import net.ion.ice.configuration.ConfigurationUtils;
import net.ion.ice.json.JsonUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
@Component
public class CoreConfig {
    private static ClassPathResource configFilePath = new ClassPathResource("/config.json");
    private static File configFile ;
    private static Map<String, Object> configData;
    private static String hostName ;

    public CoreConfig(){
        try {
            initConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static synchronized void initConfig() throws IOException {
        hostName = ConfigurationUtils.getHostName() ;
        configFile = configFilePath.getFile() ;
        initConfigData();
    }

    static void setHostName(String newHostName){
        hostName = newHostName ;
    }


    static void initConfigData() throws IOException {
        Map<String, Object> configSrc = JsonUtils.parsingJsonFileToMap(configFile) ;
        configData = new LinkedHashMap<String, Object>() ;

        configData.putAll(configSrc);

        boolean matchingProfile = false ;
        List<Map<String, Object>> profiles = (List<Map<String, Object>>) configSrc.get("profiles");
        for(Map<String, Object> profileData : profiles){
            if(JsonUtils.contains(profileData, "hostName", hostName)){
                configData.putAll(profileData);
                matchingProfile = true ;
            }
        }

        if(!matchingProfile){
            for(Map<String, Object> profileData : profiles){
                if(JsonUtils.contains(profileData, "env", "development")){
                    configData.putAll(profileData);
                    break;
                }
            }
        }
    }

    public static Object getConfigValue(String key) {
        return JsonUtils.getValue(configData, key);
    }


    public static String getHostName(){
        return hostName ;
    }
}
