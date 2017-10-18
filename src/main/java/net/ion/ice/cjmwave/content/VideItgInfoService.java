package net.ion.ice.cjmwave.content;

import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jeonjinkang on 2017. 9. 14..
 */
@Service("videItgInfoService")
public class VideItgInfoService {
    @Autowired
    private NodeService nodeService ;

    Logger logger = Logger.getLogger(VideItgInfoService.class);

    public void videItgInfoMusicVideoSave(ExecuteContext context){
        try {
            Node node = context.getNode();

            Map<String, Object> saveData = new HashMap<String, Object>();

            saveData.put("videoId", "musicVideo::" + node.getId());
            saveData.put("title", node.get("musicVideoNm"));
            saveData.put("imgUrl", node.get("imgUrl"));
            saveData.put("playTime", node.get("playTime"));
            saveData.put("videoDivCd", "musicVideo");
            saveData.put("searchKeyword", node.get("findKywrd"));
            saveData.put("contsMetaCtgryId", node.get("contsMetaCtgryId"));
            saveData.put("artistId", node.get("artistId"));
            saveData.put("rcmdContsYn", node.get("rcmdContsYn"));
            saveData.put("synopsis", node.get("musicVideoDesc"));
            saveData.put("seoUrl", node.get("seoUrl"));
            saveData.put("hitNum", node.get("hitNum"));
            saveData.put("risingHitNum", node.get("risingHitNum"));
            saveData.put("hotHitNum", node.get("hotHitNum"));
            saveData.put("mkeDay", node.get("created"));

            logger.info("videItgInfoMusicVideoSave!!! : " + saveData);

            nodeService.executeNode(saveData, "videItgInfo", "save");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void videItgInfoPgmVideoSave(ExecuteContext context){
        try {

            Node node = context.getNode();

            Map<String, Object> saveData = new HashMap<String, Object>();

            saveData.put("videoId", "pgmVideo::" + node.getId());
            saveData.put("title", node.get("title"));
            saveData.put("imgUrl", node.get("contentImgUrl"));
            saveData.put("playTime", node.get("playTime"));
            saveData.put("videoDivCd", "pgmVideo");
            saveData.put("searchKeyword", node.get("searchKeyword"));
            saveData.put("contsMetaCtgryId", node.get("contsMetaCtgryId"));
            saveData.put("artistId", node.get("prsnNo"));
            saveData.put("rcmdContsYn", node.get("rcmdContsYn"));
            saveData.put("contentTitle", node.get("contentTitle"));
            saveData.put("synopsis", node.get("synopsis"));
            saveData.put("prsnName", node.get("prsnName"));
            saveData.put("seoUrl", node.get("seoUrl"));
            saveData.put("hitNum", node.get("hitNum"));
            saveData.put("risingHitNum", node.get("risingHitNum"));
            saveData.put("hotHitNum", node.get("hotHitNum"));
            saveData.put("mkeDay", node.get("mkeDay"));

            logger.info("videItgInfo pgm save : " + saveData);
            nodeService.executeNode(saveData, "videItgInfo", "save");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
