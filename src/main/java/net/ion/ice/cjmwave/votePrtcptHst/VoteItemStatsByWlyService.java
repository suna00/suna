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

@Service("voteItemStatsByWlyService")
public class VoteItemStatsByWlyService {
    private static Logger logger = LoggerFactory.getLogger(VoteItemStatsByWlyService.class);

    @Autowired
    private VoteItemStatsByWlyTask voteItemStatsByWlyTask;

    public void execVoteItemStatsByWly(ExecuteContext context) {

        logger.info("start schedule task - execVoteItemStatsByWly");

        // execute voteItemStatsHstByMbrTask only once
        Map data = context.getData();
        if (data.get("mode") != null && data.get("mode").toString().equals("all")) {
            String target = null;
            if (data.get("voteDate") != null && data.get("voteDate").toString().length()>0){
                target = data.get("voteDate").toString();
            } else {
                Date now = new Date();
                target = DateFormatUtils.format(now, "yyyyMMdd");
            }
            voteItemStatsByWlyTask.execVoteItemStatsByWlyAll(target);
        } else {
            voteItemStatsByWlyTask.execVoteItemStatsByWly();
        }

        logger.info("complete schedule task - execVoteItemStatsByWly");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
