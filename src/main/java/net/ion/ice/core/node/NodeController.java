package net.ion.ice.core.node;


import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.SimpleQueryResult;
import net.ion.ice.core.response.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by jaehocho on 2017. 5. 17..
 */
@Controller
public class NodeController {
    private static Logger logger = LoggerFactory.getLogger(NodeController.class);

    @Autowired
    private NodeService nodeService ;

    @RequestMapping(value = "/node/{typeId}", method = RequestMethod.PUT)
    @ResponseBody
    public Object saveRest(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return save(request, typeId);
    }

    @RequestMapping(value = "/node/{typeId}/save.json", method = RequestMethod.POST)
    @ResponseBody
    public Object saveJson(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return save(request, typeId);
    }


    private Object save(HttpServletRequest request, String typeId) {
        if(request instanceof MultipartHttpServletRequest) {
            return JsonResponse.create(nodeService.saveNode(request.getParameterMap(), ((MultipartHttpServletRequest) request).getMultiFileMap(), typeId)) ;
        }
        return JsonResponse.create(nodeService.saveNode(request.getParameterMap(), typeId)) ;
    }


    @RequestMapping(value = "/node/{typeId}/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteRest(WebRequest request, @PathVariable String typeId, @PathVariable String id) throws IOException {
        return JsonResponse.create(nodeService.deleteNode(typeId, id)) ;
    }

    @RequestMapping(value = "/node/{typeId}/delete.json", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteJson(WebRequest request, @PathVariable String typeId) throws IOException {
        return delete(request, typeId);
    }


    private Object delete(WebRequest request, String typeId) {
        return JsonResponse.create(nodeService.deleteNode(request.getParameterMap(), typeId)) ;
    }

    @RequestMapping(value = "/node/{typeId}/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Object readRest(WebRequest request, @PathVariable String typeId, @PathVariable String id) throws IOException {
        return read(request, typeId, id);
    }

    @RequestMapping(value = "/node/{typeId}/read.json", method = RequestMethod.GET)
    @ResponseBody
    public Object readJson(WebRequest request, @PathVariable String typeId) throws IOException {
        return read(request, typeId);
    }


    private Object read(WebRequest request, String typeId, String id) {
        return JsonResponse.create(nodeService.readNode(request.getParameterMap(), typeId, id)) ;
    }

    private Object read(WebRequest request, String typeId) {
        return JsonResponse.create(nodeService.readNode(request.getParameterMap(), typeId)) ;
    }


    @RequestMapping(value = "/node/{typeId}", method = RequestMethod.GET)
    @ResponseBody
    public Object listRest(WebRequest request, @PathVariable String typeId) throws IOException {
        return list(request, typeId);
    }

    @RequestMapping(value = "/node/{typeId}/list.json", method = RequestMethod.GET)
    @ResponseBody
    public Object listJson(WebRequest request, @PathVariable String typeId) throws IOException {
        return list(request, typeId);
    }

    private Object list(WebRequest request, @PathVariable String typeId) {
        try {
            SimpleQueryResult simpleQueryResult = nodeService.getNodeList(typeId, request.getParameterMap()) ;
            return JsonResponse.create(simpleQueryResult) ;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                return JsonResponse.error(e);
            }
        }
    }

    @RequestMapping(value = "/node/tree/{typeId}", method = RequestMethod.GET)
    @ResponseBody
    public Object treeRest(WebRequest request, @PathVariable String typeId) throws IOException {
        return tree(request, typeId);
    }

    @RequestMapping(value = "/node/{typeId}/tree.json", method = RequestMethod.GET)
    @ResponseBody
    public Object treeJson(WebRequest request, @PathVariable String typeId) throws IOException {
        return tree(request, typeId);
    }

    private Object tree(WebRequest request, @PathVariable String typeId) {
        try {
            SimpleQueryResult simpleQueryResult = nodeService.getNodeTree(typeId, request.getParameterMap()) ;
            return JsonResponse.create(simpleQueryResult) ;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                return JsonResponse.error(e);
            }
        }
    }

    @RequestMapping(value = "/node/{typeId}/code.json", method = RequestMethod.GET)
    @ResponseBody
    public Object codeJson(WebRequest request, @PathVariable String typeId) throws IOException {
        return code(request, typeId);
    }

    private Object code(WebRequest request, @PathVariable String typeId) {
        try {
            SimpleQueryResult simpleQueryResult = nodeService.getNodeCode(typeId, request.getParameterMap()) ;
            return JsonResponse.create(simpleQueryResult) ;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                return JsonResponse.error(e);
            }
        }
    }

    @RequestMapping(value = "/node/{typeId}/sequence", method = RequestMethod.GET)
    @ResponseBody
    public Object sequenceRest(WebRequest request, @PathVariable String typeId) throws IOException {
        return sequence(request, typeId);
    }

    private Object sequence(WebRequest request, String typeId) {
        return JsonResponse.createValueResponse(NodeUtils.getSequenceValue(typeId)) ;
    }


    @RequestMapping(value = "/node/query")
    @ResponseBody
    public Object queryeRest(WebRequest request, @RequestParam(value = "query") String query) throws IOException {
        return query(request, query);
    }

    private Object query(WebRequest request, String query) {
        try {
            QueryResult queryResult = nodeService.getQueryResult(query) ;
            return queryResult ;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                return JsonResponse.error(e);
            }
        }
    }
}
