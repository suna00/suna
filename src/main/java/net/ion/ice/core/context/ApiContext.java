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


    public static ApiContext createContext(Node apiNode, Map<String, Object> config,  Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap) {
        ApiContext ctx = new ApiContext() ;
        ctx.apiNode = apiNode ;
        ctx.data = ContextUtils.makeContextData(parameterMap, multiFileMap) ;

        ctx.config = config;

        ctx.init();
        return ctx ;
    }

    private void init() {
        for(String key : config.keySet()){
            Map<String, Object> ctxRootConfig = (Map<String, Object>) config.get(key);

            if(ctxRootConfig.containsKey("event")){
                ExecuteContext executeContext = ExecuteContext.makeContextFromConfig(ctxRootConfig, data) ;
                contexts.add(executeContext) ;
            }else if(ctxRootConfig.containsKey("query")){
                QueryContext queryContext = QueryContext.makeContextFromConfig(ctxRootConfig, data) ;
            }
        }
    }

}
