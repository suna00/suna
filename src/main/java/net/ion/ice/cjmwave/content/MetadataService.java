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
    Logger logger = Logger.getLogger(MetadataService.class);

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

    public void getRcmdContsYnCount(ExecuteContext context) throws Exception{
        logger.info("getRcmdContsYnCount");
        String nodeId = context.getNodeType().getTypeId();

        String rcmdContsYnCountQuery = "SELECT COUNT(showYn) as rcmdContsYnCnt FROM " + nodeId + " WHERE rcmdContsYn = 1";

        logger.info("rcmdContsYnCountQuery : " + rcmdContsYnCountQuery);

        Map<String, Object> data = context.getData();

        logger.info("nodeTypeId : " + context.getNodeType().getTypeId());

        JdbcTemplate template = dbService.getJdbcTemplate(dsId);
        // Map<String, Object> queryRs = template.queryForMap(rcmdContsYnCountQuery);
        context.setResult(template.queryForMap(rcmdContsYnCountQuery));
    }

    public void changeRcmdContsYn(ExecuteContext context) throws Exception{
        String rcmdContsYnCountQuery = "SELECT COUNT(showYn) as rcmdContsYnCnt FROM " + context.getNodeType().getTypeId() + " WHERE rcmdContsYn = 1";

        logger.info("rcmdContsYnCountQuery : " + rcmdContsYnCountQuery);

        Map<String, Object> data = context.getData();

        logger.info("nodeTypeId : " + context.getNodeType().getTypeId());
        String nodeId = context.getNodeType().getTypeId();

        JdbcTemplate template;
        Map<String, Object> queryRs = null;

        int rcmdContsYnCnt = 0;

        // 상태값이 추천에서 비추천으로 바꿀 경우 추천 수를 체크하지 않고 그대로 save하면 됨.
        // 추천일 경우 해당 노드의 추천 콘텐츠 수를 구해서 3개가 넘을 경우 추천수를 반환함.
        if(data.get("rcmdContsYn").toString().equals("true")){
            template = dbService.getJdbcTemplate(dsId);
            queryRs = template.queryForMap(rcmdContsYnCountQuery);
            logger.info("queryRs.rcmdContsYnCnt : " + queryRs.get("rcmdContsYnCnt"));

            rcmdContsYnCnt = Integer.parseInt(queryRs.get("rcmdContsYnCnt").toString());
            if(rcmdContsYnCnt > 3){
                logger.info("rcmdContsYnCnt > 3");
                context.setResult(queryRs);
            }else{
                logger.info("rcmdContsYnCnt < 3");
                context.setResult(nodeService.executeNode(data, nodeId, "save"));
            }
        }else{
            context.setResult(nodeService.executeNode(data, nodeId, "save"));
        }
    }
}