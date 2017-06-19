package net.ion.ice.core.cluster;

/**
 * Created by jaehocho on 2017. 3. 10..
 */

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.Member;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A conditional configuration that potentially adds the bean definitions in
 * this class to the Spring application context, depending on whether the
 * {@code @ConditionalOnExpression} is true or not.
 *
 */
@Configuration
@ConfigurationProperties(prefix = "cluster")
public class ClusterConfiguration {
    private List<String> members = new ArrayList<>();
    private HazelcastInstance hazelcast  ;

    @PostConstruct
    public void init(){
        if(hazelcast == null) {
            hazelcast = Hazelcast.newHazelcastInstance(config());
        }
    }

    public Config config() {

        Config config = new Config();
        config.setInstanceName("cluster-hazelcast") ;

        JoinConfig joinConfig = config.getNetworkConfig().getJoin();

        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true).setMembers(members);

        return config;
    }

    public Set<Member> getClusterMembers(){
        return hazelcast.getCluster().getMembers() ;
    }


    public List<String> getMembers(){
        return members ;
    }

    public HazelcastInstance getHazelcast(){
        return hazelcast ;
    }

    public IAtomicLong getIAtomicLong(String name) {
        return hazelcast.getAtomicLong(name) ;
    }
}

