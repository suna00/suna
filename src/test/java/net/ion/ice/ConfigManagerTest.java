package net.ion.ice;

import net.ion.ice.core.CoreConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by jaehocho on 2017. 2. 11..
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConfigManagerTest {

    @Test
    public void configuration() throws IOException {
        assertEquals(CoreConfig.getConfigValue("project"), "ice2-cm-test");
        assertEquals(CoreConfig.getConfigValue("env"), "development");

//        CoreConfig.setHostName("stg1");
//        CoreConfig.initConfigData();

        assertEquals(CoreConfig.getConfigValue("env"), "staging");
        assertEquals(CoreConfig.getConfigValue("json-path"), "/res/stg");
    }
}
