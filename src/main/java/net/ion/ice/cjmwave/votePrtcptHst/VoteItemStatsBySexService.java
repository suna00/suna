package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsBySexService")
public class VoteItemStatsBySexService {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstByCntryTask.class);

    @Autowired
    private VoteItemStatsBySexTask voteItemStatsBySexTask;

    public void execVoteItemStatsBySex(ExecuteContext context) {

        logger.info("start schedule task - execVoteItemStatsBySex");

        voteItemStatsBySexTask.execVoteItemStatsBySex();

        logger.info("complete schedule task - execVoteItemStatsBySex");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
