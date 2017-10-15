package net.ion.ice.core.cluster;

import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Created by jaeho on 2017. 6. 13..
 */
@Service
public class ClusterService {

    @Autowired
    private ClusterConfiguration clusterConfiguration ;


    public IAtomicLong getSequence(String sequenceName){
       IAtomicLong sequence = clusterConfiguration.getIAtomicLong(sequenceName) ;
       return sequence ;
    }

    public Map<String, Object> getSession(String userToken) {
        Map<String, Map<String, Object>> sessionMap =  clusterConfiguration.getSesssionMap() ;
        return sessionMap.get(userToken) ;
    }


    public void cache(ExecuteContext executeContext) {
        Node node = executeContext.getNode() ;
        if(node != null) {
            NodeType nodeType = NodeUtils.getNodeType(node.getTypeId()) ;
            String clusterGroup = nodeType.getClusterGroup() ;
            ITopic topic = clusterConfiguration.getTopic(clusterGroup);
            if(topic == null){
                topic = clusterConfiguration.getTopic("all") ;
            }
            topic.publish(new CacheMessage(executeContext.getEvent(), node));
        }
    }

    public boolean checkClusterGroup(NodeType nodeType){
        if(clusterConfiguration.isAll()){
            return true ;
        }
        String clusterGroup = nodeType.getClusterGroup() ;
        if(StringUtils.isEmpty(clusterGroup)){
            clusterGroup = "cms" ;
        }
        if(clusterGroup.equals("all")) return true ;
        return clusterConfiguration.getGroupList().contains(clusterGroup) ;
    }

    public Member getClusterServer(String mode, String clusterGroup) {
        Set<Member> members = clusterConfiguration.getClusterMembers() ;

        for(Member member : members){
            if(mode.equals(member.getStringAttribute("mode")) && StringUtils.contains(member.getStringAttribute("groups"), clusterGroup)){
                return member ;
            }
        }
        return null ;
    }


}
