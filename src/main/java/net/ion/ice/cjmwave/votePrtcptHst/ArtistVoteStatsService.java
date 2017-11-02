package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

        Map data = context.getData();

        // 특정일 작업 수행
        // ?date=20171031
        if (! StringUtils.isEmpty(data.get("date"))) {
            DateTime workingDate = new DateTime();
            DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd");
            try {
                workingDate = dateTimeFormatter.parseDateTime(data.get("date").toString());

            } catch (Exception e) { }

            artistVoteStatsTask.artistVoteStatsJob(workingDate.toString("yyyyMMdd"));

        } else {
            // 금일 작업 수행
            artistVoteStatsTask.artistVoteStatsJob();
        }

        logger.info("complete schedule task - execArtistVoteStats");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        context.setResult(resultServiceMap);
    }


    @Autowired
    NodeService nodeService;

    @Autowired
    private ArtistVoteStatsBySexTask artistVoteStatsBySexTask;

    @Autowired
    private ArtistVoteStatsBySexWlyTask artistVoteStatsBySexWlyTask;

    @Autowired
    private ArtistVoteStatsByCntryTask artistVoteStatsByCntryTask;

    @Autowired
    private ArtistVoteStatsByCntryWlyTask artistVoteStatsByCntryWlyTask;

    /**
     * 성별 아티스트 투표현황 일별
     * @param context
     */
    public void execArtistVoteStatsBySex(ExecuteContext context) {

        logger.info("start schedule task - execArtistVoteStatsBySex");

        // execute voteItemStatsByMbrTaskBySex only once
        artistVoteStatsBySexTask.execArtistVoteStatsBySex();

        logger.info("complete schedule task - execArtistVoteStatsBySex");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        if(context != null) context.setResult(resultServiceMap);
    }

    /**
     * 성별 아티스트 투표현황  주별
     * @param context
     */
    public void execArtistVoteStatsBySexWly(ExecuteContext context) {

        logger.info("start schedule task - execArtistVoteStatsBySexWly");

        // execute voteItemStatsByMbrTaskBySex only once
        artistVoteStatsBySexWlyTask.execArtistVoteStatsBySex();

        logger.info("complete schedule task - execArtistVoteStatsBySexWly");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        if(context != null) context.setResult(resultServiceMap);
    }

    /**
     * 국가별 아티스트 투표현황.. 일별
     * @param context
     */
    public void execArtistVoteStatsByCntry(ExecuteContext context) {

        logger.info("start schedule task - execArtistVoteStatsByCntry");

        // execute voteItemStatsByMbrTaskByCntry only once
        artistVoteStatsByCntryTask.execArtistVoteStatsByCntry();

        logger.info("complete schedule task - execArtistVoteStatsByCntry");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        if(context != null) context.setResult(resultServiceMap);
    }

    /**
     * 국가별 아티스트 투표현황.. 주별
     * @param context
     */
    public void execArtistVoteStatsByCntryWly(ExecuteContext context) {

        logger.info("start schedule task - execArtistVoteStatsByCntryWly");

        // execute voteItemStatsByMbrTaskByCntry only once
        artistVoteStatsByCntryWlyTask.execArtistVoteStatsByCntry();

        logger.info("complete schedule task - execArtistVoteStatsByCntryWly");

        Map<String, String> resultServiceMap = new ConcurrentHashMap<>();
        resultServiceMap.put("status", "COMPLETE");
        if(context != null) context.setResult(resultServiceMap);
    }

}
