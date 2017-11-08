package net.ion.ice.cjmwave.monitor;


import net.ion.ice.core.data.DBService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by juneyoungoh on 2017. 10. 25..
 */
@Service
public class MnetStatsMonitorService {

    @Autowired
    DBService dbService;

    private Logger logger = Logger.getLogger(MnetStatsMonitorService.class);
    private ConcurrentHashMap<String, IpAddress> IpAddressMap = new ConcurrentHashMap<>();
    private JdbcTemplate authReplicaTemplate;


    @PostConstruct
    public void init(){
        try{
            authReplicaTemplate = dbService.getJdbcTemplate("authDbReplica");
        } catch (Exception e) {
            logger.error("Stat Monitoring function disabled, since :: ", e);
        }
    }

    public List<Map<String, Object>> getDailyReport (String date) throws Exception {
        String dashed = date.substring(0,4) + "-" + date.substring(4,6) + "-" + date.substring(6,8);
        String query =
                "SELECT '일자별 투표 건수 전체' FROM dual "
                + "UNION ALL "
                + "SELECT concat(date_format(voteDate,'%Y-%m-%d'),' : ', cast(format(count(*),0) AS char),'건') AS voteCnt FROM 800100_voteHstByMbr WHERE voteDate=? "
                + "UNION ALL "
                + "SELECT '일자별 투표 건수(Qoo10 제외)' FROM dual "
                + "UNION ALL "
                + "SELECT concat(date_format(voteDate,'%Y-%m-%d'),' : ', cast(format(count(*),0) AS char),'건') AS voteCnt FROM 800100_voteHstByMbr WHERE voteDate=? AND mbrId not LIKE '%>qoo10::%' "
                + "UNION ALL "
                + "SELECT '일자별 투표 건수(Qoo10)' FROM dual "
                + "UNION ALL "
                + "SELECT concat(date_format(voteDate,'%Y-%m-%d'),' : ', cast(format(count(*),0) AS char),'건') AS voteCnt FROM 800100_voteHstByMbr WHERE voteDate=? AND mbrId LIKE '%>qoo10::%' "
                + "UNION ALL "
                + "SELECT '일자별 회원 가입자수(Qoo10 제외)' FROM dual "
                + "UNION ALL "
                + "SELECT concat(date_format(sbscDt,'%Y-%m-%d'),' : ', cast(format(count(*),0) AS char),'명') AS mbrCnt FROM mbrInfo WHERE sbscDt between ? AND ? AND snsTypeCd != '10' "
                + "UNION ALL "
                + "SELECT '일자별 회원 가입자수(Qoo10)' FROM dual "
                + "UNION ALL "
                + "SELECT concat(date_format(sbscDt,'%Y-%m-%d'),' : ', cast(format(count(*),0) AS char),'명') AS mbrCnt FROM mbrInfo WHERE sbscDt between ? AND ? AND snsTypeCd = '10' "
                + "UNION ALL "
                + "SELECT 'K-POP POLL 투표수' FROM dual "
                + "UNION ALL "
                + "SELECT concat(date_format(voteDate,'%Y-%m-%d'),' : ', cast(format(count(*),0) AS char),'건') AS voteCnt FROM 800103_voteHstByMbr WHERE voteDate=? "
                + "UNION ALL "
                + "SELECT 'K-POP POLL 항목별 투표수' FROM dual "
                + "UNION ALL "
                + "SELECT concat(voteItemseq, ' : ', date_format(voteDate,'%Y-%m-%d'),' : ', cast(format(count(*),0) AS char),'건') AS voteCnt FROM `800103_voteItemHstByMbr` WHERE voteDate=? GROUP BY voteItemseq";
        return authReplicaTemplate.queryForList(query, date, date, date, dashed + " 00:00:00.000", dashed + " 23:59:59.999", dashed + " 00:00:00.000", dashed + " 23:59:59.999", date , date);
    }
}
