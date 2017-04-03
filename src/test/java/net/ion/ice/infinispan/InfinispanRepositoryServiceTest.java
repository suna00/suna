package net.ion.ice.infinispan;

import net.ion.ice.node.Node;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.infinispan.Cache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
    public void cacheInit(){
        Cache<String, Node> nodes = repositoryService.getNodeCache("test") ;


        Node node = new Node("id1", "test") ;
        node.put("key1", "value1") ;
        node.put("key2", 1) ;

        nodes.put(node.getId(), node) ;


        Node node2 = new Node("id2", "test") ;
        node2.put("key1", "value2") ;
        node2.put("key2", 2) ;

        nodes.put(node2.getId(), node2) ;


        assertEquals(nodes.size(), 2) ;



//        StandardTokenizerFactory tokenizerFactory =
    }
}