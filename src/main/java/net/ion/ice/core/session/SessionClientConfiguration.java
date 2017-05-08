package net.ion.ice.core.session;

/**
 * Created by jaehocho on 2017. 3. 10..
 */

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.web.WebFilter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A conditional configuration that potentially adds the bean definitions in
 * this class to the Spring application context, depending on whether the
 * {@code @ConditionalOnExpression} is true or not.
 *
 * When true, beans are added that create a Hazelcast instance, and bind this
 * instance to Tomcat for storage of HTTP sessions, instead of Tomcat's default
 * implementation.
 */
@Configuration
@ConfigurationProperties(prefix = "session")
@ConditionalOnExpression("'${session.mode}' == 'client'")
public class SessionClientConfiguration {
    private List<String> members = new ArrayList<>();

    private String baseDir ;


    /**
     * Create a Hazelcast {@code Config} object as a bean. Spring Boot will use
     * the presence of this to determine that a {@code HazelcastInstance} should
     * be created with this configuration.
     * <p>
     * As a simple side-step to possible networking issues, turn off multicast
     * in favour of TCP connection to the local host.
     *
     * @return Configuration for the Hazelcast instance
     */
    @Bean
    public ClientConfig config() {

        ClientConfig config = new ClientConfig();
        config.setInstanceName("session-hazelcast") ;
//        ClientNetworkConfig clientConfig = config.getNetworkConfig();
//        clientConfig.addAddress("localhost:5701");
//        config.setNetworkConfig(clientConfig) ;
        return config;
    }

    @Bean
    public HazelcastInstance hazelcastInstance() {
        return HazelcastClient.newHazelcastClient(config());
    }

    /**
     * Create a web filter. Parameterize this with two properties,
     * <p>
     * <ol>
     * <li><i>instance-name</i>
     * Direct the web filter to use the existing Hazelcast instance rather than
     * to create a new one.</li>
     * <li><i>sticky-session</i>
     * As the HTTP session will be accessed from multiple processes, deactivate
     * the optimization that assumes each user's traffic is routed to the same
     * process for that user.</li>
     * </ol>
     * <p>
     * Spring will assume dispatcher types of {@code FORWARD}, {@code INCLUDE}
     * and {@code REQUEST}, and a context pattern of "{@code /*}".
     *
     * @param hazelcastInstance Created by Spring
     * @return The web filter for Tomcat
     */
    @Bean
    public WebFilter webFilter(HazelcastInstance hazelcastInstance) {

        Properties properties = new Properties();
        properties.put("instance-name", hazelcastInstance.getName());
//        properties.put("sticky-session", "false");
        properties.put("use-client", "true");

        return new WebFilter(properties);
    }

    public List<String> getMembers(){
        return members ;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }


}

