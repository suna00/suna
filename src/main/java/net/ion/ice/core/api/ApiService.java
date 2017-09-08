package net.ion.ice.core.api;

import net.ion.ice.core.context.ApiContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.session.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by jaeho on 2017. 7. 3..
 */

@Service
public class ApiService {

    @Autowired
    private NodeService nodeService ;

    @Autowired
    private SessionService sessionService;

    public Object execute(NativeWebRequest request, String category, String api, String method) {
        return execute(request, category, api, method, null) ;
    }

    public Object execute(NativeWebRequest request, String category, String api, String method, String typeId) {
        Node apiCategory  = nodeService.getNode("apiCategory", category) ;
        Node apiNode = nodeService.getNode("apiConfig", category + Node.ID_SEPERATOR + api) ;

        String apiMethod = (String) apiNode.get("method");
        Map<String, Object> session = null;
        try {
            session = sessionService.getSession(request.getNativeRequest(HttpServletRequest.class));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if(!method.equals(apiMethod)){
            throw new RuntimeException("Not Allow Method") ;
        }

        if(apiMethod.equals("POST")){
            if(request instanceof MultipartHttpServletRequest) {
                ApiContext context = ApiContext.createContext(apiCategory, apiNode, typeId, (Map<String, Object>) apiNode.get("config"), request.getParameterMap(), ((MultipartHttpServletRequest) request).getMultiFileMap(), session) ;
                return context.makeApiResult() ;
            }
            ApiContext context = ApiContext.createContext(apiCategory, apiNode, typeId, (Map<String, Object>) apiNode.get("config"), request.getParameterMap(), null, session) ;
            return context.makeApiResult() ;
        }

        ApiContext context = ApiContext.createContext(apiCategory, apiNode, typeId, (Map<String, Object>) apiNode.get("config"), request.getParameterMap(), null, session) ;
        return context.makeApiResult() ;

    }
}
