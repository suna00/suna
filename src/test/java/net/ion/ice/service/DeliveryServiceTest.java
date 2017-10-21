package net.ion.ice.service;

import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.node.NodeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Component
public class DeliveryServiceTest {

    @Autowired
    private NodeBindingService nodeBindingService;
    @Autowired
    private NodeService nodeService;



    @Test
    public void calculateDeliveryPrice() throws Exception {

        JdbcTemplate jdbcTemplate = nodeBindingService.getNodeBindingInfo("YPoint").getJdbcTemplate();


        List<Map<String, Object>> cartProducts = nodeBindingService.list("cartProduct", "sorting=created&cartId_equals=250");

        System.out.println(cartProducts);
    }

}