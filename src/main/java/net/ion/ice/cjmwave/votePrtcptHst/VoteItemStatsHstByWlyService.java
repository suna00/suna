package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsHstByWlyService")
public class VoteItemStatsHstByWlyService {
    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsHstByWlyService.class);

    @Autowired
    private VoteItemStatsHstByWlyTask voteItemStatsHstByWlyTask;

    // 주간 ( 월 ~ 일 )
    public void execVoteItemStatsHstByWly(ExecuteContext context) {

        logger.info("start schedule task - execVoteItemStatsHstByWly");

        // execute voteItemStatsHstByWlyTask only once
        voteItemStatsHstByWlyTask.execVoteItemStatsHstByWly();

        logger.info("complete schedule task - execVoteItemStatsHstByWly");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
