package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("artistVoteStatService")
public class ArtistVoteStatsService {
    private static Logger logger = LoggerFactory.getLogger(ArtistVoteStatsService.class);

    @Autowired
    private ArtistVoteStatsTask artistVoteStatsTask;

    public void execArtistVoteStats(ExecuteContext context) {

        logger.info("start schedule task - execArtistVoteStats");

//        // execute voteItemStatsHstByMbrTask only once
//        Map data = context.getData();
//        if (data.get("mode") != null && data.get("mode").toString().equals("all")) {
//            String target = null;
//            if (data.get("voteDate") != null && data.get("voteDate").toString().length()>0){
//                target = data.get("voteDate").toString();
//            } else {
//                Date now = new Date();
//                target = DateFormatUtils.format(now, "yyyyMMdd");
//            }
//            voteItemStatsByWlyTask.execVoteItemStatsByWlyAll(target);
//        } else {
//            voteItemStatsByWlyTask.execVoteItemStatsByWly();
//        }
//
//        logger.info("complete schedule task - execVoteItemStatsByWly");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }
}
