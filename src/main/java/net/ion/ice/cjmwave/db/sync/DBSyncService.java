package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.cjmwave.db.sync.utils.NodeMappingUtils;
import net.ion.ice.cjmwave.db.sync.utils.SyntaxUtils;
import net.ion.ice.cjmwave.external.utils.MigrationUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.file.TolerableMissingFileException;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.core.node.NodeType;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by juneyoungoh on 2017. 9. 5..
 * 서비스에서 서비스를 Autowired
 */
@Service
public class DBSyncService {

    private Logger logger = Logger.getLogger(DBSyncService.class);

    private final String PROCESS_TID = "dbSyncProcess"
            , MAPPER_TID = "dbSyncMapper";

    private static int BATCH_UNIT = 100;

    boolean useBatch = false;

    @Autowired
    TaskExecutor taskExecutor;


    @Autowired
    NodeService nodeService;

    @Autowired
    DBService dbService;

    @Autowired
    DBProcessStorage storage;

    @Autowired
    Environment env;

    String defaultFilePath;

    private JdbcTemplate ice2Template;

    private String mnetExecuteIds[] = {
            "album"
            , "artist"
            , "musicVideo"
            , "song"
            , "mcdChartBasInfo", "mcdChartStats"
    };

    private String [] mnetPartialExecuteIds = {
            "albumPart"
            , "artistPart"
            , "musicVideoPart"
            , "songPart"
            , "mcdChartBasInfoPart"
            , "mcdChartStatsPart"
    };

    @PostConstruct
    public void init(){
        try{
            defaultFilePath = env.getProperty("file.default.path");
            ice2Template = dbService.getJdbcTemplate("cjDb");
        } catch (Exception e) {
            logger.error("Could not initialize JdbcTemplate");
        }
    }

