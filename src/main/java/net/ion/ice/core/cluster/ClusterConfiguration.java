package net.ion.ice.core.cluster;

/**
 * Created by jaehocho on 2017. 3. 10..
 */

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.core.*;
import com.hazelcast.topic.TopicOverloadPolicy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * A conditional configuration that potentially adds the bean definitions in
 * this class to the Spring application context, depending on whether the
 * {@code @ConditionalOnExpression} is true or not.
 *
 */
@Configuration
@ConfigurationProperties(prefix = "cluster")
public class ClusterConfiguration {
    private Logger logger = LoggerFactory.getLogger(ClusterConfiguration.class);

    private List<String> members = new ArrayList<>();
    private String mode ;

    private String groups ;
    private HazelcastInstance hazelcast  ;

    private List<String> groupList = new ArrayList<>() ;

    private Map<String, ITopic> topicMap = new HashMap<>() ;
    private Map<String, IQueue> queueMap = new HashMap<>() ;

    @PostConstruct
    public void init(){
        if(hazelcast == null) {
            hazelcast = Hazelcast.newHazelcastInstance(config());
            for(String grp : groupList){
                ITopic topic = hazelcast.getReliableTopic(grp + "_topic") ;
                topic.addMessageListener(new TopicListener()) ;
                topicMap.put(grp, topic) ;

                IQueue queue = hazelcast.getQueue(grp + "_queue") ;

            }
        }
    }

    public Config config() {
        Config config = new Config();
        config.setInstanceName("ice-cluster-hazelcast") ;

        if(StringUtils.isEmpty(this.groups) ){
            logger.warn("Not Define Groups");
            groupList.add("all") ;
        }else{
            for(String grp : StringUtils.split(groups, ",")){
                groupList.add(grp.trim()) ;
            }
            if(!groupList.contains("all")) {
                groupList.add("all");
            }
        }
        logger.info("Define Cluster Group List : " + groupList );

        JoinConfig joinConfig = config.getNetworkConfig().getJoin();

        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getTcpIpConfig().setEnabled(true).setMembers(members);

        for(String grp : groupList){
            ReliableTopicConfig rtConfig = config.getReliableTopicConfig(grp + "_topic") ;
            rtConfig.setName(grp + "_topic") ;
            rtConfig.setTopicOverloadPolicy(TopicOverloadPolicy.BLOCK).setReadBatchSize(10) ;

            QueueConfig queueConfig = config.getQueueConfig(grp + "_queue");
            queueConfig.setName(grp + "_queue")
                    .setBackupCount(1)
                    .setMaxSize(0)
                    .setStatisticsEnabled(true);
        }

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

    public Map<String, Map<String, Object>> getSesssionMap() {
        return hazelcast.getReplicatedMap("ice_session");
    }

    public ITopic getTopic(String group) {
        return this.topicMap.get(group);
    }

    public List<String> getGroupList(){
        return this.groupList ;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isAll() {
        return mode == null || mode.equals("all");
    }
}

