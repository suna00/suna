package net.ion.ice.core.node;


import net.ion.ice.core.context.QueryContext;
import net.ion.ice.core.infinispan.InfinispanRepositoryService;
import net.ion.ice.core.response.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

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
            return infinispanRepositoryService.getSyncQueryList(queryContext);
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;
        }
    }

    @RequestMapping(value = "/helper/syncList", method = RequestMethod.POST)
    @ResponseBody
    public Object listNode(HttpServletRequest request, @RequestParam String typeId, @RequestParam String query, @RequestParam String server)  {
        try {
            logger.info("node sync list : {}, {}", typeId, query);
            nodeHelperService.syncNodeList(NodeUtils.getNodeType(typeId), query, server);
            JsonResponse.create() ;
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;
        }
        return null ;
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

    @RequestMapping(value = "/helper/syncDb", method = RequestMethod.POST)
    @ResponseBody
    public Object syncDb(HttpServletRequest request, @RequestParam String typeId, @RequestParam String query, @RequestParam String ds)  {
        try {
            logger.info("db node sync : {}, {}", typeId, query);
            return nodeHelperService.syncNodeQuery(typeId, query, ds);
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;
        }
    }

    @RequestMapping(value = "/helper/syncBinding", method = RequestMethod.GET)
    @ResponseBody
    public Object syncBinding(HttpServletRequest request, @RequestParam String typeId, @RequestParam(required = false) String id, @RequestParam(required = false) Integer limit, @RequestParam(required = false) Integer roofCount)  {
        try {
            logger.info("db node sync : {}, {}", typeId, id);
            return nodeHelperService.syncNodeBinding(typeId, id, limit, roofCount);
        }catch(Exception e){
            logger.error(e.getMessage(), e);
            return JsonResponse.error(e) ;
        }
    }
}
