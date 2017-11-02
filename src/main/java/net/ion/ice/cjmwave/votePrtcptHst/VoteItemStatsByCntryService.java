package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsByCntryService")
public class VoteItemStatsByCntryService {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstByCntryTask.class);

    @Autowired
    private VoteItemStatsByCntryTask voteItemStatsByCntryTask;

    public void execVoteItemStatsByCntry(ExecuteContext context) {

        logger.info("start schedule task - execVoteItemStatsByCntry");

        voteItemStatsByCntryTask.execVoteItemStatsByCntry();

        logger.info("complete schedule task - execVoteItemStatsByCntry");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
