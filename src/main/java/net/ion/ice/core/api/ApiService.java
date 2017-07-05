package net.ion.ice.core.api;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.response.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * Created by jaeho on 2017. 7. 3..
 */

@Service
public class ApiService {

    @Autowired
    private NodeService nodeService ;

    public Object execute(WebRequest request, String category, String api, String method) {
        Node apiNode = nodeService.getNode("apiConfig", category + Node.ID_SEPERATOR + api) ;

        String apiMethod = (String) apiNode.get("method");

        if(!method.equals(apiMethod)){
            throw new RuntimeException("Not Allow Method") ;
        }

        if(apiMethod.equals("POST")){
            if(request instanceof MultipartHttpServletRequest) {
                ApiContext context = ApiContext.createContext(apiNode, request.getParameterMap(), ((MultipartHttpServletRequest) request).getMultiFileMap()) ;
                return JsonResponse.create(nodeService.saveNode(request.getParameterMap(), ((MultipartHttpServletRequest) request).getMultiFileMap(), "")) ;
            }
            return JsonResponse.create(nodeService.saveNode(request.getParameterMap(), "")) ;
        }
        return null ;
    }
}
