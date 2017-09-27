package net.ion.ice.cjmwave.pip;

import com.skb.auth.local.SkbURI;
import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by leehh on 2017. 9. 7.
 */

@Service("videoService")
public class VideoService {
    private static Logger logger = LoggerFactory.getLogger(VideoService.class);
    private String[] authKey = {"smccjenm"};
    private String[] cdnDomainUrl = {"pip-vodhls-mwave.cjenm.skcdn.com", "pip-cliphls-mwave.cjenm.skcdn.com"};
    private String[] buildPids = {"mediaUrl", "contentImgUrl", "subtitlePath"};

    public void getVideoCdnUrl(ExecuteContext context) {
        Map<String, String> returnResult = new HashMap<>();
        String cdmDomainUrlStr;

        Map<String, Object> data = context.getData();
        if (data.isEmpty()) throw new ApiException("400", "Parameter is Empty");

        String contentId = data.get("contentId").toString();
        if (StringUtils.isEmpty(contentId)) {
            throw new ApiException("400", "Required Parameter : contentId");
        }

        Node node = null;
        try {
            node = NodeUtils.getNode("pgmVideo", contentId);
        } catch (NotFoundNodeException e) {
        }
        if (node == null || node.isEmpty()) {
            throw new ApiException("404", "contentId=" + contentId+" data is Null");
        }

        String clipTypeObj = node.getStringValue("clipType");
        String clipType = (StringUtils.contains(clipTypeObj, Node.ID_SEPERATOR)) ? StringUtils.substringAfterLast(clipTypeObj, Node.ID_SEPERATOR) : clipTypeObj;
        String playTime = node.getStringValue("playTime");
        returnResult.put("playTime", playTime);

        if ("1".equals(clipType)) {
            cdmDomainUrlStr = cdnDomainUrl[0];
        } else if ("2".equals(clipType)) {
            cdmDomainUrlStr = cdnDomainUrl[1];
        } else {
            cdmDomainUrlStr = "";
        }

        for (String pid : buildPids) {
            String pidValue = node.getStringValue(pid);
            if (StringUtils.isNotEmpty(pidValue)) {
                SkbURI uri = new SkbURI.Builder().sharedKey(authKey[0]).mediaUrl(pidValue).build();

                try {
                    String uriStri = cdmDomainUrlStr + uri.getURI();
                    logger.info("##PIP " + pid + " CDN URL : " + uriStri);
                    returnResult.put(pid, uriStri);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                } catch (URISyntaxException use) {
                    use.printStackTrace();
                }
            } else {
                returnResult.put(pid, "");
            }
        }

        context.setResult(returnResult);
    }
}
