package net.ion.ice.core.node;


import net.ion.ice.core.cluster.ClusterUtils;
import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.response.JsonResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class NodeHelperController {
    private Logger logger = LoggerFactory.getLogger(NodeHelperController.class);

    @Autowired
    private NodeHelperService nodeHelperService ;

    @Autowired
    private InfinispanRepositoryService infinispanRepositoryService ;


    @RequestMapping(value = "/helper/reloadSchema.json", method = RequestMethod.GET)
    @ResponseBody
    public Object saveJson(HttpServletRequest request, @RequestParam String filePath)  {
        try {
            logger.info("reload schema");
            nodeHelperService.reloadSchema(filePath);
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;

        }
        return JsonResponse.create() ;
    }

    @RequestMapping(value = "/helper/read", method = RequestMethod.POST)
    @ResponseBody
    public Object readNode(HttpServletRequest request, @RequestParam String typeId, @RequestParam String id)  {
        try {
            logger.info("node read : {}, {}", typeId, id);
            return infinispanRepositoryService.read(typeId, id).toMap();
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;
        }
    }

    @RequestMapping(value = "/helper/list", method = RequestMethod.POST)
    @ResponseBody
    public Object listNode(HttpServletRequest request, @RequestParam String typeId, @RequestParam String query)  {
        try {
            logger.info("node list : {}, {}", typeId, query);
            QueryContext queryContext = QueryContext.createQueryContextFromText(query, NodeUtils.getNodeType(typeId), null) ;
            return infinispanRepositoryService.getSyncQueryList(typeId, queryContext);
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;
        }
    }

    @RequestMapping(value = "/helper/rebuild", method = RequestMethod.GET)
    @ResponseBody
    public Object rebuildIndex(HttpServletRequest request, @RequestParam String typeId)  {
        try {
            long start = System.currentTimeMillis() ;
            logger.info("index rebuild start : {}", typeId);
            infinispanRepositoryService.rebuild(typeId) ;
            logger.info("index rebuild end : {} {}ms", typeId, System.currentTimeMillis() - start);
            return JsonResponse.create() ;
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;
        }
    }

    @RequestMapping(value = "/helper/cache", method = RequestMethod.GET)
    @ResponseBody
    public Object cache(HttpServletRequest request, @RequestParam String typeId, @RequestParam String id, @RequestParam String server){
        try {
            logger.info("Cache Sync : {}.{} ", typeId, id);
            Map<String, Object> data = ClusterUtils.callNode(server, typeId, id) ;
            if(data != null) {
                Node node = new Node(data);
                NodeUtils.getInfinispanService().cacheNode(node);
                return node ;
            }
        }catch (Exception e){
            e.printStackTrace();
            return JsonResponse.error(e) ;
        }
        return null ;
    }

}
