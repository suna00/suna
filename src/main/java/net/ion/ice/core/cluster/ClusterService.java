package net.ion.ice.core.cluster;

import com.hazelcast.core.IAtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
