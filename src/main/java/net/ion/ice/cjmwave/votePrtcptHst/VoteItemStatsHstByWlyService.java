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

import java.text.SimpleDateFormat;
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
            voteItemStatsHstByWlyTask.execVoteItemStatsHstByTargetWly(target);
        } else {
            voteItemStatsHstByWlyTask.execVoteItemStatsHstByWly();
        }

        logger.info("complete schedule task - execVoteItemStatsHstByWly");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
