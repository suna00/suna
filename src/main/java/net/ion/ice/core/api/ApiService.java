package net.ion.ice.core.api;

import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.ApiContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by jaeho on 2017. 7. 3..
 */

@Service
public class ApiService {

    private static Logger logger = LoggerFactory.getLogger(ApiService.class);

    @Autowired
    private NodeService nodeService ;

    @Autowired
    private SessionService sessionService;

    public Object execute(NativeWebRequest request, HttpServletResponse response, String category, String api, String method) {
        return execute(request, response, category, api, method, null) ;
    }

    public Object execute(NativeWebRequest request, HttpServletResponse response, String category, String api, String method, String typeId) {
        Node apiCategory  = nodeService.getNode("apiCategory", category) ;
        Node apiNode = nodeService.getNode("apiConfig", category + Node.ID_SEPERATOR + api) ;

        if(apiCategory == null){
            logger.error("Not Found Api Category : " + category );
            throw new IceRuntimeException("Not Found Api Category : " + category) ;
        }

        if(apiNode == null){
            logger.error("Not Found Api Config : " + api);
            throw new IceRuntimeException("Not Found Api Config : " + api) ;
        }


        String apiMethod = (String) apiNode.get("method");
        Map<String, Object> session = null;
        try {
            session = sessionService.getSession(request.getNativeRequest(HttpServletRequest.class));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if(apiMethod.equals("GET") || !method.equals(apiMethod)){
            throw new RuntimeException("Not Allow Method") ;
        }

        ApiContext context = ApiContext.createContext(apiCategory, apiNode, typeId, (Map<String, Object>) apiNode.get("config"), request, response, session) ;
        return context.makeApiResult() ;
    }
}
