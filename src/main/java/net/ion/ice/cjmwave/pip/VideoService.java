package net.ion.ice.cjmwave.pip;

import com.skb.auth.local.SkbURI;
import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created by leehh on 2017. 9. 7.
 */

@Service("videoService")
public class VideoService {
    private String[] authKey = {"smccjenm"};
    private String[] cdnDomainUrl = {"pip-vodhls-mwave.cjenm.skcdn.com","pip-cliphls-mwave.cjenm.skcdn.com"};

    @Autowired
    private NodeService nodeService;

    public String getVideoCdnUrl(ExecuteContext context){
        String retrunUrl = "";
        String cdmDomainUrlStr = "";
        Map<String, Object> data = context.getData();

        if (data == null || StringUtils.isEmpty(data.get("contentId").toString())) {
            throw new IceRuntimeException("contentId Parameter is Null") ;
        }
        String contentIdStr = data.get("contentId").toString();
        Node node = nodeService.getNode("pgmVideo", contentIdStr);
        if (node == null) {
            throw new IceRuntimeException("Node is Null : contentId="+contentIdStr) ;
        }

        String clipType = node.get("clipType").toString();
        String mediaUrl = node.get("mediaUrl").toString();
        if("1".equals(clipType)){
            cdmDomainUrlStr = cdnDomainUrl[0];
        }else if("2".equals(clipType)){
            cdmDomainUrlStr = cdnDomainUrl[1];
        }else{
            cdmDomainUrlStr = "";
        }

        SkbURI uri = new SkbURI.Builder().sharedKey(authKey[0]).mediaUrl(mediaUrl).build();

        try {
            retrunUrl = cdmDomainUrlStr+uri.getURI();
            System.out.println("##PIP CDN URL : "+retrunUrl);
        }catch (IllegalArgumentException iae){
            iae.printStackTrace();
        }catch (URISyntaxException use){
            use.printStackTrace();
        }

        return retrunUrl;
    }
}