    /*
    * 10-01
    * 기존 쿼리 속도가 운영에서 문제가 있어 한번에 처리하는 부분을 나눴음
    * 이 부분은 시간 관계상 하드코딩으로 처림됨.
    * */
    private Map<String, Object> appendCommaStringProperties (String nodeType, Map<String, Object> fit){
        // 차트는 그룹콘캣이 불필요하므로 처리할 필요가 없음
        logger.debug("ORIGINAL NODE SUB QUERY ::" + nodeType);
        String genreSubQuery = "";
        String countrySubQuery = "";
        String keywordSubQuery = "";
        String artistSubQuery = "";
        String albumSubQuery = "";
        String songSubQuery = "";
        String id = "";
        switch(nodeType) {
            case "album" :
                id = String.valueOf(fit.get("albumId"));
                // 장르코드, 국가코드, 키워드, 아티스트 아이디, 곡 아이디
                genreSubQuery = "SELECT GROUP_CONCAT(GENRE_CD) AS genreCd FROM MT_ALBUM_GENRE WHERE ALBUM_ID = ?";
                countrySubQuery = "SELECT GROUP_CONCAT(country_cd) AS showCntryCdList FROM MT_ALBUM_COUNTRY WHERE ALBUM_ID = ?";
                keywordSubQuery = "SELECT GROUP_CONCAT(KEYWORD) AS findKywrd FROM MT_ALBUM_KEYWORD WHERE ALBUM_ID = ?";
                artistSubQuery = "SELECT GROUP_CONCAT(ARTIST_ID) AS relArtistIds FROM MT_ALBUM_ARTIST WHERE ALBUM_ID = ?";
                songSubQuery = "SELECT GROUP_CONCAT(SONG_ID) AS relSongIds FROM MT_ALBUM_SONGS WHERE ALBUM_ID = ?";

                break;
            case "artist" :
                id = String.valueOf(fit.get("artistId"));
                // 장르코드. 키워드, 국가코드
                genreSubQuery = "SELECT GROUP_CONCAT(GENRE_CD) AS genreCd FROM MT_ARTIST_GENRE WHERE ARTIST_ID = ?";
                keywordSubQuery = "SELECT GROUP_CONCAT(KEYWORD) AS findKywrd FROM MT_ARTIST_KEYWORD WHERE ARTIST_ID = ?";
                countrySubQuery = "SELECT GROUP_CONCAT(country_cd) AS showCntryCdList FROM MT_ARTIST_COUNTRY WHERE ARTIST_ID = ?";

                break;
            case "musicVideo" :
                id = String.valueOf(fit.get("mvId"));
                // 키워드, 국가, 아티스트 - youtubeUrl, toudoUrl??
                keywordSubQuery = "SELECT GROUP_CONCAT(KEYWORD) FROM MT_MV_KEYWORD WHERE MV_ID = ?";
                countrySubQuery = "SELECT GROUP_CONCAT(country_cd) FROM MT_MV_COUNTRY WHERE mv_id = ?";
                artistSubQuery = "SELECT GROUP_CONCAT(ARTIST_ID) FROM MT_MV_ARTIST WHERE MV_ID = ?";
                break;
            case "song" :
                id = String.valueOf(fit.get("songId"));
                // 키워드, 국가, 아티스트, 앨범
                keywordSubQuery = "SELECT GROUP_CONCAT(KEYWORD) AS findKywrd FROM MT_SONG_KEYWORD WHERE SONG_ID = ?";
                countrySubQuery = "SELECT GROUP_CONCAT(country_cd) AS showCntryCdList FROM MT_SONG_COUNTRY WHERE song_id = ?";
                artistSubQuery = "SELECT GROUP_CONCAT(ARTIST_ID) AS relArtistIds FROM MT_SONG_ARTIST WHERE SONG_ID = ?";
                albumSubQuery = "SELECT GROUP_CONCAT(ALBUM_ID) AS relAlbumIds FROM MT_ALBUM_SONGS WHERE SONG_ID = ?";
                break;
        }

        if(genreSubQuery != null && genreSubQuery.length() > 0) {
            Map<String, Object> rs = new HashMap<>();
            try{
                rs = ice2Template.queryForMap(genreSubQuery, id);
                switch(nodeType) {
                    case "album":
                    case "artist":
                        fit.putAll(rs);
                        break;
                }
            } catch (Exception e) {
                logger.error("Failed to retrieve sub information GENRE :: " + nodeType + "." + id);
            }
        }

        if(countrySubQuery != null && countrySubQuery.length() > 0) {
            Map<String, Object> rs = new HashMap<>();
            try{
                rs = ice2Template.queryForMap(countrySubQuery, id);
                fit.putAll(rs);
            } catch (Exception e) {
                logger.error("Failed to retrieve sub information COUNTRY :: " + nodeType + "." + id);
            }
        }

        if(keywordSubQuery != null && keywordSubQuery.length() > 0) {
            Map<String, Object> rs = new HashMap<>();
            try{
                rs = ice2Template.queryForMap(keywordSubQuery, id);
                fit.putAll(rs);
            } catch (Exception e) {
                logger.error("Failed to retrieve sub information KEYWORD :: " + nodeType + "." + id);
            }
        }

        if(artistSubQuery != null && artistSubQuery.length() > 0) {
            Map<String, Object> rs = new HashMap<>();
            try{
                rs = ice2Template.queryForMap(artistSubQuery, id);
                switch (nodeType) {
                    case "album":
                    case "musicVideo":
                    case "song":
                        fit.putAll(rs);
                        break;
                }
            } catch (Exception e) {
                logger.error("Failed to retrieve sub information ARTIST :: " + nodeType + "." + id);
            }
        }

        if(songSubQuery != null && songSubQuery.length() > 0) {
            Map<String, Object> rs = new HashMap<>();
            try{
                rs = ice2Template.queryForMap(songSubQuery, id);
                switch (nodeType){
                    case  "album" :
                        fit.putAll(rs);
                        break;
                }
            } catch (Exception e) {
                logger.error("Failed to retrieve sub information SONG :: " + nodeType + "." + id);
            }
        }


        if(albumSubQuery != null && albumSubQuery.length() > 0) {
            Map<String, Object> rs = new HashMap<>();
            try{
                rs = ice2Template.queryForMap(albumSubQuery, id);
                switch (nodeType) {
                    case "song":
                        fit.putAll(rs);
                        break;
                }
            } catch (Exception e) {
                logger.error("Failed to retrieve sub information ALBUM :: " + nodeType + "." + id);
            }
        }
        logger.debug("ORIGINAL NODE INFORMATION :: " + String.valueOf(fit));
        return fit;
    }

