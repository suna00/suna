package net.ion.ice.core.cluster;

/**
 * Created by jaehocho on 2017. 3. 10..
 */

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.web.WebFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A conditional configuration that potentially adds the bean definitions in
 * this class to the Spring application context, depending on whether the
 * {@code @ConditionalOnExpression} is true or not.
 *
 */
@Service
public class ClusterConfiguration {
    private List<String> members = new ArrayList<>();


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
    public Config config() {

        Config config = new Config();
        config.setInstanceName("cluster-hazelcast") ;

        JoinConfig joinConfig = config.getNetworkConfig().getJoin();

        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true).setMembers(members);



        return config;
    }

    @Bean
    public HazelcastInstance hazelcastInstance() {
        return Hazelcast.newHazelcastInstance(config());
    }


    public List<String> getMembers(){
        return members ;
    }




}

