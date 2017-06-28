package net.ion.ice.core.configuration;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.HashMap;


/**
 * Created by juneyoungoh on 2017. 6. 27..
 */
@Configuration
public class DatabaseConfig {

    private Logger logger = Logger.getLogger(DatabaseConfig.class);

    @Autowired
    ApplicationContext context;

    private Map<String, Map<String, String>> dsMap;


    private Map<String, String> getTestDataSourceInfo () {
        Map<String, String> ds = new HashMap<String, String> ();
        ds.put("driverClassName", "com.mysql.jdbc.Driver");
        ds.put("url", "jdbc:mysql://125.131.88.156:3306/webtoon2");
        ds.put("username", "ice");
        ds.put("password", "ice");
        return ds;
    }


    public DatabaseConfig () {
        this.dsMap = new HashMap<String, Map<String, String>>();
        // 파일 읽어서 올림

        // 파일 로드 끝. 샘플 DS 올리기
        dsMap.put("sampleDs1", getTestDataSourceInfo());
        dsMap.put("sampleDs2", getTestDataSourceInfo());
    }

    @PostConstruct
    public void loadDataSource () {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry)  context.getAutowireCapableBeanFactory();

        logger.info("DS ================================ :: " +  String.valueOf(this.dsMap));
        this.dsMap.forEach((k,v) -> {

            // Bean 만들고 context 에 추가
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(BasicDataSource.class);
            v.forEach((ds_key, ds_val) -> {
                builder.addPropertyValue(ds_key, ds_val);
            });

            BeanDefinition def = builder.getBeanDefinition();
            logger.info("========================== BeanName :: " + k + ", BeanDefinition :: " + String.valueOf(def));
            if(!registry.containsBeanDefinition(k)) registry.registerBeanDefinition(k, def);
        });
    }
}
