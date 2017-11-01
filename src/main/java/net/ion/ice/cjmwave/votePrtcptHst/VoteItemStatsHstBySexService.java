package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsHstBySexService")
public class VoteItemStatsHstBySexService {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstBySexService.class);

    @Autowired
    private VoteItemStatsHstBySexTask voteItemStatsHstBySexTask;


    public void execVoteItemStatsHstBySex(ExecuteContext context) {

        logger.info("start schedule task - execVoteItemStatsHstBySex");

        voteItemStatsHstBySexTask.execVoteItemStatsHstBySex();

        logger.info("complete schedule task - execVoteItemStatsHstBySex");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
