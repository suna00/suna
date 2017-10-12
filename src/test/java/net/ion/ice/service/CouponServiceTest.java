package net.ion.ice.service;

import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeQuery;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.session.SessionService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest

public class CouponServiceTest {

    @Autowired
    private NodeService nodeService;
    @Autowired
    NodeBindingService nodeBindingService;
    @Autowired
    SessionService sessionService;

    CommonService common;
    @Test

    public void ApplicableCouponList() {

        String typeId = "tempOrder";                                          //cart or tempOrder
        String targetProductTypeId = "tempOrderProduct";           //cartProduct or tempOrderProduct
        String targetProductItemTypeId = "tempOrderProductItem";   //cartProductItem or tempOrderProductItem
        String id = "tempOrderId";   //cartProductItem or tempOrderProductItem
        String idValue = "B20171011163334139315";
        List<Map<String, Object>> targetProductList = nodeBindingService.list(targetProductTypeId, "sorting=created&".concat(id).concat("_equals=)").concat(idValue));
        List<Map<String, Object>> targetProductItemList = nodeBindingService.list(targetProductItemTypeId, "sorting=created&".concat(id).concat("_equals=)").concat(idValue));

        System.out.println("targetProductList" + targetProductList);
        System.out.println("targetProductList");

        List<Node> couponList = (List<Node>) NodeQuery.build("coupon").matching("memberNo", "77777").matching("couponStatus", "n").getList();

    }

}