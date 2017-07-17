package net.ion.ice.core.context;

import org.junit.Test;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by jaehocho on 2017. 7. 16..
 */
public class TemplateTest {
    @Test
    public void parsing() throws Exception {
        Map<String, Object> testData = new HashMap<>() ;
        testData.put("val1", "Val") ;
        parsing("testItem", "testItem", testData);
        parsing("testItem{{:val1}}", "testItemVal", testData);
    }

    public void parsing(String templateStr, String expectedStr, Map<String, Object> data) throws ParseException {
        Template template = new Template(templateStr) ;
        template.parsing();

        assertEquals(template.format(data), expectedStr) ;

    }
}