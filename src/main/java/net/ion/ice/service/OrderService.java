package net.ion.ice.service;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service("orderService")
public class OrderService {

    public void directOrder(ExecuteContext context){
        Map<String, Object> data = context.getData();


    }

    public void orderFromCart(ExecuteContext context){
        Map<String, Object> data = context.getData();

    }

    //  주문서 작성
    public void addTempOrder(){

    }

    // batch : 주문 성공 or 일정기간 주문 성사되지 않은 주문서 제거
    public void cleanTempOrder(){

    }

    // 주문성공
    public void addOrder(){

    }
}
