package net.ion.ice.core.api;

import net.ion.ice.core.node.Node;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Created by jaehocho on 2017. 7. 4..
 */
public class ApiContext {

    private Node apiNode;

    public static ApiContext createContext(Node apiNode, Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap) {
        ApiContext ctx = new ApiContext() ;
        ctx.apiNode = apiNode ;
        return ctx ;
    }
}
