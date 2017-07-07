package net.ion.ice.core.data.bind;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by seonwoong on 2017. 7. 4..
 */
@Controller
public class NodeBindingController {
    @Autowired
    NodeBindingService nodeBindingService;


    @RequestMapping(value = "/data/create/{tid}", method = RequestMethod.GET)
    @ResponseBody
    public Object createTable(WebRequest request, HttpServletResponse response, @PathVariable String tid) throws IOException {
        try{
            nodeBindingService.createTable(tid, response);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
