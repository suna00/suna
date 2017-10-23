package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsHstByMbrService")
public class VoteItemStatsHstByMbrService {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstByMbrService.class);

    @Autowired
    NodeService nodeService;

    @Autowired
    private VoteItemStatsHstByMbrTask voteItemStatsHstByMbrTask;

    public void execVoteItemStatsHstByMbr(ExecuteContext context) {

        logger.info("start schedule task - execVoteItemStatsHstByMbr");

        // execute voteItemStatsHstByMbrTask only once
        voteItemStatsHstByMbrTask.execVoteItemStatsHstByMbr();

        logger.info("complete schedule task - execVoteItemStatsHstByMbr");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
