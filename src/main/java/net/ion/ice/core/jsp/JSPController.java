package net.ion.ice.core.jsp;

import net.ion.ice.core.data.bind.NodeBindingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class JSPController {
    private static Logger logger = LoggerFactory.getLogger(NodeBindingInfo.class);

    @RequestMapping("/order")
    public ModelAndView order() {
        ModelAndView mv = new ModelAndView();

        mv.setViewName("/order/index");
        return mv;
    }
}
