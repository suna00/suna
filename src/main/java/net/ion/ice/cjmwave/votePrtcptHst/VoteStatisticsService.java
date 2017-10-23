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

import javax.xml.crypto.Data;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by latinus.hong on 2017. 9. 20.
 */
@Service("voteStatisticsService")
public class VoteStatisticsService {

    private static Logger logger = LoggerFactory.getLogger(VoteStatisticsService.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private VoteItemStatsTask voteItemStatsTask;

    public void voteItemStatsJob(ExecuteContext context) {

        logger.info("start schedule task - voteItemStatsJob");

        // execute voteItemstatsTask only once
        voteItemStatsTask.voteItemStatsJob();

        logger.info("complete schedule task - voteItemStatsJob");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
