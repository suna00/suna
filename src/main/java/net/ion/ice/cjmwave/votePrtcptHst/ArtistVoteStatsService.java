package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.context.ExecuteContext;
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
}
