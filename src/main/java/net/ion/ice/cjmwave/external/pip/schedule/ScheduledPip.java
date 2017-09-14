package net.ion.ice.cjmwave.external.pip.schedule;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.cjmwave.external.pip.PipApiService;
import net.ion.ice.core.node.NodeService;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 13..
 * 72 시간 단위로 execute 되며 이 부분은 net.ion.ice.schedule 에서 읽힘
 */
public class ScheduledPip {

    private ApplicationContext ctx;
    private PipApiService pipService;
    private NodeService nodeService;
    private Logger logger;

    public ScheduledPip() {
        logger = Logger.getLogger(ScheduledPip.class);
        ctx = ApplicationContextManager.getContext();
        pipService = (PipApiService) ctx.getBean("pipApiService");
        nodeService = (NodeService) ctx.getBean("nodeService");
    }

    private Map<String, Object> convertToAcceptableMap (String nodeType, Object info) {
        Map<String, Object> nodeAcceptableMap = new HashMap<>();
        nodeAcceptableMap.put("nodeType", nodeType);
        /*
        * Converting 할 때 rule define + 다국어 정보 어떻게 처리하지
        * */


        return nodeAcceptableMap;
    }

    public void doWork() {
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
