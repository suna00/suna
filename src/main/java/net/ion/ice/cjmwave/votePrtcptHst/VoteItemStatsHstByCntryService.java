package net.ion.ice.cjmwave.votePrtcptHst;


import net.ion.ice.core.context.ExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsHstByCntryService")
public class VoteItemStatsHstByCntryService {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstByCntryService.class);

    @Autowired
    private VoteItemStatsHstByCntryTask voteItemStatsHstByCntryTask;

    public void execVoteItemStatsHstByCntry(ExecuteContext context) {

        logger.info("start schedule task - execVoteItemStatsHstByCntry");

        voteItemStatsHstByCntryTask.execVoteItemStatsHstByCntry();

        logger.info("complete schedule task - execVoteItemStatsHstByCntry");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
