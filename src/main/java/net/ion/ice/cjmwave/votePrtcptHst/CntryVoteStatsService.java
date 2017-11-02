package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("cntryVoteStatsService")
public class CntryVoteStatsService {

    private static Logger logger = LoggerFactory.getLogger(CntryVoteStatsService.class);

    @Autowired
    NodeService nodeService;

    @Autowired
    private CntryVoteStatsTask cntryVoteStatsTask;

    @Autowired
    private CntryVoteStatsWlyTask cntryVoteStatsWlyTask;

    @Autowired
    private CntryVoteStatsByVoteWlyTask cntryVoteStatsByVoteWlyTask;


    /**
     * 전체 국가 투표현황
     * @param context
     */
    public void execCntryVoteStats(ExecuteContext context) {

        logger.info("start schedule task - execCntryVoteStats");

        // execute voteItemStatsByMbrTaskBySex only once
        cntryVoteStatsTask.execCntryVoteStats();

        logger.info("complete schedule task - execCntryVoteStats");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        if(context != null) context.setResult(resultServiceMap);
    }

    /**
     * 전체 국가 투표현황(금주)
     * @param context
     */
    public void execCntryVoteStatsWly(ExecuteContext context) {

        logger.info("start schedule task - execCntryVoteStatsWly");

        // execute voteItemStatsByMbrTaskByCntry only once
        cntryVoteStatsWlyTask.execCntryVoteStatsWly();

        logger.info("complete schedule task - execCntryVoteStatsWly");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        if(context != null) context.setResult(resultServiceMap);
    }

    /**
     * 투표별 국가 투표 현황(금주)
     * @param context
     */
    public void execCntryVoteStatsByVoteWly(ExecuteContext context) {

        logger.info("start schedule task - execCntryVoteStatsByVoteWly");

        // execute voteItemStatsByMbrTaskByCntry only once
        cntryVoteStatsByVoteWlyTask.execCntryVoteStatsByVoteWly();

        logger.info("complete schedule task - execCntryVoteStatsByVoteWly");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        if(context != null) context.setResult(resultServiceMap);
    }


}
