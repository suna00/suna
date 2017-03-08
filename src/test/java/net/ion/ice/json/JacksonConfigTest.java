package net.ion.ice.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 3. 6..
 */
public class JacksonConfigTest {

    Logger logger = LoggerFactory.getLogger(JacksonConfigTest.class) ;

    @Test
    public void convert() throws JsonProcessingException {
        Map<String, Object> data = new HashMap<String, Object>() ;
        data.put("name", "NAME") ;
        data.put("date", new Date()) ;

        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).modules(new JavaTimeModule()).build() ;

        logger.info(objectMapper.writeValueAsString(data)) ;
    }

}