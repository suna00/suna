package net.ion.ice.cjmwave.content;

import net.ion.ice.cjmwave.db.sync.DBSyncController;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.DBService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by jeonjinkang on 2017. 9. 4..
 */
@Service("artistService")
public class ArtistService{
    Logger logger = Logger.getLogger(DBSyncController.class);

    static String artistSeqQuery = "SELECT * FROM metadataSeqTblc WHERE seqName = artist";
    static String dsId = "artist";

    @Autowired
    DBService dbService;

    public void getArtistSeq(ExecuteContext executeContext){
        // Seq 가져오는 소스 작성
        logger.info("getArtistSeqQuery : " + artistSeqQuery);

        try {
            JdbcTemplate template = dbService.getJdbcTemplate(dsId);
            Map<String, Object> queryRs = template.queryForMap(artistSeqQuery);
            logger.info("queryRs.seqName" + queryRs.get("seqName"));
            logger.info("queryRs.seqCurval" + queryRs.get("seqCurval"));
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}