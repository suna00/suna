package net.ion.ice.cjmwave.votePrtcptHst;

import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service("artistVoteStatsTask")
public class ArtistVoteStatsTask {
    private static Logger logger = LoggerFactory.getLogger(ArtistVoteStatsTask.class);

    public static final String VOTE_BAS_INFO = "voteBasInfo";

    private JdbcTemplate jdbcTemplate;
    private JdbcTemplate jdbcTemplateReplica;


    @Autowired
    private DBService dbService;

    public void artistVoteStatsJob() {
        artistVoteStatsJob(null);
    }

    public void artistVoteStatsJob(String dateString) {
        logger.info("start schedule task - artistVoteStatsJob");

        if (jdbcTemplate == null) {
            jdbcTemplate = dbService.getJdbcTemplate("authDb");
        }

        if (jdbcTemplateReplica == null) {
            jdbcTemplateReplica = dbService.getJdbcTemplate("authDbReplica");
        }


        // 작업일자
        DateTime workingDate = new DateTime();
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd");
        if (dateString != null) {
            try {
                workingDate = dateTimeFormatter.parseDateTime(dateString);
            } catch (Exception e) { }
        }

//        workingDate = workingDate.minusMinutes(10);

        logger.info("working date : {}", workingDate.toString("yyyy-MM-dd"));
        logger.info("first day of this week : {}", workingDate.withDayOfWeek(DateTimeConstants.MONDAY).toString("yyyy-MM-dd"));
        logger.info("working month : {}", workingDate.toString("yyyy-MM"));
        logger.info("working year : {}", workingDate.toString("yyyy"));


        // total vote count
        Long totalVoteCount = 0L;

        // 집계용
        Map<String, DailyVoteStat> voteCountByArtistStat = new HashMap<>();


        // 투표 기간안에 있는 모든 VoteBasInfo 조회
        String searchText = String.format("pstngStDt_below=%s&pstngFnsDt_above=%s",
                workingDate.plusDays(1).toString("yyyyMMddHHmmss"),
                workingDate.minusDays(1).toString("yyyyMMddHHmmss"));
        List<Node> voteBasInfoList = NodeUtils.getNodeList(VOTE_BAS_INFO, searchText);

        for (Node voteBasInfo : voteBasInfoList) {
            logger.info("vote item stat schedule task - {} - {} ", voteBasInfo.getId(), voteBasInfo.getStringValue("voteNm"));


            // 투표별 집계
            List<Map<String, Object>> voteCountByArtistList = getVoteCountByArtist(voteBasInfo.getId(), workingDate.toString("yyyyMMdd"));
            for (Map<String, Object> voteCountByArtist : voteCountByArtistList) {

                // 아티스트
                String voteItemSeq = voteCountByArtist.get("voteItemSeq").toString();
                // 투표수
                Long voteCount = (Long) voteCountByArtist.get("voteCount");

                // 아티스트별 투표 누적
                if (voteCountByArtistStat.containsKey(voteItemSeq)) {
                    DailyVoteStat dailyVoteStat = voteCountByArtistStat.get(voteItemSeq);
                    dailyVoteStat.voteCount += voteCount;

                    voteCountByArtistStat.put(voteItemSeq, dailyVoteStat);
                } else {
                    voteCountByArtistStat.put(voteItemSeq, new DailyVoteStat(
                                    workingDate.toString("yyyy-MM-dd"),
                                    voteItemSeq,
                                    voteCount,
                                    workingDate.toString("yyyy"),
                                    workingDate.toString("yyyyMM"),
                                    workingDate.withDayOfWeek(DateTimeConstants.MONDAY).toString("yyyyMMdd")
                            )
                    );
                }

                // 총 투표수 누적
                totalVoteCount += voteCount;
            }
        }


        // 점유율, 순위 처리
        List<DailyVoteStat> dailyVoteStatList = determineRanking(voteCountByArtistStat, totalVoteCount);

        try {
            // 일간 통계 적재
            persistDailyStat(dailyVoteStatList, workingDate);


            // 주간 통계
            weeklyProc(workingDate);

            // 월간 통계
            monthlyProc(workingDate);

            // 년간 통계
            yearlyProc(workingDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }


    private void weeklyProc(DateTime workingDate) {
        String perdStDate = workingDate.withDayOfWeek(DateTimeConstants.MONDAY).toString("yyyy-MM-dd");
        String perdFnsDate = workingDate.withDayOfWeek(DateTimeConstants.SUNDAY).toString("yyyy-MM-dd");

        BigDecimal totalVoteCount = new BigDecimal(0);

        // 집계용
        List<VoteStatInfo> weeklyVoteStatList = new ArrayList<>();


        List<Map<String, Object>> weeklyVoteCountByArtistList = getWeeklyVoteCountByArtist(workingDate);
        for (Map<String, Object> weeklyVoteCountByArtist : weeklyVoteCountByArtistList) {
            // 아티스트
            String artistId = weeklyVoteCountByArtist.get("artistId").toString();
            // 투표수
            BigDecimal voteCount = (BigDecimal) weeklyVoteCountByArtist.get("voteCount");

            weeklyVoteStatList.add(new VoteStatInfo(perdStDate, perdFnsDate, artistId, voteCount));

            totalVoteCount = totalVoteCount.add(voteCount);
        }


        // 아티스트 투표수 정렬
        weeklyVoteStatList.sort(Comparator.comparing(VoteStatInfo::getVoteNum).reversed());


        // 점유율, 순위
        int rank = 1;
        for (VoteStatInfo weeklyVoteStat : weeklyVoteStatList) {
            // 점유율 = 아티스트 투표수 / 총 투표수 * 100
            weeklyVoteStat.voteRate = Double.parseDouble(String.format("%.2f", weeklyVoteStat.voteNum.doubleValue() / totalVoteCount.doubleValue() * 100));

            // 순위
            weeklyVoteStat.rankNum = new BigDecimal(rank++);
        }

        persistWeeklyStat(weeklyVoteStatList);
    }


    private void monthlyProc(DateTime workingDate) {
        String perdYm = workingDate.toString("yyyy-MM");

        BigDecimal totalVoteCount = new BigDecimal(0);

        // 집계용
        List<VoteStatInfo> monthlyVoteStatList = new ArrayList<>();


        List<Map<String, Object>> monthlyVoteCountByArtistList = getMonthlyVoteCountByArtist(workingDate);
        for (Map<String, Object> monthlyVoteCountByArtist : monthlyVoteCountByArtistList) {
            // 아티스트
            String artistId = monthlyVoteCountByArtist.get("artistId").toString();
            // 투표수
            BigDecimal voteCount = (BigDecimal) monthlyVoteCountByArtist.get("voteCount");

            VoteStatInfo voteStatInfo = new VoteStatInfo(artistId, voteCount);
            voteStatInfo.perdYm = perdYm;
            monthlyVoteStatList.add(voteStatInfo);

            totalVoteCount = totalVoteCount.add(voteCount);
        }


        // 아티스트 투표수 정렬
        monthlyVoteStatList.sort(Comparator.comparing(VoteStatInfo::getVoteNum).reversed());


        // 점유율, 순위
        int rank = 1;
        for (VoteStatInfo monthlyVoteStat : monthlyVoteStatList) {
            // 점유율 = 아티스트 투표수 / 총 투표수 * 100
            monthlyVoteStat.voteRate = Double.parseDouble(String.format("%.2f", monthlyVoteStat.voteNum.doubleValue() / totalVoteCount.doubleValue() * 100));

            // 순위
            monthlyVoteStat.rankNum = new BigDecimal(rank++);
        }

        persistMonthlyStat(monthlyVoteStatList);
    }


    private void yearlyProc(DateTime workingDate) {
        String perdYear = workingDate.toString("yyyy");

        BigDecimal totalVoteCount = new BigDecimal(0);

        // 집계용
        List<VoteStatInfo> yearlyVoteStatList = new ArrayList<>();


        List<Map<String, Object>> yearlyVoteCountByArtistList = getYearlyVoteCountByArtist(workingDate);
        for (Map<String, Object> yearlyVoteCountByArtist : yearlyVoteCountByArtistList) {
            // 아티스트
            String artistId = yearlyVoteCountByArtist.get("artistId").toString();
            // 투표수
            BigDecimal voteCount = (BigDecimal) yearlyVoteCountByArtist.get("voteCount");

            VoteStatInfo voteStatInfo = new VoteStatInfo(artistId, voteCount);
            voteStatInfo.perdYear = perdYear;
            yearlyVoteStatList.add(voteStatInfo);

            totalVoteCount = totalVoteCount.add(voteCount);
        }


        // 아티스트 투표수 정렬
        yearlyVoteStatList.sort(Comparator.comparing(VoteStatInfo::getVoteNum).reversed());


        // 점유율, 순위
        int rank = 1;
        for (VoteStatInfo yearlyVoteStat : yearlyVoteStatList) {
            // 점유율 = 아티스트 투표수 / 총 투표수 * 100
            yearlyVoteStat.voteRate = Double.parseDouble(String.format("%.2f", yearlyVoteStat.voteNum.doubleValue() / totalVoteCount.doubleValue() * 100));

            // 순위
            yearlyVoteStat.rankNum = new BigDecimal(rank++);
        }

        persistYearlyStat(yearlyVoteStatList);
    }


    private List<DailyVoteStat> determineRanking(Map<String, DailyVoteStat> voteCountByArtistStat, Long totalVoteCount) {
        List<DailyVoteStat> dailyVoteStatList = new ArrayList<>(voteCountByArtistStat.values());

        // 점유율 = 아티스트 투표수 / 총 투표수 * 100
        for (DailyVoteStat dailyVoteStat : dailyVoteStatList) {
            dailyVoteStat.totalVoteCount = totalVoteCount;

            Double share = (double) dailyVoteStat.voteCount / dailyVoteStat.totalVoteCount * 100.0;
            BigDecimal tempValue = new BigDecimal(share);
            tempValue = tempValue.setScale(2, RoundingMode.HALF_UP);
            dailyVoteStat.share = tempValue.doubleValue();
        }

        // 아티스트 투표수 정렬
        dailyVoteStatList.sort(Comparator.comparing(DailyVoteStat::getVoteCount).reversed());

        // 순위
        int rank = 1;
        for (DailyVoteStat dailyVoteStat : dailyVoteStatList) {
            dailyVoteStat.rank = rank++;
        }

        return dailyVoteStatList;
    }


    private void persistDailyStat(List<DailyVoteStat> dailyVoteStatList, DateTime workingDate) {
        // 작업일 통계 삭제
        String delete = "DELETE FROM artistVoteStatsByDaily WHERE voteDate = ?";
        jdbcTemplate.update(delete, workingDate.toString("yyyy-MM-dd"));
        logger.info("DELETE FROM artistVoteStatsByDaily WHERE voteDate = '{}';", workingDate.toString("yyyy-MM-dd"));

        // 새로운 통계 기록
        String insert = "INSERT INTO artistVoteStatsByDaily(voteDate, artistId, voteNum, voteRate, rankNum, owner, created, year, yearMonth, firstDayOfWeek) " +
                "VALUES(?, ?, ?, ?, ?, 'owner', NOW(), ?, ?, ?)";
        for (DailyVoteStat dailyVoteStat : dailyVoteStatList) {
            jdbcTemplate.update(insert,
                    dailyVoteStat.voteDate,
                    dailyVoteStat.artistId,
                    dailyVoteStat.voteCount,
                    dailyVoteStat.share,
                    dailyVoteStat.rank,
                    dailyVoteStat.year,
                    dailyVoteStat.yearMonth,
                    dailyVoteStat.firstDayOfWeek);
            logger.info("INSERT INTO artistVoteStatsByDaily(voteDate, artistId, voteNum, voteRate, rankNum, owner, created, year, yearMonth, firstDayOfWeek) VALUES('{}', '{}', {}, {}, {}, 'owner', NOW(), '{}', '{}', '{}');",
                    dailyVoteStat.voteDate,
                    dailyVoteStat.artistId,
                    dailyVoteStat.voteCount,
                    dailyVoteStat.share,
                    dailyVoteStat.rank,
                    dailyVoteStat.year,
                    dailyVoteStat.yearMonth,
                    dailyVoteStat.firstDayOfWeek);
        }
    }


    private void persistWeeklyStat(List<VoteStatInfo> weeklyVoteStatList) {
        // 작업주 통계 삭제
        String delete = "DELETE FROM artistVoteStatsByWly WHERE perdStDate = ?";
        jdbcTemplate.update(delete, weeklyVoteStatList.get(0).perdStDate);
        logger.info("DELETE FROM artistVoteStatsByWly WHERE perdStDate = '{}';", weeklyVoteStatList.get(0).perdStDate);

        // 새로운 통계 기록
        String insert = "INSERT INTO artistVoteStatsByWly(perdStDate, perdFnsDate, artistId, rankNum, voteRate, voteNum, owner, created) " +
                "VALUES(?, ?, ?, ?, ?, ?, 'owner', NOW())";
        for (VoteStatInfo weeklyVoteStat : weeklyVoteStatList) {
            jdbcTemplate.update(insert,
                    weeklyVoteStat.perdStDate,
                    weeklyVoteStat.perdFnsDate,
                    weeklyVoteStat.artistId,
                    weeklyVoteStat.rankNum,
                    weeklyVoteStat.voteRate,
                    weeklyVoteStat.voteNum);
            logger.info("INSERT INTO artistVoteStatsByWly(perdStDate, perdFnsDate, artistId, rankNum, voteRate, voteNum, owner, created) VALUES('{}', '{}', '{}', {}, {}, {}, 'owner', NOW());",
                    weeklyVoteStat.perdStDate,
                    weeklyVoteStat.perdFnsDate,
                    weeklyVoteStat.artistId,
                    weeklyVoteStat.rankNum,
                    weeklyVoteStat.voteRate,
                    weeklyVoteStat.voteNum);
        }
    }


    private void persistMonthlyStat(List<VoteStatInfo> monthlyVoteStatList) {
        // 작업월 통계 삭제
        String delete = "DELETE FROM artistVoteStatsByMly WHERE perdYm = ?";
        jdbcTemplate.update(delete, monthlyVoteStatList.get(0).perdYm);
        logger.info("DELETE FROM artistVoteStatsByMly WHERE perdYm = '{}';", monthlyVoteStatList.get(0).perdYm);

        // 새로운 통계 기록
        String insert = "INSERT INTO artistVoteStatsByMly(perdYm, artistId, rankNum, voteRate, voteNum, owner, created) " +
                "VALUES(?, ?, ?, ?, ?, 'owner', NOW())";
        for (VoteStatInfo monthlyVoteStat : monthlyVoteStatList) {
            jdbcTemplate.update(insert,
                    monthlyVoteStat.perdYm,
                    monthlyVoteStat.artistId,
                    monthlyVoteStat.rankNum,
                    monthlyVoteStat.voteRate,
                    monthlyVoteStat.voteNum);
            logger.info("INSERT INTO artistVoteStatsByMly(perdYm, artistId, rankNum, voteRate, voteNum, owner, created) VALUES('{}', '{}', {}, {}, {}, 'owner', NOW());",
                    monthlyVoteStat.perdYm,
                    monthlyVoteStat.artistId,
                    monthlyVoteStat.rankNum,
                    monthlyVoteStat.voteRate,
                    monthlyVoteStat.voteNum);
        }
    }


    private void persistYearlyStat(List<VoteStatInfo> yearlyVoteStatList) {
        // 작업월 통계 삭제
        String delete = "DELETE FROM artistVoteStatsByYear WHERE perdYear = ?";
        jdbcTemplate.update(delete, yearlyVoteStatList.get(0).perdYear);
        logger.info("DELETE FROM artistVoteStatsByYear WHERE perdYear = '{}';", yearlyVoteStatList.get(0).perdYear);

        // 새로운 통계 기록
        String insert = "INSERT INTO artistVoteStatsByYear(perdYear, artistId, rankNum, voteRate, voteNum, owner, created) " +
                "VALUES(?, ?, ?, ?, ?, 'owner', NOW())";
        for (VoteStatInfo yearlyVoteStat : yearlyVoteStatList) {
            jdbcTemplate.update(insert,
                    yearlyVoteStat.perdYear,
                    yearlyVoteStat.artistId,
                    yearlyVoteStat.rankNum,
                    yearlyVoteStat.voteRate,
                    yearlyVoteStat.voteNum);
            logger.info("INSERT INTO artistVoteStatsByYear(perdYear, artistId, rankNum, voteRate, voteNum, owner, created) VALUES('{}', '{}', {}, {}, {}, 'owner', NOW());",
                    yearlyVoteStat.perdYear,
                    yearlyVoteStat.artistId,
                    yearlyVoteStat.rankNum,
                    yearlyVoteStat.voteRate,
                    yearlyVoteStat.voteNum);
        }
    }


    private List<Map<String, Object>> getVoteInfoList() {
        String query =
                "SELECT "
                        + " T2.* "
                        + " FROM "
                        + " sersVoteItemInfo T1 "
                        + " JOIN "
                        + " voteBasInfo T2 "
                        + " ON "
                        + " T1.sersItemVoteSeq = T2.voteSeq "
                        + " WHERE "
                        + " T1.voteSeq = 800100 "
                ;
        return jdbcTemplateReplica.queryForList(query);
    }


    private List<Map<String, Object>> getVoteCountByArtist(String voteSeq, String voteDate) {
        String query = String.format(
                "SELECT "
                        + " T1.voteItemSeq, COUNT(T1.mbrId) AS voteCount "
                        + " FROM "
                        + voteSeq + "_voteItemHstByMbr T1 "
                        + " WHERE "
                        + " T1.voteDate = '%s' "
                        + " GROUP BY "
                        + " T1.voteItemSeq ", voteDate);
//        logger.info("query :{}", query);
        return jdbcTemplateReplica.queryForList(query);
    }


    private List<Map<String, Object>> getWeeklyVoteCountByArtist(DateTime workingDate) {
        String query = String.format(
                "SELECT " +
                        " artistId " +
                        " , SUM(voteNum) AS voteCount " +
                        " FROM " +
                        " artistVoteStatsByDaily " +
                        " WHERE " +
                        " firstDayOfWeek = '%s' " +
                        " GROUP BY " +
                        " artistId ", workingDate.withDayOfWeek(DateTimeConstants.MONDAY).toString("yyyyMMdd"));
//        logger.info("query :{}", query);
        return jdbcTemplate.queryForList(query);
    }


    private List<Map<String, Object>> getMonthlyVoteCountByArtist(DateTime workingDate) {
        String query = String.format(
                "SELECT " +
                        " artistId " +
                        " , SUM(voteNum) AS voteCount " +
                        " FROM " +
                        " artistVoteStatsByDaily " +
                        " WHERE " +
                        " yearMonth = '%s' " +
                        " GROUP BY " +
                        " artistId ", workingDate.toString("yyyyMM"));
//        logger.info("query :{}", query);
        return jdbcTemplate.queryForList(query);
    }


    private List<Map<String, Object>> getYearlyVoteCountByArtist(DateTime workingDate) {
        String query = String.format(
                "SELECT " +
                        " artistId " +
                        " , SUM(voteNum) AS voteCount " +
                        " FROM " +
                        " artistVoteStatsByDaily " +
                        " WHERE " +
                        " year = '%s' " +
                        " GROUP BY " +
                        " artistId ", workingDate.toString("yyyy"));
//        logger.info("query :{}", query);
        return jdbcTemplate.queryForList(query);
    }


    public class DailyVoteStat {
        public String voteDate;
        public String artistId;
        public Long voteCount;
        public Long totalVoteCount;
        public Double share;
        public Integer rank;

        public String year;
        public String yearMonth;
        public String firstDayOfWeek;

        public DailyVoteStat(String voteDate, String artistId, Long voteCount, String year, String yearMonth, String firstDayOfWeek) {
            this.voteDate = voteDate;
            this.artistId = artistId;
            this.voteCount = voteCount;
            this.year = year;
            this.yearMonth = yearMonth;
            this.firstDayOfWeek = firstDayOfWeek;
        }

        public Long getVoteCount() {
            return this.voteCount;
        }

        public String toString() {
            return String.format("voteDate:%s artistId:%s voteCount:%d totalVoteCount:%d share:%f rank:%d year:%s yearMonth:%s firstDayOfWeek:%s",
                    this.voteDate,
                    this.artistId,
                    this.voteCount,
                    this.totalVoteCount,
                    this.share,
                    this.rank,
                    this.year,
                    this.yearMonth,
                    this.firstDayOfWeek
            );
        }
    }


    public class VoteStatInfo {
        public static final String TYPE_YEAR = "YEAR";
        public static final String TYPE_MONTH = "MONTH";
        public static final String TYPE_WEEK = "WEEK";

        public String perdStDate;
        public String perdFnsDate;
        public String perdYm;
        public String perdYear;

        public String artistId;
        public BigDecimal rankNum;
        public Double voteRate;
        public BigDecimal voteNum;

        public VoteStatInfo(String perdStDate, String perdFnsDate, String artistId, BigDecimal voteNum) {
            this.perdStDate = perdStDate;
            this.perdFnsDate = perdFnsDate;
            this.artistId = artistId;
            this.voteNum = voteNum;
        }

        public VoteStatInfo(String artistId, BigDecimal voteNum) {
            this.artistId = artistId;
            this.voteNum = voteNum;
        }

        public BigDecimal getVoteNum() {
            return this.voteNum;
        }

        public String toString(String type) {
            if (type.equals(TYPE_WEEK)) {
                return String.format("perdStDate:%s perdFnsDate:%s artistId:%s rankNum:%f voteRate:%f voteNum:%f",
                        this.perdStDate,
                        this.perdFnsDate,
                        this.artistId,
                        this.rankNum,
                        this.voteRate,
                        this.voteNum
                );
            } else if (type.equals(TYPE_MONTH)) {
                return String.format("perdYm:%s artistId:%s rankNum:%f voteRate:%f voteNum:%f",
                        this.perdYm,
                        this.artistId,
                        this.rankNum,
                        this.voteRate,
                        this.voteNum
                );
            } else if (type.equals(TYPE_YEAR)) {
                return String.format("perdYear:%s artistId:%s rankNum:%f voteRate:%f voteNum:%f",
                        this.perdYear,
                        this.artistId,
                        this.rankNum,
                        this.voteRate,
                        this.voteNum
                );
            } else {
                return null;
            }
        }
    }
}
