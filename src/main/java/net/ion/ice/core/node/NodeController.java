package net.ion.ice.core.node;


import net.ion.ice.core.response.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by jaehocho on 2017. 5. 17..
 */
@Controller
public class NodeController {
    private static Logger logger = LoggerFactory.getLogger(NodeController.class);

    @Autowired
    private NodeService nodeService ;

    @RequestMapping(value = "/node/{nodeType}", method = RequestMethod.PUT)
    @ResponseBody
    public Object saveRest(WebRequest request, @PathVariable String nodeType) throws IOException {
        return save(request, nodeType);
    }

    private Object save(WebRequest request, String nodeType) {
        return JsonResponse.create(nodeService.saveNode(request.getParameterMap())) ;
    }

    @RequestMapping(value = "/node/{nodeType}", method = RequestMethod.GET)
    @ResponseBody
    public Object listRest(WebRequest request, @PathVariable String nodeType) throws IOException {
        return list(request, nodeType);
    }

    @RequestMapping(value = "/node/{nodeType}/list.json", method = RequestMethod.GET)
    @ResponseBody
    public Object listJson(WebRequest request, @PathVariable String nodeType) throws IOException {
        return list(request, nodeType);
    }


    private Object list(WebRequest request, @PathVariable String nodeType) {
        try {
            QueryResult queryResult = nodeService.getNodeList(nodeType, request.getParameterMap()) ;
            return JsonResponse.create(queryResult) ;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                return JsonResponse.error(e);
            }
        }
    }
}
