package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 1..
 */
@Controller
@RequestMapping(value = { "dbSync" })
public class DBSyncController {

    Logger logger = Logger.getLogger(DBSyncController.class);

    private final String PROCESS_TID = "dbSyncProcess"
            , MAPPER_TID = "dbSyncMapper"
            , HISTORY_TID = "dbSyncHistory";


    @Autowired
    NodeService nodeService;

    @Autowired
    DBService dbService;

    @RequestMapping(value = { "/execute/{executeId}" }, produces = {"application/json"})
    public @ResponseBody String execute (@PathVariable String executeId, HttpServletRequest request) throws Exception {

        logger.info("Start executing database Sync process with Id [ " + executeId + " ]");
        JSONObject rtn = new JSONObject();
        String result = "500", result_msg = "ERROR", cause = "";

        try{
            //dbSyncProcess Node 에서 가져오기
            Object dbSyncMetaInfo = nodeService.readNode(null, PROCESS_TID, executeId);
            if(dbSyncMetaInfo == null) throw new Exception("[ " + executeId + " ] does not exists");
            Map itemMap = (Map) ((Map) dbSyncMetaInfo).get("item");
            String query = String.valueOf(itemMap.get("query"));
            String targetNodeType = String.valueOf(itemMap.get("targetNodeType"));
            String targetDs = String.valueOf(itemMap.get("targetDs"));

            // 쿼리
            JdbcTemplate template = dbService.getJdbcTemplate(targetDs);
            List<Map<String, Object>> queryRs = template.queryForList(query);

            // mapper 정보 추출
            List<Node> mapperInfoList = NodeUtils.getNodeList(MAPPER_TID, "executeId_matching=" + executeId);
            Map<String, String> mapperStore = extractPropertyColumnMap(mapperInfoList);


            List<Map> altered = new ArrayList<>();
            for(Map qMap : queryRs) {
                // mapping 정보에 맞게 변경
                Map<String, Object> fit = mapData(targetNodeType, qMap, mapperStore);
                nodeService.saveNode(fit);
                altered.add(fit);
            }

            rtn.put("items", queryRs);
            result = "200";
            result_msg = "SUCCESS";
        } catch (Exception e) {
            logger.error("Database synchronize execution Error :: ", e);
            cause = e.getMessage();
        }
        rtn.put("result", result);
        rtn.put("result_msg", result_msg);
        rtn.put("cause", cause);
        return rtn.toString();
    }

    private Map<String, String> extractPropertyColumnMap(List<Node> mappers) {
        Map<String, String> mapperMap = new HashMap<>();
        for(Node mapper : mappers) {
            mapperMap.put(String.valueOf(mapper.get("propertyId")), String.valueOf(mapper.get("columnName")));
        }
        return mapperMap;
    }

    /*
    * 개별 쿼리 결과를 node.pid 에 맞춰줌
    * */
    private Map<String, Object> mapData (String targetNodeType, Map<String, Object> singleQueryResult, Map<String, String> mapperStore) {
        Map<String, Object> combined = new HashMap<String, Object>();
        mapperStore.forEach((k, v) -> {
            combined.put(k, singleQueryResult.get(v));
        });
        combined.put("typeId", targetNodeType);
        return combined;
    }

}
