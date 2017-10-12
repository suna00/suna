package net.ion.ice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IceController {

    @RequestMapping("/checkThread")
    public ModelAndView commonResult() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("/CheckThreadGC");
        return mv;
    }
}
