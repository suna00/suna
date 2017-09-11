package net.ion.ice.cjmwave.content;

import net.ion.ice.cjmwave.db.sync.DBSyncController;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.data.DBService;
import net.ion.ice.core.node.NodeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * Created by jeonjinkang on 2017. 9. 4..
 */
@Service("metadataService")
public class MetadataService {
    Logger logger = Logger.getLogger(DBSyncController.class);

    static String metadataSeqQuery = "SELECT getMetadataSeq(?) as metadataId";
    static String dsId = "cjDb";

    @Autowired
    DBService dbService;

    @Autowired
    private NodeService nodeService ;

    public void saveMetadata(ExecuteContext context) throws Exception{
        // Seq 가져오는 소스 작성
        logger.info("getArtistSeqQuery : " + metadataSeqQuery);

        Map<String, Object> data = context.getData();

        JdbcTemplate template = dbService.getJdbcTemplate(dsId);
        Map<String, Object> queryRs = template.queryForMap(metadataSeqQuery, data.get("seqName"));

        logger.info("queryRs.metadataId : " + queryRs.get("metadataId"));

        data.put(data.get("idableId").toString(), queryRs.get("metadataId"));

        logger.info("data.get(\"idableId\").toString() : " + data.get("idableId").toString());

//        ExecuteContext createContext = ExecuteContext.makeContextFromMap(data,data.get("seqName").toString(), EventService.CREATE);
//        createContext.execute();
//
//        context.setResult(createContext.getNode());

         context.setResult(nodeService.executeNode(data, data.get("seqName").toString(), "save"));

    }
}