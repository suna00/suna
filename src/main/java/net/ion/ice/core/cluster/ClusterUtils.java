package net.ion.ice.core.cluster;

import com.hazelcast.core.Member;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.api.ApiUtils;
import net.ion.ice.core.context.ApiExecuteContext;
import net.ion.ice.core.context.ApiQueryContext;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.NodeType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ClusterUtils {

    private static Logger logger = LoggerFactory.getLogger(ClusterUtils.class) ;

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

    public static Map<String, Object> callExecute(ApiExecuteContext executeContext) {
        Member server = getClusterService().getClusterServer("cms", executeContext.getNodeType().getClusterGroup()) ;
        String url =  "http://" + server.getAddress().getHost() + ":" + server.getStringAttribute("port") + "/node/" + executeContext.getNodeType().getTypeId() + "/" + executeContext.getEvent() + ".json" ;

        try {
            executeContext.getData().put(CONFIG_, JsonUtils.toJsonString(executeContext.getConfig())) ;
            if(StringUtils.isNotEmpty(executeContext.getDateFormat())) {
                executeContext.getData().put(DATE_FORMAT_, executeContext.getDateFormat());
            }
            if(executeContext.getFileUrlFormat() != null) {
                executeContext.getData().put(FILE_URL_FORMAT_, JsonUtils.toJsonString(executeContext.getFileUrlFormat()));
            }
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

    public static List<Map<String, Object>> callNodeList(Member member, String typeId, String queryString) {
        String url = "http://" + member.getAddress().getHost() + ":" + member.getStringAttribute("port")  + "/helper/list" ;
        Map<String, Object> param = new HashMap<>();
        param.put("typeId", typeId) ;
        param.put("query", queryString) ;
        String resultStr = null ;
        try {
            resultStr = ApiUtils.callApiMethod(url, param, 5000, 20000, ApiUtils.POST) ;
            List<Map<String, Object>> result = JsonUtils.parsingJsonToList(resultStr) ;
            return result ;
        } catch (Exception e) {
            logger.error("CALL NODE LIST ERROR : {} - {}", url, resultStr);
            e.printStackTrace();
        }
        return null ;
    }

    public static Map<String, Object> callNode(NodeType nodeType, String id) {
        Member server = getClusterService().getClusterServer("cache", nodeType.getClusterGroup()) ;
        if(server == null){
            server = getClusterService().getClusterServer("cms", nodeType.getClusterGroup()) ;
        }
        String url = "http://" + server.getAddress().getHost() + ":" + server.getStringAttribute("port")  + "/helper/read" ;
        return getSyncNodeResult(url, nodeType.getTypeId(), id);
    }

    private static Map<String, Object> getSyncNodeResult(String url, String typeId, String id) {
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("typeId", typeId) ;
            param.put("id", id) ;

            String resultStr = ApiUtils.callApiMethod(url, param, 5000, 20000, ApiUtils.POST) ;
            Map<String, Object> result = JsonUtils.parsingJsonToMap(resultStr) ;
            if(result.containsKey("result") && result.containsKey("resultMessage")){
                logger.error("CALL NODE ERROR : {} - {}", url, result);
                return null;
            }
            return result ;
        } catch (Exception e) {
            logger.error("CONNECT ERROR : " + url );
            e.printStackTrace();
        }
        return null ;
    }


    public static Map<String, Object> callNode(Member member, String typeId, String id) {
        String url = "http://" + member.getAddress().getHost() + ":" + member.getStringAttribute("port")  + "/helper/read" ;
        return getSyncNodeResult(url, typeId, id);
    }

}