    private Map<String, Object> appendMultiLangCommaStringProperties (String originalNodeType, String langCd, Map<String, Object> additional){
        // 차트는 그룹콘캣이 불필요하므로 처리할 필요가 없음
        logger.debug("MULTILINGUAL SEPARATED SUB QUERY :: " + originalNodeType + "/" + langCd);
        String keywordSubQuery = "";
        String id = "";
        switch(originalNodeType) {
            case "album" :
                id = String.valueOf(additional.get("albumId"));
                keywordSubQuery = "SELECT GROUP_CONCAT(keyword) AS findKywrd FROM MT_ALBUM_KEYWORD_META WHERE album_id = ? AND lang_cd = ?";
                break;
            case "artist" :
                id = String.valueOf(additional.get("artistId"));
                keywordSubQuery = "SELECT GROUP_CONCAT(keyword) AS findKywrd FROM MT_ARTIST_KEYWORD_META WHERE artist_id = ? AND lang_cd = ?";
                break;
            case "musicVideo" :
                id = String.valueOf(additional.get("mvId"));
                keywordSubQuery = "SELECT GROUP_CONCAT(KEYWORD) AS findKywrd FROM MT_MV_KEYWORD_META WHERE mv_id = ? AND lang_cd = ?";
                break;
            case "song" :
                id = String.valueOf(additional.get("songId"));
                keywordSubQuery = "SELECT GROUP_CONCAT(keyword) AS findKywrd FROM MT_SONG_KEYWORD_META WHERE song_id = ? AND lang_cd = ?";
                break;
        }

        if(keywordSubQuery != null && keywordSubQuery.length() > 0) {
            Map<String, Object> rs = new HashMap<>();
            try{
                rs = ice2Template.queryForMap(keywordSubQuery, id, langCd);
                Iterator<String> iterator = rs.keySet().iterator();
                while (iterator.hasNext()) {
                    String k = iterator.next();
                    Object v = rs.get(k);
                    additional.put(k + "_" + langCd, v);
                }
            } catch (Exception e) {
                logger.error("Failed to retrieve Multilingual sub information KEYWORD :: " + originalNodeType + "." + id);
            }
        }

        return additional;
    }

