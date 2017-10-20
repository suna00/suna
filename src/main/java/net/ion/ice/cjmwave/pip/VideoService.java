package net.ion.ice.cjmwave.pip;

import com.skb.auth.local.SkbURI;
import net.ion.ice.ApplicationContextManager;
import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.event.EventService;
import net.ion.ice.core.file.FileValue;
import net.ion.ice.core.infinispan.NotFoundNodeException;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import net.ion.ice.core.node.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by leehh on 2017. 9. 7.
 */

@Service("videoService")
public class VideoService {
    private static Logger logger = LoggerFactory.getLogger(VideoService.class);
    private String[] authKey = {"smccjenm"};
    private String[] cdnDomainUrl = {"http://pip-vodhls-mwave.cjenm.skcdn.com", "http://pip-cliphls-mwave.cjenm.skcdn.com"};
    private String[] buildPids = {"mediaUrl", "subtitlePath"};//{"mediaUrl", "contentImgUrl", "subtitlePath"};

    public void getVideoCdnUrl(ExecuteContext context) {
        Map<String, String> returnResult = new HashMap<>();

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
            throw new ApiException("404", "contentId=" + contentId + " data is Null");
        }

        String clipTypeObj = node.getStringValue("clipType");
        String clipType = (StringUtils.contains(clipTypeObj, Node.ID_SEPERATOR)) ? StringUtils.substringAfterLast(clipTypeObj, Node.ID_SEPERATOR) : clipTypeObj;
        String playTime = node.getStringValue("playTime");
        returnResult.put("playTime", playTime);
        String fileUrlFormat = ApplicationContextManager.getContext().getEnvironment().getProperty("image.s3PrefixUrl");
        if(fileUrlFormat == null || StringUtils.isEmpty(fileUrlFormat)){
            fileUrlFormat= ApplicationContextManager.getContext().getEnvironment().getProperty("image.prefixUrl");
        }
        String fullUrl = "";
        if (node.get("contentImgUrl") != null) {
            if(node.get("contentImgUrl") instanceof FileValue) {
                fullUrl = fileUrlFormat + ((FileValue) node.get("contentImgUrl")).getStorePath();
            }
        }
        returnResult.put("contentImgUrl", fullUrl);//노드에서 가져오도록 추가

        String cdmDomainUrlStr = getCdnDomain(clipType);

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

    public void pipImgSave(ExecuteContext context) {
        Node pgmVideoNode = context.getNode();
        if (pgmVideoNode == null || pgmVideoNode.isEmpty()) {
            throw new IceRuntimeException("pgmVideoNode is null");
        }

        String contentImgPip = pgmVideoNode.getStringValue("contentImgPip");
        String clipTypeObj = pgmVideoNode.getStringValue("clipType");
        String clipType = (StringUtils.contains(clipTypeObj, Node.ID_SEPERATOR)) ? StringUtils.substringAfterLast(clipTypeObj, Node.ID_SEPERATOR) : clipTypeObj;
//        String imgPathChgYn = pgmVideoNode.getStringValue("imgPathChgYn");
        String pipImgUrl = null;
        if (StringUtils.isEmpty(clipType) || StringUtils.isEmpty(contentImgPip)) {
            throw new IceRuntimeException("pipImgSave required value is null : clipType=" + clipType + ", contentImgPip=" + contentImgPip);
        } else {
            pipImgUrl = getImgUrl(clipType, contentImgPip);
            System.out.println("pipImgUrl:" + pipImgUrl);
        }

        PropertyType contentImgUrlPt = context.getNodeType().getPropertyType("contentImgUrl");
        FileValue contentImgUrlFile = NodeUtils.getFileService().saveResourceFile(contentImgUrlPt, pgmVideoNode.getId(), pipImgUrl);

        if (contentImgUrlFile != null) {
            Map<String, Object> updateData = new LinkedHashMap<>();
            updateData.put("contentId", pgmVideoNode.getId());
            updateData.put("contentImgUrl", contentImgUrlFile);
            updateData.put("imgPathChgYn", true);
            Node result = (Node) NodeUtils.getNodeService().executeNode(updateData, "pgmVideo", EventService.SAVE);
            context.setResult(result);
        } else {
            System.out.println("contentImgUrlFile is null");
        }
    }

    public String getImgUrl(String clipType, String imgUrl) {
        String returnUrl = null;
        if (StringUtils.isEmpty(imgUrl)) {
            throw new IceRuntimeException("Required Parameter : imgUrl");
        }

        String cdmDomainUrlStr = getCdnDomain(clipType);

        SkbURI uri = new SkbURI.Builder().sharedKey(authKey[0]).mediaUrl(imgUrl).build();

        try {
            returnUrl = cdmDomainUrlStr + uri.getURI();
            logger.info("##PIP getImgUrl CDN URL : " + returnUrl);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        } catch (URISyntaxException use) {
            use.printStackTrace();
        }
        return returnUrl;
    }

    private String getCdnDomain(String clipType) {
        String cdmDomainUrlStr;
        if ("1".equals(clipType)) {
            cdmDomainUrlStr = cdnDomainUrl[0];
        } else if ("2".equals(clipType)) {
            cdmDomainUrlStr = cdnDomainUrl[1];
        } else {
            cdmDomainUrlStr = "";
        }
        return cdmDomainUrlStr;
    }
}
