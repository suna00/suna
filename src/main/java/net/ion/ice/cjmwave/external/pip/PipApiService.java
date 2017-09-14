package net.ion.ice.cjmwave.external.pip;

import net.ion.ice.cjmwave.db.sync.DBSyncService;
import net.ion.ice.cjmwave.external.utils.CommonNetworkUtils;
import net.ion.ice.cjmwave.external.utils.JSONNetworkUtils;
import net.ion.ice.cjmwave.external.utils.MigrationUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.NodeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Map<String,Object> match (String nodeTypeId, Map<String, Object> data) {
        Map <String, Object> transformed = new HashMap<String, Object>();
        transformed.put("typeId", nodeTypeId);
        transformed.put("mnetIfTrtYn", "Y");
        switch (nodeTypeId) {
            case "program" :
                transformed.put("pgmId", data.get("programid"));
                transformed.put("pgmNm", data.get("title"));
                transformed.put("pgmDesc", data.get("synopsis"));
                transformed.put("chId", data.get("channelid"));
                transformed.put("bradStDate", data.get("startdate"));
                transformed.put("bradFnsDate", data.get("enddate"));
                transformed.put("repImgPath", data.get("programimg"));
                transformed.put("thumbnailImgPath", data.get("programthumimg"));
                transformed.put("catmName", data.get("prsn_nm"));
                transformed.put("showYn", "Y"); //?

                if(data.containsKey("multilanguage")) {
                    List<Map<String, Object>> multiLangArr = (List<Map<String, Object>>) data.get("multilanguage");
                    // 받아줄 테이블이 없음
                }
                break;
            case "pgmVideo" :
                transformed.put("programId", data.get("programid"));
                transformed.put("contentId", data.get("contentid"));
                transformed.put("contentTitle", data.get("contenttitle"));
                transformed.put("cornerId", data.get("cornerid"));
                transformed.put("clipOrder", data.get("cliporder"));
                transformed.put("title", data.get("title"));
                transformed.put("synopsis", data.get("synopsis"));
                transformed.put("prsnName", data.get("prsn_nm"));
                transformed.put("prsnFName", data.get("prsn_f_nm"));
                transformed.put("prsnNo", data.get("prsn_no"));
                transformed.put("searchKeyword", data.get("searchkeyword"));
                transformed.put("clipType", data.get("cliptype"));
                transformed.put("contentType", data.get("contenttype"));
                transformed.put("broadDate", data.get("broaddate"));
                transformed.put("contentImgUrl", data.get("contentimg"));
                transformed.put("playTime", data.get("playtime"));
                transformed.put("targetAge", data.get("targetage"));
//                transformed.put("adLink", data.get("adlink"));
                transformed.put("price", data.get("price"));
                transformed.put("isMasterClip", data.get("ismasterclip"));
                transformed.put("isUse", data.get("isuse"));
                transformed.put("isFullVod", data.get("isfullvod"));

                if(data.containsKey("multilanguage")) {
                    List<Map<String, Object>> multiLangArr = (List<Map<String, Object>>) data.get("multilanguage");
                    // 받아줄 테이블이 없음
                }

                break;
        }
        return transformed;
    }


    public void doProgramMigration (String paramStr) throws Exception {
        List fetchedPrograms = JSONNetworkUtils.fetchJSON(programApiUrl, paramStr);
        Date startTime = new Date();
        int successCnt = 0, skippedCnt = 0;
        for(Object program : fetchedPrograms) {
            // 이 형변환이 실패하면 전체가 의미가 없음 - 그냥 규약 위반
            Map<String, Object> programMap = (Map<String, Object>) program;
            try {
                nodeService.saveNode(match("program", programMap));
                successCnt++;
            } catch (Exception e) {
                logger.error("Failed to register PIP program :: ", e);
                // 실패 목록에 쌓기
                skippedCnt++;
            }
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
            try {
                Map<String, Object> clipMap = (Map<String, Object>) clip;
                nodeService.saveNode(match("pgmVideo", clipMap));
                successCnt++;
            } catch (Exception e) {
                logger.error("Failed to register PIP clip :: ", e);
                // 실패 목록에 쌓기
                skippedCnt++;
            }
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