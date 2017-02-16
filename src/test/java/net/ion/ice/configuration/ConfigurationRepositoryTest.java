package net.ion.ice.configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by jaehocho on 2017. 2. 11..
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfigurationRepositoryTest {
    @Autowired
    private ConfigurationRepository configurationRepository ;


    @Test
    public void initRepository(){
        assertEquals(configurationRepository.getMode(), "LOCAL");
    }

    @Test
    public void writeTypeConfig(){
        configurationRepository.writeConfig("test", makeData( 1, "title1"));

        Collection<Map<String, Object>> savedDatas = configurationRepository.getConfigList("test") ;
        assertEquals(savedDatas.size(), 1);

        configurationRepository.writeConfig("test", makeData(1, "title2"));
        savedDatas = configurationRepository.getConfigList("test") ;

        assertEquals(savedDatas.size(), 1);
        assertEquals(((ArrayList<Map<String,Object>>)savedDatas).get(0).get("name"), "title2");

        configurationRepository.writeConfig("test", makeData(2, "name1"));

        savedDatas = configurationRepository.getConfigList("test") ;
        assertEquals(savedDatas.size(), 2);

        configurationRepository.removeConfig("test", makeData(2, "name1"));

        savedDatas = configurationRepository.getConfigList("test") ;
        assertEquals(savedDatas.size(), 1);

    }

    private Map<String, Object> makeData(int idx, String name) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", idx + "");
        data.put("name", name);
        data.put("order", idx);
        return data;
    }
}