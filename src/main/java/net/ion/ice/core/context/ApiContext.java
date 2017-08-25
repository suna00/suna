package net.ion.ice.core.context;

import net.ion.ice.core.cluster.ClusterService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.ResultField;
import net.ion.ice.core.session.SessionService;
import net.ion.ice.security.common.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 7. 4..
 */
public class ApiContext {

    private Node apiNode;
    private Map<String, Object> data;

    private Map<String, Object> config;

    private List<ResultField> resultFieldList;


    public static ApiContext createContext(Node apiNode, Map<String, Object> config, Map<String, String[]> parameterMap, MultiValueMap<String, MultipartFile> multiFileMap, Map<String, Object> session) {
        ApiContext ctx = new ApiContext();
        ctx.apiNode = apiNode;
        ctx.data = ContextUtils.makeContextData(parameterMap, multiFileMap);

        ctx.data.put("session", session);

        ctx.config = config;

        ctx.init();
        return ctx;
    }

    private void init() {
        resultFieldList = new ArrayList<>();
        if (config.containsKey("typeId") || config.containsKey("apiType")) {
            resultFieldList.add(makeSubContext("root", config));
        } else {
            for (String key : config.keySet()) {
                Map<String, Object> ctxRootConfig = (Map<String, Object>) config.get(key);
                resultFieldList.add(makeSubContext(key, ctxRootConfig));
            }
        }
    }

    private ResultField makeSubContext(String key, Map<String, Object> ctxRootConfig) {
        if (ctxRootConfig.containsKey("event")) {
            ExecuteContext executeContext = ExecuteContext.makeContextFromConfig(ctxRootConfig, data);
            return new ResultField(key, executeContext);
        } else if (ctxRootConfig.containsKey("query")) {
            ApiQueryContext queryContext = ApiQueryContext.makeContextFromConfig(ctxRootConfig, data);
            return new ResultField(key, queryContext);
        } else if (ctxRootConfig.containsKey("select")) {
            ApiSelectContext selectContext = ApiSelectContext.makeContextFromConfig(ctxRootConfig, data);
            return new ResultField(key, selectContext);
        }
        return null;
    }


    public Object makeApiResult() {
        QueryResult queryResult = new QueryResult();

        for (ResultField field : resultFieldList) {
            Context context = field.getContext();
            if (context instanceof ReadContext) {
                if ("root".equals(field.getFieldName())) {
                    queryResult = (QueryResult) ((ReadContext) context).makeQueryResult(null, null);
                } else {
                    queryResult.put(field.getFieldName(), ((ReadContext) context).makeQueryResult(null, null));
                }
            }
        }

        return queryResult;
    }
}
