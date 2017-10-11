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
import java.util.List;
import java.util.Map;

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
            for(Map<String, Object> mapData : csvRowList) {
                nodeService.createNode(mapData, nodeType);
            }
        }
    }

}
