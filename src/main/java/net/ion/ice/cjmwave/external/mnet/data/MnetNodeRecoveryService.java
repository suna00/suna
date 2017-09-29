package net.ion.ice.cjmwave.external.mnet.data;

import net.ion.ice.core.data.DBService;
import net.ion.ice.core.json.JsonUtils;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * Created by juneyoungoh on 2017. 9. 28..
 * Connected to NODE_CREATION_FAIL Table
 */
@Service
public class MnetNodeRecoveryService {


    @Autowired
    DBService dbService;

    @Autowired
    NodeService nodeService;

    private JdbcTemplate ice2template;
    private Logger logger = Logger.getLogger(MnetNodeRecoveryService.class);

    @PostConstruct
    public void init() {
        try{
            ice2template = dbService.getJdbcTemplate("cjDb");
        } catch (Exception e) {
            logger.error("Can not initialize MnetNodeRecoveryService. Disable recovery function :-(");
        }
    }

    @Transactional
    public void recoverNodeBySeq(String seq) throws Exception {
        logger.info("Recover NODE_CREATION_FAIL seq [ " + seq + " ]");
        String q = "SELECT * FROM NODE_CREATION_FAIL WHERE isFixed=0 AND SEQ = ?";
        Map<String, Object> qRs = ice2template.queryForMap(q, seq);
        String jsonValue = String.valueOf(qRs.get("jsonValue"));
        Node node = nodeService.saveNode(JsonUtils.parsingJsonToMap(jsonValue));
        if(node == null) {
            throw new Exception("Result of saveNode is null");
        } else {
            String upQ = "UPDATE NODE_CREATION_FAIL SET isFixed=1 WHERE SEQ = ?";
            ice2template.update(upQ, seq);
        }
    }


    @Transactional
    public void recoverNodeByException(String exceptionName) throws Exception {
        int success = 0, skipped = 0;
        logger.info("Recover NODE_CREATION_FAIL where exception contains [ " + exceptionName + " ]");
        String q = "SELECT * FROM NODE_CREATION_FAIL WHERE isFixed=0 AND exception LIKE ?";
        List<Map<String, Object>> qRs = ice2template.queryForList(q, "%" + exceptionName +"%");
        for(Map<String, Object> qRSSingle : qRs) {
            try{
                String jsonValue = String.valueOf(qRSSingle.get("jsonValue"));
                Node node = nodeService.saveNode(JsonUtils.parsingJsonToMap(jsonValue));
                if(node == null) {
                    skipped++;
                } else {
                    String seq = String.valueOf(qRSSingle.get("seq"));
                    String upQ = "UPDATE NODE_CREATION_FAIL SET isFixed=1 WHERE SEQ = ?";
                    ice2template.update(upQ, seq);
                    success++;
                }
            } catch (Exception e) {
                logger.error("Error for single data", e);
                skipped++;
            }

        }
        logger.info("RECOVER BY EXCEPTION Execution result :: success :: " + success + " :: skipped :: " + skipped);
    }


    public void recoverNodeAll() throws Exception {
        int success = 0, skipped = 0;
        logger.info("Recover All NODE_CREATION_FAIL");
        String q = "SELECT * FROM NODE_CREATION_FAIL WHERE isFixed=0";
        List<Map<String, Object>> qRs = ice2template.queryForList(q);
        for(Map<String, Object> qRSSingle : qRs) {
            try{
                String jsonValue = String.valueOf(qRSSingle.get("jsonValue"));
                Node node = nodeService.saveNode(JsonUtils.parsingJsonToMap(jsonValue));
                if(node == null) {
                    skipped++;
                } else {
                    String seq = String.valueOf(qRSSingle.get("seq"));
                    String upQ = "UPDATE NODE_CREATION_FAIL SET isFixed=1 WHERE SEQ = ?";
                    ice2template.update(upQ, seq);
                    success++;
                }

            } catch (Exception e) {
                logger.error("Error for single data", e);
                skipped++;
            }
        }
        logger.info("RECOVER ALL Execution result :: success :: " + success + " :: skipped :: " + skipped);
    }
}