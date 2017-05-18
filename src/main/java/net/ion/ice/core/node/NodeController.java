package net.ion.ice.core.node;

import com.sun.tools.javac.util.List;
import net.ion.ice.core.cluster.ClusterController;
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


    @RequestMapping(value = "/node/{nodeType}", method = RequestMethod.GET)
    @ResponseBody
    public Object list(WebRequest request, @PathVariable String nodeType, HttpServletResponse response) throws IOException {
        try {
            List<Node> nodeList = nodeService.getNodeList(nodeType, request.getParameterMap()) ;
            return new ListResponse(nodeList) ;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if(e.getCause() instanceof ClassCastException){
                ResponseUtils.writeErrorJson(response, new Exception("형식이 맞지 않습니다."));
            }else{
                ResponseUtils.writeErrorJson(response, e);
            }
        }
    }
}
