package net.ion.ice.cjmwave.pip;

import com.skb.auth.local.SkbURI;
import net.ion.ice.IceRuntimeException;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeUtils;
import org.apache.commons.lang3.StringUtils;
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

    public void getVideoCdnUrl(ExecuteContext context){
        Map<String, Object> returnResult = null;
        String cdmDomainUrlStr = "";
        Map<String, Object> data = context.getData();
        String contentId = data.get("contentId").toString();

        if (data == null || StringUtils.isEmpty(contentId)) {
            throw new IceRuntimeException("contentId Parameter is Null") ;
        }

        Node node = NodeUtils.getNode("pgmVideo", contentId);
        if (node == null) {
            throw new IceRuntimeException("Node is Null : contentId="+contentId) ;
        }

        String clipType = node.getStringValue("clipType");
        String mediaUrl = node.getStringValue("mediaUrl");
        if("1".equals(clipType)){
            cdmDomainUrlStr = cdnDomainUrl[0];
        }else if("2".equals(clipType)){
            cdmDomainUrlStr = cdnDomainUrl[1];
        }else{
            cdmDomainUrlStr = "";
        }

        SkbURI uri = new SkbURI.Builder().sharedKey(authKey[0]).mediaUrl(mediaUrl).build();

        try {
            returnResult.put("mediaUrl",cdmDomainUrlStr+uri.getURI());
            System.out.println("##PIP CDN URL : "+cdmDomainUrlStr+uri.getURI());
        }catch (IllegalArgumentException iae){
            iae.printStackTrace();
        }catch (URISyntaxException use){
            use.printStackTrace();
        }

        context.setResult(returnResult);
    }
}
