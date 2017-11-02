package net.ion.ice.core.cluster;

import com.hazelcast.core.Member;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.api.ApiUtils;
import net.ion.ice.core.context.ApiExecuteContext;
import net.ion.ice.core.context.ApiQueryContext;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterUtils {

    private static Logger logger = LoggerFactory.getLogger(ClusterUtils.class) ;

    public static final String CONFIG_ = "_config_";
    public static final String DATE_FORMAT_ = "_dateFormat_";
    public static final String FILE_URL_FORMAT_ = "_fileUrlFormat_";
    static ClusterService clusterService ;

    public static Member defaultServer ;


    public static ClusterService getClusterService() {
        if (clusterService == null) {
            clusterService = ApplicationContextManager.getBean(ClusterService.class);
        }
        return clusterService;
    }

    private static Map<String, Integer> nofoundNode = new ConcurrentHashMap<>(2000) ;


    public static Map<String, Object> callExecute(ApiExecuteContext executeContext, boolean retry) {
        Member server = getClusterService().getClusterServer("cms", executeContext.getNodeType().getClusterGroup(), retry) ;
        if(server == null){
            server = getClusterService().getClusterServer("cache", executeContext.getNodeType().getClusterGroup(), retry) ;
        }
        String url =  "http://" + server.getAddress().getHost() + ":" + server.getStringAttribute("port") + "/node/event" ;
        String resultStr = "";
        try {
            executeContext.getData().put(CONFIG_, JsonUtils.toJsonString(executeContext.getConfig())) ;
            if(StringUtils.isNotEmpty(executeContext.getDateFormat())) {
                executeContext.getData().put(DATE_FORMAT_, executeContext.getDateFormat());
            }
            if(executeContext.getFileUrlFormat() != null) {
                executeContext.getData().put(FILE_URL_FORMAT_, JsonUtils.toJsonString(executeContext.getFileUrlFormat()));
            }
            resultStr = ApiUtils.callApiMethod(url, executeContext.getData(), 5000, 1500, ApiUtils.POST) ;
            return JsonUtils.parsingJsonToMap(resultStr) ;
        } catch (IOException e) {
            if(!retry){
                return callExecute(executeContext, true);
            }
            e.printStackTrace();
        }
        return null;
    }


    public static Map<String, Object> callQuery(ApiQueryContext queryContext, boolean retry) {
        Member server = getClusterService().getClusterServer("cache", queryContext.getNodeType().getClusterGroup(), retry) ;

        if(server == null){
            server = getClusterService().getClusterServer("cms", queryContext.getNodeType().getClusterGroup(), retry) ;
        }
        String url = "http://" + server.getAddress().getHost() + ":" + server.getStringAttribute("port")  + "/node/query" ;
        String resultStr = "" ;
        try {
            queryContext.getData().put(CONFIG_, JsonUtils.toJsonString(queryContext.getConfig())) ;
            if(StringUtils.isNotEmpty(queryContext.getDateFormat())) {
                queryContext.getData().put(DATE_FORMAT_, queryContext.getDateFormat());
            }
            if(queryContext.getFileUrlFormat() != null) {
                queryContext.getData().put(FILE_URL_FORMAT_, JsonUtils.toJsonString(queryContext.getFileUrlFormat()));
            }

            resultStr = ApiUtils.callApiMethod(url, queryContext.getData(), 5000, 1000, ApiUtils.POST) ;
            return JsonUtils.parsingJsonToMap(resultStr) ;
        } catch (IOException e) {
            if(!retry){
                return callQuery(queryContext, true) ;
            }
            logger.error(resultStr);
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
        }
        return null ;
    }

    public static Map<String, Object> callNode(NodeType nodeType, String id, boolean retry) {
        Member server = getClusterService().getClusterServer("cache", nodeType.getClusterGroup(), retry) ;
        if(server == null){
            server = getClusterService().getClusterServer("cms", nodeType.getClusterGroup(), retry) ;
        }
        String url = "http://" + server.getAddress().getHost() + ":" + server.getStringAttribute("port")  + "/helper/read" ;
        return getSyncNodeResult(url, nodeType.getTypeId(), id, retry);
    }

    private static Map<String, Object> getSyncNodeResult(String url, String typeId, String id, boolean retry) {
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("typeId", typeId) ;
            param.put("id", id) ;
//            logger.info("CALL NODE : {} - {}", url, param);

            String resultStr = ApiUtils.callApiMethod(url, param, 5000, 1000, ApiUtils.POST) ;
            Map<String, Object> result = JsonUtils.parsingJsonToMap(resultStr) ;
            if(result.containsKey("result") && result.containsKey("resultMessage")){
                logger.error("CALL NODE ERROR : {} - {} - {}, {}", url, result, typeId, id);
                if(!retry){
                    return callNode(NodeUtils.getNodeType(typeId), id, true) ;
                }
                return null;
            }
            return result ;
        } catch (Exception e) {
            if(!retry){
                return callNode(NodeUtils.getNodeType(typeId), id, true) ;
            }
            logger.error("CONNECT ERROR : " + url );
        }
        return null ;
    }


    public static Map<String, Object> callNode(Member member, String typeId, String id) {
        String url = "http://" + member.getAddress().getHost() + ":" + member.getStringAttribute("port")  + "/helper/read" ;
        return getSyncNodeResult(url, typeId, id, false);
    }

    public static Map<String, Object> callNode(String server, String typeId, String id) {
        String url = "http://" + server + "/helper/read" ;
        return getSyncNodeResult(url, typeId, id, false);
    }
}
