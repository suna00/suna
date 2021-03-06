package net.ion.ice.core.infinispan;

import net.ion.ice.core.node.Node;
import org.infinispan.Cache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.*;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by jaeho on 2017. 3. 31..
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class InfinispanRepositoryServiceTest {

    @Autowired
    private InfinispanRepositoryService repositoryService ;

    @Test
    public void cacheInit() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        Cache<String, Node> nodes = repositoryService.getNodeCache("test") ;


        Node node = new Node("id1", "test") ;
        node.put("key1", "value1") ;
        node.put("key2", 1) ;

//        nodes.getAdvancedCache().getTransactionManager().begin();
        nodes.put(node.getId().toString(), node) ;


        Node node2 = new Node("id2", "test") ;
        node2.put("key1", "value2") ;
        node2.put("key2", 2) ;

        nodes.put(node2.getId().toString(), node2) ;


//        assertEquals(nodes.size(), 2) ;

        for(int i=3; i<=10; i++){
            Node anode = new Node("auto" + i, "test") ;
            anode.put("key1", "auto " + i) ;
            anode.put("key2", i) ;
            nodes.put(anode.getId().toString(), anode) ;
        }


        assertEquals(nodes.size(), 10) ;

//        for(int i=3; i<=10; i++){
//            nodes.remove("auto" + i) ;
//        }
//
//        assertEquals(nodes.size(), 2) ;
//        nodes.getAdvancedCache().getTransactionManager().commit();

//        StandardTokenizerFactory tokenizerFactory =
    }

}