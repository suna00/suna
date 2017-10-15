package net.ion.ice.core.cluster;

import com.hazelcast.core.Member;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.api.ApiUtils;
import net.ion.ice.core.context.ApiQueryContext;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;

public class ClusterUtils {
    public static final String CONFIG_ = "_config_";
    public static final String DATE_FORMAT_ = "_dateFormat_";
    public static final String FILE_URL_FORMAT_ = "_fileUrlFormat_";
    static ClusterService clusterService ;

    public static ClusterService getClusterService() {
        if (clusterService == null) {
            clusterService = ApplicationContextManager.getBean(ClusterService.class);
        }
        return clusterService;
    }

    public static Map<String, Object> callExecute(ExecuteContext executeContext) {
        Member server = getClusterService().getClusterServer("cms", executeContext.getNodeType().getClusterGroup()) ;
        String url =  "http://" + server.getAddress().getHost() + ":" + server.getStringAttribute("port") + "/node/" + executeContext.getNodeType().getTypeId() + "/" + executeContext.getEvent() + ".json" ;

        try {
            String resultStr = ApiUtils.callApiMethod(url, executeContext.getData(), 5000, 20000, ApiUtils.POST) ;
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
        String url = "http://" + server.getAddress().getHost() + ":" + server.getStringAttribute("port")  + "/node/query" ;

        try {
            queryContext.getData().put(CONFIG_, JsonUtils.toJsonString(queryContext.getConfig())) ;
            if(StringUtils.isNotEmpty(queryContext.getDateFormat())) {
                queryContext.getData().put(DATE_FORMAT_, queryContext.getDateFormat());
            }
            if(queryContext.getFileUrlFormat() != null) {
                queryContext.getData().put(FILE_URL_FORMAT_, JsonUtils.toJsonString(queryContext.getFileUrlFormat()));
            }

            String resultStr = ApiUtils.callApiMethod(url, queryContext.getData(), 5000, 10000, ApiUtils.POST) ;
            return JsonUtils.parsingJsonToMap(resultStr) ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Object> callNodeList(Member member, String typeId, String queryString) {
        String url = "http://" + member.getAddress().getHost() + ":" + member.getStringAttribute("port")  + "/node/" + typeId + "?" + queryString ;
        try {
            String resultStr = ApiUtils.callApiMethod(url, null, 5000, 20000, ApiUtils.GET) ;
            return JsonUtils.parsingJsonToMap(resultStr) ;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
