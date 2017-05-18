package net.ion.ice.core.cluster;

import com.hazelcast.core.Member;
import net.ion.ice.core.infinispan.InfinispanCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.Set;

/**
 * Created by jaeho on 2017. 3. 29..
 */
@Controller
public class ClusterController {
    private static Logger logger = LoggerFactory.getLogger(ClusterController.class);

    @Autowired
    private ClusterConfiguration config ;

    @RequestMapping(value = "/cluster")
    @ResponseBody
    public Set<Member> cluster() {
        config.init();

        Set<Member> members = config.getClusterMembers() ;

        for(Member member : members){
            System.out.println( member.getAddress().getHost()) ;
        }


        return members ;
    }


    @RequestMapping(value = "/client")
    @ResponseBody
    public Object client() {
        config.init();

        return config.getHazelcast().getClientService() ;
    }

    @RequestMapping(value = "/cardinal")
    @ResponseBody
    public Object cardinal() {
        config.init();

        return config.getHazelcast().getName() ;
    }

    @RequestMapping(value = "/endpoint")
    @ResponseBody
    public Object endpoint() {
        config.init();

        return config.getHazelcast().getLocalEndpoint() ;
    }
}
