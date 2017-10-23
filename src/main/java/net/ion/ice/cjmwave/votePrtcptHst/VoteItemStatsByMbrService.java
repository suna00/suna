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

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service("voteItemStatsByMbrService")
public class VoteItemStatsByMbrService {

    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsByMbrService.class);

    @Autowired
    NodeService nodeService;

    @Autowired
    private VoteItemStatsByMbrTask voteItemStatsByMbrTask;

    // 성별.
    public void execVoteItemStatsBySex(ExecuteContext context) {

        logger.info("start schedule task - execVoteItemStatsBySex");

        // execute voteItemStatsByMbrTaskBySex only once
        voteItemStatsByMbrTask.execVoteItemStatsBySex();

        logger.info("complete schedule task - execVoteItemStatsBySex");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }

    // 국가.
    public void execVoteItemStatsByCntry(ExecuteContext context) {

        logger.info("start schedule task - execVoteItemStatsByCntry");

        // execute voteItemStatsByMbrTaskByCntry only once
        voteItemStatsByMbrTask.execVoteItemStatsByCntry();

        logger.info("complete schedule task - execVoteItemStatsByCntry");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }


}
