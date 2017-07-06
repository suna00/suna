package net.ion.ice.core.context;

import net.ion.ice.core.node.Node;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 7. 4..
 */
public class ApiContext {

    private Node apiNode;
    private Map<String, Object> data  ;

    private Map<String, Object> config  ;

    private List<Context> contexts ;


    public static ApiContext createContext(Node apiNode, Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap) {
        ApiContext ctx = new ApiContext() ;
        ctx.apiNode = apiNode ;
        ctx.data = ContextUtils.makeContextData(parameterMap, multiFileMap) ;

        ctx.config = (Map<String, Object>) apiNode.get("config");

        for(String key : ctx.config.keySet()){
            Map<String, Object> ctxRootConfig = (Map<String, Object>) ctx.config.get(key);

//            if(hasEvent(ctxRootConfig)){
//
//            }

        }


        return ctx ;
    }

}