    private Map<String, Object> getMultiLanguageInfo (Map<String, Object> queryMap, String multiLangQuery, String foreignKey) {
        Map<String, Object> additional = new HashMap<String, Object>();
        try{
            if(foreignKey != null) {
                // 다국어 사용하는 쿼리 노드라면 추가쿼리를 해서 노드 정보에 다국어 정보를 추가한다.
                Map<String, Object> idMap = new HashMap<>();
                idMap.put("id", queryMap.get(foreignKey));
                Map<String, Object> jdbcParam = SyntaxUtils.parse(multiLangQuery, idMap);
                String jdbcQuery = String.valueOf(jdbcParam.get("query"));
                Object[] params = (Object[]) jdbcParam.get("params");

                String originalTypeId = String.valueOf(queryMap.get("typeId"));

                List<Map<String, Object>> multiLanguageInformation = ice2Template.queryForList(jdbcQuery, params);
                // langcd 랑 뭐 이것저것 들었다고 치자
                for(Map<String, Object> singleLangMap : multiLanguageInformation) {
                    String langCd = String.valueOf(singleLangMap.get("langCd"));
                    langCd = langCd.toLowerCase();
                    if("chn".equals(langCd)) {
                        langCd = "zho-cn";
                    }else if("twn".equals(langCd)) {
                        langCd = "zho-tw";
                    }

                    Iterator<String> multiIter = singleLangMap.keySet().iterator();
                    while(multiIter.hasNext()) {
                        String k = multiIter.next();
                        Object v = singleLangMap.get(k);

                        // ==================================================
                        // ==================================================
                        // ==================================================
                        // 쿼리 성능 이슈로 GROUP_CONCAT 분리 1001
                        // ==================================================
                        // ==================================================
                        // ==================================================
                        additional = appendMultiLangCommaStringProperties(originalTypeId, langCd, additional);
                        // ==================================================
                        // ==================================================
                        // ==================================================
                        // 1001 ENDS
                        // ==================================================
                        // ==================================================
                        // ==================================================


                        if(!k.equals("langCd")) {
                            additional.put(k + "_" + langCd, v);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to set multi language information, but consider it as normal", e);
        }

        logger.debug("MULTILINGUAL INFORMATION :: " + String.valueOf(additional));
        return additional;
    }

    // 복수 키 수용 못함
    private Map<String, Object> getImageInfo(Map<String, Object> queryMap, String targetNodeTypeId, String nodePKPid) {
        Map<String, Object> imageValues = new HashMap<String, Object>();
        if(targetNodeTypeId.equals("album")
                || targetNodeTypeId.equals("artist")){
            try {
                if(queryMap.containsKey(nodePKPid)){
                    String nodePKValue = String.valueOf(queryMap.get(nodePKPid));
                    String mnetFileUrl =
                            MigrationUtils.getMnetFileUrl(nodePKValue
                                    , targetNodeTypeId, targetNodeTypeId.equals("album") ? "360" : "320");
                    imageValues.put("imgUrl", mnetFileUrl);
                }
            } catch (Exception e) {
                logger.error("Failed to Load Image ... ", e);
            }
        }
        return imageValues;
    }

    private List<String> executeSingleTaskAndRecord (Node executionNode, List<Map<String, Object>> queryRs, String mig_target, String mig_type, boolean byId, Map<String, Object> idReport) throws Exception {
        List<String> successIds = new ArrayList<>();
        int successCnt = 0;
        int skippedCnt = 0;
        Date startTime = new Date();
        String executeId = executionNode.getId();
        String targetNodeType = String.valueOf(executionNode.get("targetNodeType"));
        String failPolicy = String.valueOf(executionNode.get("onFail")).trim().toUpperCase();
        failPolicy = (!"NULL".equals(failPolicy) && "STOP".equals(failPolicy)) ? "STOP" : "SKIP";

        List<Node> mapperInfoList = NodeUtils.getNodeList(MAPPER_TID, "executeId_matching=" + executeId);
        Map<String, String> mapperStore = NodeMappingUtils.extractPropertyColumnMap(mapperInfoList);

        /*
            다국어 처리가 여기로 변경되어야 함
            {pid}_{langCd}
        * */
        final String MLANG_PID = "multiLanguageQuery";
        boolean useMultiLanguage = executionNode.containsKey(MLANG_PID) && executionNode.get(MLANG_PID) != null;
        String multiLangQuery = null;
        String currentNodePKPid = NodeMappingUtils.retrieveNodePrimaryKey(nodeService, targetNodeType);
        if(useMultiLanguage) {
            multiLangQuery = String.valueOf(executionNode.get("multiLanguageQuery"));
        }

        logger.info("MYSQL QUERY RESULT COUNT :: " + queryRs.size());
        for (Map qMap : queryRs) {
            int rs = 0;
            // 만약에 Node 의 mnetIfTrtYn 가 false 라면 무시한다.
            try{
                String pkValue = String.valueOf(qMap.get(currentNodePKPid));
                Node targetNode = nodeService.read(targetNodeType, pkValue);
                if(targetNode != null) {
                    boolean mnetIfTrtYn = targetNode.getBooleanValue("mnetIfTrtYn");
                    logger.info("mnetIfTrtYn Value For :: " + targetNodeType + " :: "
                            + pkValue + " :: mnetIfTrtYn :: " + String.valueOf(mnetIfTrtYn));
                    if(!mnetIfTrtYn) {
                        logger.info("mnetIfTrtYn is [ false ]. Skip update");
                        continue;
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to check [ mnetIfTrtYn ]. Ignore and override");
            }


            Map<String, Object> fit = NodeMappingUtils.mapData(targetNodeType, qMap, mapperStore);
            fit = appendCommaStringProperties(targetNodeType, fit);
            // 다국어 처리
            try{
                if(useMultiLanguage) {
                    fit.putAll(getMultiLanguageInfo(qMap, multiLangQuery, currentNodePKPid));
                }
            } catch (Exception e) {
                logger.error("Failed to set multi language information, but consider it as normal");
            }
            fit.putAll(getImageInfo(qMap,targetNodeType, currentNodePKPid));


            logger.info("CREATE MIGRATION NODE :: " + String.valueOf(fit));
            try{
                Node finished = nodeService.saveNodeWithException(fit); // 반환 안됨
                successIds.add(finished.getId());
                successCnt ++;
                rs = 1;
            } catch (Exception e) {
                // 실패한다면 실패 기록을 DB 에 저장한다.
                logger.error("Recording exception :: ", e);
                if(failPolicy.equals("STOP")){
                    break;
                } else {
                    skippedCnt++;
                    if(e instanceof TolerableMissingFileException) {
                        MigrationUtils.handoutNodeFailReport(ice2Template, ((TolerableMissingFileException) e).getRootCause().getClass().getName(), fit);
                    } else {
                        MigrationUtils.handoutNodeFailReport(ice2Template, e.getClass().getName(), fit);
                    }
                }
            }
        }

        long jobTaken = (new Date().getTime() - startTime.getTime());
        if(!byId){
            MigrationUtils.printReport(startTime, executeId, failPolicy, successCnt, skippedCnt);
            MigrationUtils.recordResult(ice2Template, mig_target, mig_type, null, null
                    ,targetNodeType, successCnt, skippedCnt, jobTaken, startTime);
        } else {
            if(idReport.isEmpty()) {

                idReport.put(MigrationUtils.MIGTARGET, mig_target);
                idReport.put(MigrationUtils.MIGTYPE, mig_type);
                idReport.put(MigrationUtils.NODE_TARGET, targetNodeType);
                idReport.put(MigrationUtils.JOB_SUCCESS, successCnt);
                idReport.put(MigrationUtils.JOB_SKIPPED, skippedCnt);
                idReport.put(MigrationUtils.JOB_DURATION, jobTaken);
                idReport.put(MigrationUtils.JOB_STARTON, startTime);
            } else {
                Map<String, Object> newRepo = new HashMap<String, Object>();
                idReport.put(MigrationUtils.JOB_SUCCESS, successCnt);
                idReport.put(MigrationUtils.JOB_SKIPPED, skippedCnt);
                idReport.put(MigrationUtils.JOB_DURATION, jobTaken);
                mergeReport(idReport, newRepo);
            }
        }
        return  successIds;
    }

    /*
    * 결과가 안나올 때까지 이터레이션하면서 처리한다, 쿼리에 반드시 limit @{start} @{unit} 있어야 한다
    * start unit 외 다른 파라미터는 받을 수 없음
    * */
    private void executeWithIteration (String executeId, Integer startPage, Integer totalPages) throws Exception {
//        int max = 1;
        logger.info("DBSyncService.executeWithIteration :: " + executeId);
        boolean loop = true;
        startPage = (startPage == null) ? 1 : startPage;
        int i = startPage - 1; // 사용자는 1 페이지부터 시작할 건데, 계산은 인덱스 기준이므로 1 뺌
        int unit = BATCH_UNIT;
        int successCnt = 0;
        int skippedCnt = 0;
        Date startTime = new Date();
        String failPolicy = "SKIP";
        String targetNodeType = null;
        JdbcTemplate template = null;


        while(loop) {
            // i 가 0 부터 99 까지
            // 100 부터 199 까지
            int start = i * unit;
            // TEST
//            if(start > max) {
//                loop = false;
//                continue;
//            }

            if(totalPages != null && totalPages > 0) {
                if(i > totalPages -1) {
                    logger.info("Batch finished arrange from [ " + startPage + " ] page to [ " + totalPages + " ] page has finished");
                    loop = false;
                    continue;
                }
            }

            Node executionNode = nodeService.read(PROCESS_TID, executeId);
            if (executionNode == null) throw new Exception("[ " + executeId + " ] does not exists");
            String query = String.valueOf(executionNode.get("query"));
            targetNodeType = String.valueOf(executionNode.get("targetNodeType"));
            String targetDs = String.valueOf(executionNode.get("targetDs"));
            failPolicy = String.valueOf(executionNode.get("onFail")).trim().toUpperCase();
            failPolicy = (!"NULL".equals(failPolicy) && "STOP".equals(failPolicy)) ? "STOP" : "SKIP";


            if(useBatch) nodeService.startBatch(targetNodeType);
            /*
            다국어 처리가 여기로 변경되어야 함
            {pid}_{langCd}
            * */
            final String MLANG_PID = "multiLanguageQuery";
            boolean useMultiLanguage = executionNode.containsKey(MLANG_PID) && executionNode.get(MLANG_PID) != null;
            String multiLangQuery = null;
            String currentNodePKPid = NodeMappingUtils.retrieveNodePrimaryKey(nodeService, targetNodeType);
            if(useMultiLanguage) {
                multiLangQuery = String.valueOf(executionNode.get("multiLanguageQuery"));
            }

            // 쿼리
            Map<String, Object> jdbcParam = SyntaxUtils.parseWithLimit(query, start, unit);
            String jdbcQuery = String.valueOf(jdbcParam.get("query"));
            Object[] params = (Object[]) jdbcParam.get("params");

            template = dbService.getJdbcTemplate(targetDs);
            List<Map<String, Object>> queryRs = template.queryForList(jdbcQuery, params);

            if (queryRs == null || queryRs.isEmpty()) {
                loop = false;
                continue;
            } else {

                List<Node> mapperInfoList = NodeUtils.getNodeList(MAPPER_TID, "executeId_matching=" + executeId);
                Map<String, String> mapperStore = NodeMappingUtils.extractPropertyColumnMap(mapperInfoList);

                for (Map qMap : queryRs) {
                    // mapping 정보에 맞게 변경

                    int rs = 0;
                    Map<String, Object> fit = NodeMappingUtils.mapData(targetNodeType, qMap, mapperStore);
                    fit = appendCommaStringProperties(targetNodeType, fit);
                    if(useMultiLanguage) {
                        fit.putAll(getMultiLanguageInfo(qMap, multiLangQuery, currentNodePKPid));
                    }
                    fit.putAll(getImageInfo(qMap,targetNodeType, currentNodePKPid));
//                    logger.debug("CREATE INITIAL MIGRATION NODE :: " + String.valueOf(fit));
                    System.out.println("CREATE INITIAL MIGRATION NODE :: " + String.valueOf(fit));

                    try{
                        nodeService.saveNodeWithException(fit);
                        successCnt ++;
                        rs = 1;
                    } catch (Exception e) {
                        // 실패한다면 실패 기록을 DB 에 저장한다.
                        logger.error("Recording exception :: ", e);
                        if(failPolicy.equals("STOP")){
                            loop = false;
                        } else {
                            skippedCnt++;
                        }
                        if(e instanceof TolerableMissingFileException) {
                            MigrationUtils.handoutNodeFailReport(ice2Template, ((TolerableMissingFileException) e).getRootCause().getClass().getName(), fit);
                        } else {
                            MigrationUtils.handoutNodeFailReport(ice2Template, e.getClass().getName(), fit);
                        }
                    }
                }
                i++;
            }

            if(useBatch) nodeService.endBatch(targetNodeType, true);
        }

        long jobTaken = (new Date().getTime() - startTime.getTime());
        MigrationUtils.printReport(startTime, executeId, failPolicy, successCnt, skippedCnt);
        MigrationUtils.recordResult(template, "MNET", "INIT", null, null
                ,targetNodeType, successCnt, skippedCnt, jobTaken, startTime);
    }

    private void executeWithRange(String mig_target, String executeId, Date provided) throws Exception {
        String queryForLastExecution =
                "SELECT execution_date as lastUpdated "
                        + "FROM MIG_HISTORY "
                        + "WHERE mig_target = ? AND target_node = ? AND mig_type='SCHEDULE' "
                        + "ORDER BY execution_date DESC LIMIT 1";

        Node executionNode = nodeService.getNode(PROCESS_TID, executeId);
        String targetNodeType = String.valueOf(executionNode.get("targetNodeType"));
        String query = String.valueOf(executionNode.get("query"));

        Map<String, Object> lastExecutionRs = null;
        if(provided == null) {
            try {
                lastExecutionRs = ice2Template.queryForMap(queryForLastExecution, mig_target, targetNodeType);
            } catch (EmptyResultDataAccessException erda) {
                logger.info("No data found");
                lastExecutionRs = new HashMap<String, Object>();
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) - 14);
                lastExecutionRs.put("lastUpdated", cal.getTime());
            }
        } else {
            lastExecutionRs = new HashMap<String, Object>();
            lastExecutionRs.put("lastUpdated", provided);
        }

        // 그래봤자 ID 속성이 뭔지 모르잖아
        List<String> successIds = new ArrayList<>();
        Map<String, Object> jdbcParam = SyntaxUtils.parse(query, lastExecutionRs);
        String jdbcQuery = String.valueOf(jdbcParam.get("query"));
        Object[] params = (Object[]) jdbcParam.get("params");

        // executeId 실행하고 결과 처리
        List<Map<String, Object>> targets2Update =
                ice2Template.queryForList(jdbcQuery, params);

        successIds = executeSingleTaskAndRecord(executionNode, targets2Update, "MNET", "SCHEDULE", false, null);
    }

    private void executeWithIds(String mig_target, String executeId, List<String> ids) throws Exception {
        Map<String, Object> idReport = new HashedMap();
        Date startTime = new Date();

        Node tempExecutionNode = nodeService.getNode(PROCESS_TID, executeId);
        String query = String.valueOf(tempExecutionNode.get("query"));
        // executeId 실행하고 결과 처리


        for(String id : ids) {
            Map<String, Object> idMap = new HashMap<>();
            idMap.put("id", id);
            Map<String, Object> preparedQueryMap = SyntaxUtils.parse(query, idMap);
            query = String.valueOf(preparedQueryMap.get("query"));
            List<Map<String, Object>> targets2Update =
                    ice2Template.queryForList(query, id);
            executeSingleTaskAndRecord(tempExecutionNode, targets2Update, mig_target.toLowerCase(), "MANUAL", true, idReport);
        }

        try{
            MigrationUtils.printReport(startTime, executeId, "SKIP"
                    , (int) idReport.get(MigrationUtils.JOB_SUCCESS)
                    , (int) idReport.get(MigrationUtils.JOB_SKIPPED));
            MigrationUtils.recordResult(ice2Template, idReport);
        } catch (Exception e) {
            logger.error("Failed to handout report :: migration by ids", e);
        }
    }


    public void executeForInitData (String type, Integer start, Integer total) throws Exception {
        switch (type.toLowerCase()) {
            case "all" :
                for(String executeId : mnetExecuteIds) {
                    taskExecutor.execute(new ParallelDBSyncExecutor(executeId) {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIteration(this.executeId, start, total);
                        }
                    });
                }
                break;
            case "album" :
                if(storage.isAbleToRun("album")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("album") {
                        @Override
                        public void action() throws Exception {
                            System.out.println(this.executeId + " :: " + start + " :: " + total);
                            this.dbSyncService.executeWithIteration(this.executeId, start, total);
                        }
                    });
                } else {
                    logger.info("[ album ] Task is already Running. Ignore this request");
                }
                break;
            case "artist" :
                if(storage.isAbleToRun("artist")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("artist") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIteration(this.executeId, start, total);
                        }
                    });
                } else {
                    logger.info("[ artist ] Task is already Running. Ignore this request");
                }
                break;
            case "song" :
                if(storage.isAbleToRun("song")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("song") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIteration(this.executeId, start, total);
                        }
                    });
                } else {
                    logger.info("[ song ] Task is already Running. Ignore this request");
                }
                break;
            case "musicvideo" :
                if(storage.isAbleToRun("musicVideo")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("musicVideo") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIteration(this.executeId, start, total);
                        }
                    });
                } else {
                    logger.info("[ musicVideo ] Task is already Running. Ignore this request");
                }
                break;
            case "chart" :
                if(storage.isAbleToRun("mcdChartBasInfo")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("mcdChartBasInfo") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIteration(this.executeId, start, total);
                        }
                    });
                } else {
                    logger.info("[ mcdChartBasInfo ] Task is already Running. Ignore this request");
                }
                if(storage.isAbleToRun("mcdChartStats")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("mcdChartStats") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIteration(this.executeId, start, total);
                        }
                    });
                } else {
                    logger.info("[ mcdChartStats ] Task is already Running. Ignore this request");
                }
            default:
                logger.info("Could not find appropriate type for migration");
                break;
        }
    }

    public void executeForNewData (String mig_target, String type, Date provided) throws Exception {
        switch (type.toLowerCase()) {
            case "all" :
                for(String executeId : mnetPartialExecuteIds) {
                    taskExecutor.execute(new ParallelDBSyncExecutor(executeId) {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithRange(mig_target, this.executeId, provided);
                        }
                    });
                }
                break;
            case "album" :
                if(storage.isAbleToRun("albumPart")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("albumPart") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithRange(mig_target, this.executeId, provided);
                        }
                    });
                } else {
                    logger.info("[ albumPart ] Task is already Running. Ignore this request");
                }
                break;
            case "artist" :
                if(storage.isAbleToRun("artistPart")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("artistPart") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithRange(mig_target, this.executeId, provided);
                        }
                    });
                } else {
                    logger.info("[ artistPart ] Task is already Running. Ignore this request");
                }
                break;
            case "song" :
                if(storage.isAbleToRun("songPart")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("songPart") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithRange(mig_target, this.executeId, provided);
                        }
                    });
                } else {
                    logger.info("[ songPart ] Task is already Running. Ignore this request");
                }
                break;
            case "musicvideo" :
                if(storage.isAbleToRun("musicVideoPart")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("musicVideoPart") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithRange(mig_target, this.executeId, provided);
                        }
                    });
                } else {
                    logger.info("[ musicVideoPart ] Task is already Running. Ignore this request");
                }
                break;
            case "chart" :
                if(storage.isAbleToRun("mcdChartBasInfoPart")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("mcdChartBasInfoPart") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithRange(mig_target, this.executeId, provided);
                        }
                    });
                } else {
                    logger.info("[ mcdChartBasInfoPart ] Task is already Running. Ignore this request");
                }

                if(storage.isAbleToRun("mcdChartStatsPart")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("mcdChartStatsPart") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithRange(mig_target, this.executeId, provided);
                        }
                    });
                } else {
                    logger.info("[ mcdChartStatsPart ] Task is already Running. Ignore this request");
                }
                break;
            default:
                logger.info("Could not find appropriate type for migration");
                break;
        }
    }

    private Map<String, Object> mergeReport(Map<String, Object> oldReport, Map<String, Object> newReport){
        try {
            if(oldReport.containsKey(MigrationUtils.JOB_SUCCESS)) {
                int successCnt = (int) oldReport.get(MigrationUtils.JOB_SUCCESS);
                int newSuccessCnt = (newReport.get(MigrationUtils.JOB_SUCCESS) == null ? 0 : (int) newReport.get(MigrationUtils.JOB_SUCCESS));
                oldReport.put(MigrationUtils.JOB_SUCCESS, successCnt + newSuccessCnt);
            }

            if(oldReport.containsKey(MigrationUtils.JOB_SKIPPED)) {
                int skippedCnt = (int) oldReport.get(MigrationUtils.JOB_SKIPPED);
                int newSkippedCnt = (newReport.get(MigrationUtils.JOB_SKIPPED) == null ? 0 : (int) newReport.get(MigrationUtils.JOB_SKIPPED));
                oldReport.put(MigrationUtils.JOB_SKIPPED, skippedCnt + newSkippedCnt);
            }

            if(oldReport.containsKey(MigrationUtils.JOB_DURATION)) {
                long taskDuration = (long) oldReport.get(MigrationUtils.JOB_DURATION);
                long newTaskDuration = (newReport.get(MigrationUtils.JOB_DURATION) == null ? 0 : (long) newReport.get(MigrationUtils.JOB_DURATION));
                oldReport.put(MigrationUtils.JOB_DURATION, taskDuration + newTaskDuration);
            }
        } catch (Exception e) {
            logger.error("Failed to merge migration report :: by ids", e);
        }
        return oldReport;
    }

    public void executeForTempData (String mig_target, String type, List<String> ids) throws Exception {
        switch (type.toLowerCase()) {
            case "album" :
                if(storage.isAbleToRun("tempAlbum")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("tempAlbum") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIds(mig_target, this.executeId, ids);
                        }
                    });
                } else {
                    logger.info("[ albumPart ] Task is already Running. Ignore this request");
                }
                break;
            case "artist" :
                if(storage.isAbleToRun("tempArtist")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("tempArtist") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIds(mig_target, this.executeId, ids);
                        }
                    });
                } else {
                    logger.info("[ artistPart ] Task is already Running. Ignore this request");
                }
                break;
            case "song" :
                if(storage.isAbleToRun("tempSong")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("tempSong") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIds(mig_target, this.executeId, ids);
                        }
                    });
                } else {
                    logger.info("[ songPart ] Task is already Running. Ignore this request");
                }
                break;
            case "musicvideo" :
                if(storage.isAbleToRun("tempMusicVideo")) {
                    taskExecutor.execute(new ParallelDBSyncExecutor("tempMusicVideo") {
                        @Override
                        public void action() throws Exception {
                            this.dbSyncService.executeWithIds(mig_target, this.executeId, ids);
                        }
                    });
                } else {
                    logger.info("[ musicVideoPart ] Task is already Running. Ignore this request");
                }
                break;
            default:
                logger.info("Could not find appropriate type for migration");
                break;
        }
    }


    public void loadTable2Node(String tid, boolean skip) {
        NodeType nt = nodeService.getNodeType(tid);
        String targetTableName = nt.getTableName();
        String query = "SELECT * FROM " + targetTableName + " LIMIT ?,?";
        int start = 0;
        int unit = 1000;
        int loop = 0;
        boolean hasNext = true;
        while(hasNext) {
            start = unit*loop;
            logger.info("START :: " + start + " :: END :: " + unit);
            List<Map<String, Object>> contents = ice2Template.queryForList(query, start, unit);
            if(contents != null && contents.size() < unit) {
                hasNext = false;
            }
            for(Map<String, Object> content : contents) {
                try{
                    content.put("typeId", tid);
                    nodeService.saveNode(content);
                } catch (Exception e) {
                    e.printStackTrace();
                    if(skip){
                        continue;
                    } else {
                        hasNext = false;
                        break;
                    }
                }
            }
            loop++;
        }
    }
}