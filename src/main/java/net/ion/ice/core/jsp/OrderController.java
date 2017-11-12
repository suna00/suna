package net.ion.ice.core.jsp;

import net.ion.ice.core.context.RequestDataHolder;
import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
public class OrderController {
    private static Logger logger = LoggerFactory.getLogger(NodeBindingInfo.class);

    @Autowired
    OrderService orderService;

//    @RequestMapping("/order")
//    public ModelAndView order() {
//        ModelAndView mv = new ModelAndView();
//        mv.setViewName("/order/index");
//        return mv;
//    }
//
    @RequestMapping("/order/orderTest")
    public ModelAndView orderTest() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/orderTest");
        return mv;
    }

    @RequestMapping("/order/hub")
    public ModelAndView hub() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/hub");
        return mv;
    }
    @RequestMapping("/order/mobile_hub")
    public ModelAndView mobileHub() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/mobile_hub");
        return mv;
    }

    @RequestMapping("/order/cash_hub")
    public ModelAndView cashHub() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/cash_hub");
        return mv;
    }

//    @RequestMapping("/order/result")
//    public ModelAndView result() {
//        ModelAndView mv = new ModelAndView();
//        mv.setViewName("/order/result");
//        return mv;
//    }
    @RequestMapping("/order/common_return")
    public ModelAndView commonResult() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/common_return");
        return mv;
    }

    @RequestMapping("/order/order_approval")
    public ModelAndView approval() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/order_approval");
        return mv;
    }

    @RequestMapping("/order/order_mobile")
    public ModelAndView mobileOrder() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/order_mobile");
        return mv;
    }

    @RequestMapping("/order/cancel")
    public ModelAndView cancel() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/cancel/hub");
        return mv;
    }

    @RequestMapping("/order/refundTest")
    public ModelAndView refund() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/refund/order_refund_test");
        return mv;
    }

    @RequestMapping("/order/refundTest1")
    public ModelAndView refund1() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/refund/refund");
        return mv;
    }

    @RequestMapping("/order/refundTest2")
    public ModelAndView refund2() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/order/refund/result");
        return mv;
    }


}
