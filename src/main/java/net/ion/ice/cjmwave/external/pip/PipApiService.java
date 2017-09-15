package net.ion.ice.cjmwave.external.pip;

import net.ion.ice.cjmwave.db.sync.DBSyncService;
import net.ion.ice.cjmwave.external.utils.CommonNetworkUtils;
import net.ion.ice.cjmwave.external.utils.JSONNetworkUtils;
import net.ion.ice.cjmwave.external.utils.MigrationUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.NodeService;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by juneyoungoh on 2017. 9. 5..
 */
@Service
public class PipApiService {

    private Logger logger = Logger.getLogger(PipApiService.class);

    private static final String PROGRAM_NT = "program", CLIP_NT = "pgmVideo";

    @Value("${cjapi.pip.programurl}")
    String programApiUrl;

    @Value("${cjapi.pip.clipmediaurl}")
    String clipMediaApiUrl;

    @Autowired
    NodeService nodeService;

    @Autowired
    DBService dbService;

    private JdbcTemplate template;

    @PostConstruct
    public void prepareJdbcTemplate () {
        template = dbService.getJdbcTemplate("cjDb");
    }

    private Map<String,Object> match (String nodeTypeId, Map<String, Object> data) throws ParseException {
        Map <String, Object> transformed = new HashMap<String, Object>();
        transformed.put("typeId", nodeTypeId);
        transformed.put("mnetIfTrtYn", 1);  // default
        SimpleDateFormat sdf14 = new SimpleDateFormat("yyyyMMddHHmmss");
        String isUse = "N";
        String regDateStr = "", modifyDateStr = "";
        Date regDate = null, modifyDate = null;


        switch (nodeTypeId) {
            case "program2" :
                logger.info("빌어먹을 거 :: " + String.valueOf(data));
                transformed.put("programId", data.get("programid"));
                transformed.put("programCd", data.get("programcode"));
                transformed.put("title", data.get("title"));
                transformed.put("synopsis", data.get("synopsis"));
                transformed.put("genre", data.get("genre"));

                transformed.put("targetAge", data.get("targetage"));
                transformed.put("chId", data.get("channelid"));
                transformed.put("searchKeyword", data.get("searchkeyword"));
                transformed.put("startDate", data.get("startdate"));
                transformed.put("endDate", data.get("endDate"));

                transformed.put("weekCd", data.get("weekcode"));    // 추가 작업 필요
                transformed.put("startTime", data.get("starttime"));
                transformed.put("endTime", data.get("endTime"));

                regDateStr = String.valueOf(data.get("regdate"));
                regDate = sdf14.parse(regDateStr);
                transformed.put("regDate", regDate);

                modifyDateStr = String.valueOf(data.get("modifydate"));
                modifyDate = sdf14.parse(modifyDateStr);
                transformed.put("modifyDate", modifyDate);


                transformed.put("homepageUrl", data.get("homepageurl"));
                transformed.put("reviewUrl", data.get("reviewurl"));
                transformed.put("bbsUrl", data.get("bbsurl"));
                transformed.put("programImg", data.get("programimg"));
                transformed.put("pgmPosterImg", data.get("programposterimg"));

                transformed.put("programBannerImg", data.get("programbannerimg"));
                transformed.put("programThumbImg", data.get("programthumimg"));
                transformed.put("prsnNm", data.get("prsn_nm"));
                transformed.put("prsnFNm", data.get("prsn_f_nm"));
                transformed.put("prsnNo", data.get("prsn_no"));

                transformed.put("actor", data.get("actor"));
                transformed.put("director", data.get("director"));
                isUse = String.valueOf("isuse").toUpperCase();
                transformed.put("isUse", "Y".equals(isUse) ? 1 : 0);
                break;
            case "pgmVideo2" :

                transformed.put("pgmId", data.get("programid"));
                transformed.put("contentId", data.get("contentid"));
                transformed.put("contentTitle", data.get("contenttitle"));
                transformed.put("cornerId", data.get("cornerid"));
                transformed.put("clipOrder", data.get("cliporder"));

                transformed.put("title", data.get("title"));
                transformed.put("synopsis", data.get("synopsis"));
                transformed.put("prsnNm", data.get("prsn_nm"));
                transformed.put("prsnFNm", data.get("prsn_f_nm"));
                transformed.put("prsnNo", data.get("prsn_no"));

                transformed.put("searchKeyword", data.get("searchkeyword"));
                transformed.put("mediaUrl", data.get("mediaurl"));
                transformed.put("itemTypeId", data.get("itemtypeid"));
                transformed.put("clipType", data.get("cliptype"));
                transformed.put("contentType", data.get("contenttype"));

                String broadDateStr = String.valueOf(data.get("broaddate"));
                Date braodDate = sdf14.parse(broadDateStr);
                transformed.put("broadDate", braodDate);

                regDateStr = String.valueOf(data.get("regdate"));
                regDate = sdf14.parse(regDateStr);
                transformed.put("regDate", regDate);

                modifyDateStr = String.valueOf(data.get("modifydate"));
                modifyDate = sdf14.parse(modifyDateStr);
                transformed.put("modifyDate", modifyDate);

                transformed.put("contentImgUrl", data.get("contentimg"));
                transformed.put("playTime", data.get("playtime"));
                transformed.put("targetAge", data.get("targetage"));
                transformed.put("adLink", data.get("adlink"));
                transformed.put("price", data.get("price"));

                String isMasterClip = String.valueOf(data.get("ismasterclip")).trim().toUpperCase();
                transformed.put("isMasterClip", ("Y".equals(isMasterClip) ? 1: 0));

                isUse = String.valueOf(data.get("isuse")).trim().toUpperCase();
                transformed.put("isUse", ("Y".equals(isUse) ? 1: 0));

                String isFullVod = String.valueOf(data.get("isfullvod")).trim().toUpperCase();
                transformed.put("isFullVod", ("Y".equals(isFullVod) ? 1: 0));

                transformed.put("rcmdContsYn", 0); // 모르겠음

                break;
        }

        // 다국어는 한번에 처리함
        List<String> countryList = new ArrayList<>();
        if(data.containsKey("multilanguage")) {
            List<Map<String, Object>> multiLangArr = (List<Map<String, Object>>) data.get("multilanguage");
            // 받아줄 테이블이 없음
            for(Object mLang : multiLangArr) {
                Map<String, Object> mLangMap = (Map<String, Object>) mLang;
                if(mLangMap.containsKey("lang_cd")) {
                    countryList.add(String.valueOf(mLangMap.get("lang_cd")));
                }
            }
        }
        transformed.put("showCntryCdList", StringUtils.join(countryList, ","));



        logger.info("변환 후 :: " + String.valueOf(transformed));
        return transformed;
    }


