package net.ion.ice.cjmwave.content;

import net.ion.ice.core.context.ExecuteContext;
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
        logger.info("videItgInfoPgmVideoSave!!!");
        Map<String, Object> contextData = context.getData();

        Map<String, Object> saveData = new HashMap<String, Object>();

        saveData.put("videoId", "musicVideo::" + contextData.get("musicVideoId"));
        saveData.put("title", contextData.get("musicVideoNm"));
        saveData.put("imgUrl", contextData.get("imgUrl"));
        saveData.put("playTime", contextData.get("playTime"));
        saveData.put("videoDivCd", "musicVideo");
        saveData.put("searchKeyword", contextData.get("findKywrd"));
        saveData.put("contsMetaCtgryId", contextData.get("contsMetaCtgryId"));
        saveData.put("artistId", contextData.get("artistId"));
        saveData.put("rcmdContsYn", contextData.get("rcmdContsYn"));
        saveData.put("synopsis", contextData.get("musicVideoDesc"));
        saveData.put("seoUrl", contextData.get("seoUrl"));

        nodeService.executeNode(saveData, "videItgInfo".toString(), "save");
    }

    public void videItgInfoPgmVideoSave(ExecuteContext context){
        logger.info("videItgInfoPgmVideoSave!!!");
        Map<String, Object> contextData = context.getData();

        Map<String, Object> saveData = new HashMap<String, Object>();

        saveData.put("videoId", "pgmVideo::" + contextData.get("contentId"));
        saveData.put("title", contextData.get("title"));
        saveData.put("imgUrl", contextData.get("contentImgUrl"));
        saveData.put("playTime", contextData.get("playTime"));
        saveData.put("videoDivCd", "pgmVideo");
        saveData.put("searchKeyword", contextData.get("searchKeyword"));
        saveData.put("contsMetaCtgryId", contextData.get("contsMetaCtgryId"));
        saveData.put("artistId", contextData.get("prsnNo"));
        saveData.put("rcmdContsYn", contextData.get("rcmdContsYn"));
        saveData.put("contentTitle", contextData.get("contentTitle"));
        saveData.put("synopsis", contextData.get("synopsis"));
        saveData.put("prsnName", contextData.get("prsnName"));
        saveData.put("seoUrl", contextData.get("seoUrl"));

        nodeService.executeNode(saveData, "videItgInfo".toString(), "save");
    }
}
