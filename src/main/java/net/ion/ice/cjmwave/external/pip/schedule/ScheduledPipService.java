package net.ion.ice.cjmwave.external.pip.schedule;

import net.ion.ice.cjmwave.external.pip.PipApiService;
import net.ion.ice.core.node.NodeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 13..
 * 72 시간 단위로 execute 되며 이 부분은 net.ion.ice.schedule 에서 읽힘
 */
@Service
public class ScheduledPipService {

    private ApplicationContext ctx;

    @Autowired
    private PipApiService pipService;

    @Autowired
    private NodeService nodeService;

    private Logger logger = Logger.getLogger(ScheduledPipService.class);


    private Map<String, Object> convertToAcceptableMap (String nodeType, Object info) {
        Map<String, Object> nodeAcceptableMap = new HashMap<>();
        nodeAcceptableMap.put("nodeType", nodeType);
        /*
        * Converting 할 때 rule define + 다국어 정보 어떻게 처리하지
        * */


        return nodeAcceptableMap;
    }

    public void executeScheduledMigration() {
        /*
        try{
            // 여기 매칭되는 노드가 어떤 노드인지 알아볼 필요가 있겠다
            List newPrograms = pipService.fetchProgram("type=recent");
            List newClips = pipService.fetchClipMedia("type=recent");

            for(Object programInfo : newPrograms) {
                nodeService.saveNode(convertToAcceptableMap("program", programInfo));
            }

            for(Object clipMediaInfo : newClips) {
                nodeService.saveNode(convertToAcceptableMap("pgmVideo", clipMediaInfo));
            }

        } catch (Exception e) {
            logger.error("Error while pip migration. do failure action :: ", e);
        }
        */
    }
}