    public void doProgramMigration (String paramStr) throws Exception {
        List fetchedPrograms = JSONNetworkUtils.fetchJSON(programApiUrl, paramStr);
        Date startTime = new Date();
        int successCnt = 0, skippedCnt = 0;
        for(Object program : fetchedPrograms) {
            int rs = 0;
            // 이 형변환이 실패하면 전체가 의미가 없음 - 그냥 규약 위반
            Map<String, Object> programMap = (Map<String, Object>) program;
            try {
                nodeService.saveNodeWithException(match("program2", programMap));
                logger.info("PIP MIGRATION PROGRAM FETCHED :: " + String.valueOf(programMap));
                successCnt++;
                rs = 1;
            } catch (Exception e) {
                logger.error("Failed to register PIP program :: ", e);
                // 실패 목록에 쌓기
                skippedCnt++;
            }
            MigrationUtils.recordSingleDate(template,"program2", String.valueOf(programMap), rs);
        }
        long jobTaken = (new Date().getTime() - startTime.getTime());
        MigrationUtils.printReport(startTime, "PIPProgramRecent", "SKIP", successCnt, skippedCnt);
        MigrationUtils.recordResult(template, "PIP", "MANUAL", paramStr, null
                , "program", successCnt, skippedCnt, jobTaken, startTime);
    }

    public void doClipMediaMigration (String paramStr) throws Exception {
        List fetchedClips = JSONNetworkUtils.fetchJSON(clipMediaApiUrl, paramStr);
        Date startTime = new Date();
        int successCnt = 0, skippedCnt = 0;
        for(Object clip : fetchedClips) {
            int rs = 0;
            Map<String, Object> clipMap = (Map<String, Object>) clip;
            try {
                nodeService.saveNodeWithException(match("pgmVideo2", clipMap));
                logger.info("PIP MIGRATION MEDIACLIP FETCHED :: " + String.valueOf(clipMap));
                successCnt++;
                rs = 1;
            } catch (Exception e) {
                logger.error("Failed to register PIP clip :: ", e);
                // 실패 목록에 쌓기
                skippedCnt++;
            }
            MigrationUtils.recordSingleDate(template, "pgmVideo2", String.valueOf(clipMap), rs);
        }
        long jobTaken = (new Date().getTime() - startTime.getTime());
        MigrationUtils.printReport(startTime, "PIPClipMediaRecent", "SKIP", successCnt, skippedCnt);
        MigrationUtils.recordResult(template, "PIP", "MANUAL", paramStr, null
                ,"pgmVideo", successCnt, skippedCnt, jobTaken, startTime);
    }

    public void doProgramMigration (Map paramMap) throws Exception {
        doProgramMigration(CommonNetworkUtils.MapToString(paramMap));
    }

    public void doClipMediaMigration (Map paramMap) throws Exception {
        doClipMediaMigration(CommonNetworkUtils.MapToString(paramMap));
    }
};