package net.ion.ice.core.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 2. 10..
 */
@Service("configurationRepositoryService")
public class ConfigurationRepositoryService {

    @Autowired
    private ConfigurationRepository configurationRepository ;


    public Collection<Map<String,Object>> list(String type) {
        return configurationRepository.getConfigList(type) ;
    }
}
