package net.ion.ice.core.jsp;

import net.ion.ice.core.data.bind.NodeBindingInfo;
import net.ion.ice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

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
}
