package net.ion.ice.cjmwave.db.sync;

import net.ion.ice.cjmwave.external.utils.MigrationUtils;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.NodeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by juneyoungoh on 2017. 10. 11..
 */
@Service
public class TemporaryFileService {

    private Logger logger = Logger.getLogger(TemporaryFileService.class);

    @Autowired
    NodeService nodeService;

    @Autowired
    DBService dbService;

    @Autowired
    TemporaryMigrationService temporaryMigrationService;

    private JdbcTemplate ice2template;

    @PostConstruct
    public void init(){
        try {
            ice2template = dbService.getJdbcTemplate("cjDb");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerNodeFromCSV (String nodeType, String fullPath) throws Exception {
        List<Map<String, Object>> csvRowList = MigrationUtils.readFromCSV(fullPath, ",");
        int success = 0;
        int fail = 0;

        if("artist".equals(nodeType)) {
            for(Map<String, Object> mapData : csvRowList) {
                String artistId = String.valueOf(mapData.get("artistId"));
                String query = "SELECT " +
                        "ARTIST_ID as artistId , ARTIST_NM as artistNm , STR_TO_DATE(ARTIST_BIRTH_YMD, '%Y%m%d')  as cretDt " +
                        ", ARTIST_NATIONALITY as bpnac , RIGHT(ARTIST_GENDER, 3) as sex ,  RIGHT(ARTIST_TYPE_CD, 3) as typeCd" +
                        ", STR_TO_DATE(DEBUT_YMD, '%Y%m%d') as debutDt , DEBUT_ALBUM_ID as debutAlbum " +
                        ", ARTIST_INTRO as artistDesc , ARTIST_NM as atvyName, DISPLAY_FLG as showYn " +
                        ", 1 as mnetIfTrtYn FROM MT_ARTIST WHERE ARTIST_ID = ?";

                try{
                    Map<String, Object> rs = ice2template.queryForMap(query, artistId);
                    rs.put("typeId", nodeType);
                    fetchArtistSubInfo(artistId, rs);
                    fetchArtistMultiLanguageInfo(artistId, rs);
                    fetchArtistImg(artistId, rs);

                    logger.info("FULL FETCHED INFORMATION :: " + String.valueOf(rs));
                    nodeService.saveNodeWithException(rs);
                    success++;
                } catch (Exception e) {
                    if(! (e instanceof EmptyResultDataAccessException)) e.printStackTrace();
                    fail++;
                }
            }
            logger.info("#########################################################");
            logger.info("########## CSV READER FOR " + nodeType + " ##############");
            logger.info("success :: " + success);
            logger.info("fail :: " + fail);
            logger.info("#########################################################");
        } else {
            // 앨범 / 뮤직비디오 / 곡
            List<String> ids = new ArrayList<>();
            String key = "";
            switch (nodeType){
                case "album":
                    key = "albumId";
                    break;
                case  "song":
                    key = "songId";
                    break;
                case "musicVideo":
                    key = "mvId";
                    break;
            }
            for(Map<String, Object> mapData : csvRowList) {
                String id = String.valueOf(mapData.get(key));
                ids.add(id);
            }
            temporaryMigrationService.doTemporaryMigration(nodeType, ids);
        }
    }

    private Map<String, Object> fetchArtistSubInfo(String artistId, Map<String, Object> artistBasicInfo) {
        String genreSubQuery = "SELECT GROUP_CONCAT(GENRE_CD) AS genreCd FROM MT_ARTIST_GENRE WHERE ARTIST_ID = ?";
        String keywordSubQuery = "SELECT GROUP_CONCAT(KEYWORD) AS findKywrd FROM MT_ARTIST_KEYWORD WHERE ARTIST_ID = ?";
        String countrySubQuery = "SELECT GROUP_CONCAT(country_cd) AS showCntryCdList FROM MT_ARTIST_COUNTRY WHERE ARTIST_ID = ?";

        try{
            artistBasicInfo.putAll(ice2template.queryForMap(genreSubQuery, artistId));
        } catch (Exception e) {
            logger.error("Failed to add genreCd :: ", e);
        }

        try{
            artistBasicInfo.putAll(ice2template.queryForMap(keywordSubQuery, artistId));
        } catch (Exception e) {
            logger.error("Failed to add findKywrd :: ", e);
        }

        try{
            artistBasicInfo.putAll(ice2template.queryForMap(countrySubQuery, artistId));
        } catch (Exception e) {
            logger.error("Failed to add showCntryCdList :: ", e);
        }
        return  artistBasicInfo;
    }


    private Map<String, Object> fetchArtistMultiLanguageInfo(String artistId, Map<String, Object> artistBasicInfo){
        try {

            String query = "SELECT " +
                    "lang_cd as langCd , artist_nm as artistNm " +
                    ", artist_intro as artistDesc " +
                    ", artist_prev_active_nm as atvyName " +
                    "FROM MT_ARTIST_META ARTIST_META WHERE artist_id= ?";

            List<Map<String, Object>> multiLanguages = ice2template.queryForList(query, artistId);
            for(Map<String, Object> singleLangMap : multiLanguages) {
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
                    String keywordSubQuery = "SELECT GROUP_CONCAT(keyword) AS findKywrd FROM MT_ARTIST_KEYWORD_META WHERE artist_id = ? AND lang_cd = ?";
                    Map<String, Object> rs = new HashMap<>();
                    try{
                        rs = ice2template.queryForMap(keywordSubQuery, artistId, langCd);
                        Iterator<String> iterator = rs.keySet().iterator();
                        while (iterator.hasNext()) {
                            String k1 = iterator.next();
                            Object v1 = rs.get(k1);
                            artistBasicInfo.put(k + "_" + langCd, v1);
                        }
                    } catch (Exception e) {
                        logger.info("Failed to register Artist Multi Language Information CSV", e);
                    }
                    if(!k.equals("langCd")) {
                        artistBasicInfo.put(k + "_" + langCd, v);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to set multi language information, but consider it as normal", e);
        }
        return artistBasicInfo;
    }

    private Map<String, Object> fetchArtistImg(String artistId, Map<String, Object> artistBasicInfo) {
        String mnetFileUrl =
                MigrationUtils.getMnetFileUrl(artistId, "artist", "320");
        artistBasicInfo.put("imgUrl", mnetFileUrl);
        logger.info(mnetFileUrl);
        return artistBasicInfo;
    }
}
