package net.ion.ice.core.node;


import net.ion.ice.core.response.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class NodeHelperController {
    private Logger logger = LoggerFactory.getLogger(NodeHelperController.class);

    @Autowired
    private NodeHelperService nodeHelperService ;

    @RequestMapping(value = "/helper/reloadSchema.json", method = RequestMethod.GET)
    @ResponseBody
    public Object saveJson(HttpServletRequest request, @RequestParam String filePath)  {
        try {
            logger.info("reload schema");
            nodeHelperService.reloadSchema(filePath);
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;

        }
        return JsonResponse.create() ;
    }

    @RequestMapping(value = "/helper/read", method = RequestMethod.GET)
    @ResponseBody
    public Object readNode(HttpServletRequest request, @RequestParam String typeId, @RequestParam String id)  {
        try {
            logger.info("node read : {}, {}", typeId, id);
            return NodeUtils.readNode(typeId, id).toMap();
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;
        }
    }
}
