package net.ion.ice.core.cluster;

import com.hazelcast.core.Member;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.api.ApiUtils;
import net.ion.ice.core.context.ApiQueryContext;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.json.JsonUtils;

import java.io.IOException;
import java.util.Map;

public class ClusterUtils {
    static ClusterService clusterService ;

    public static ClusterService getClusterService() {
        if (clusterService == null) {
            clusterService = ApplicationContextManager.getBean(ClusterService.class);
        }
        return clusterService;
    }

    public static Map<String, Object> callExecute(ExecuteContext executeContext) {
        Member server = getClusterService().getClusterServer("cms", executeContext.getNodeType().getClusterGroup()) ;
        String url =  "http://" + server.getAddress().getHost() + ":" + server.getAddress().getPort() + "/node/" + executeContext.getNodeType().getTypeId() + "/" + executeContext.getEvent() + ".json" ;

        try {
            String resultStr = ApiUtils.callApiMethod(url, executeContext.getData(), 5000, 10000, ApiUtils.POST) ;
            return JsonUtils.parsingJsonToMap(resultStr) ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Map<String, Object> callQuery(ApiQueryContext queryContext) {
        Member server = getClusterService().getClusterServer("cache", queryContext.getNodeType().getClusterGroup()) ;
        if(server == null){
            server = getClusterService().getClusterServer("cms", queryContext.getNodeType().getClusterGroup()) ;
        }
        String url =  "http://" + server.getAddress().getHost() + ":" + server.getAddress().getPort() + "/node/query" ;

        try {
            queryContext.getData().put("_config_", queryContext.getConfig()) ;
            String resultStr = ApiUtils.callApiMethod(url, queryContext.getData(), 5000, 10000, ApiUtils.POST) ;
            return JsonUtils.parsingJsonToMap(resultStr) ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}
