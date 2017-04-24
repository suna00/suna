package net.ion.ice;

import net.ion.ice.json.JsonRepositoryService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class Ice2ApplicationTests {

	@Test
	public void contextLoads() {
        JsonRepositoryService jsonRepositoryService = (JsonRepositoryService) ApplicationContextManager.getContext().getBean("jsonRepositoryService");
        jsonRepositoryService.getClass() ;
    }

    @Test
    public void configuration(){
        assertEquals(CoreConfig.getConfigValue("project"), "ice2-cm-test");
        assertEquals(CoreConfig.getConfigValue("env"), "development");
    }

}
