package net.ion.ice.core.node;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ion.ice.core.context.RequestDataHolder;
import net.ion.ice.core.data.bind.NodeBindingService;
import net.ion.ice.core.query.QueryResult;
import net.ion.ice.core.query.SimpleQueryResult;
import net.ion.ice.core.response.JsonResponse;
import net.ion.ice.core.session.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 5. 17..
 */
@Controller
public class NodeController {
    private static Logger logger = LoggerFactory.getLogger(NodeController.class);

    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeBindingService nodeBindingService;

    @Autowired
    private SessionService sessionService;


    @RequestMapping(value = "/node/{typeId}", method = RequestMethod.PUT)
    @ResponseBody
    public Object saveRest(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return execute(request, typeId, "save");
    }

    @RequestMapping(value = "/node/{typeId}/save.json", method = RequestMethod.POST)
    @ResponseBody
    public Object saveJson(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return execute(request, typeId, "save");
    }

    @RequestMapping(value = "/node/{typeId}/create.json", method = RequestMethod.POST)
    @ResponseBody
    public Object createJson(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return execute(request, typeId, "create");
    }

    @RequestMapping(value = "/node/{typeId}/update.json", method = RequestMethod.POST)
    @ResponseBody
    public Object updateJson(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return execute(request, typeId, "update");
    }

    @RequestMapping(value = "/node/{typeId}/{event}.json", method = RequestMethod.POST)
    @ResponseBody
    public Object updateJson(HttpServletRequest request, @PathVariable String typeId, @PathVariable String event) throws IOException {
        return execute(request, typeId, event);
    }


    private Object execute(HttpServletRequest request, String typeId, String event) {
        if(request instanceof MultipartHttpServletRequest) {
            return nodeService.executeNode(request.getParameterMap(), ((MultipartHttpServletRequest) request).getMultiFileMap(), typeId, event) ;
        }
        return nodeService.executeNode(request.getParameterMap(), null, typeId, event) ;
    }


    @RequestMapping(value = "/node/{typeId}/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public Object deleteRest(HttpServletRequest request, @PathVariable String typeId, @PathVariable String id) throws IOException {
        return delete(request, typeId, id);
    }

    @RequestMapping(value = "/node/{typeId}/delete.json", method = RequestMethod.POST)
    @ResponseBody
    public Object deleteJson(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return delete(request, typeId, null);
    }


    private Object delete(HttpServletRequest request, String typeId, String id) {
        return nodeService.deleteNode(request.getParameterMap(), typeId, id) ;
    }

    @RequestMapping(value = "/node/{typeId}/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Object readRest(HttpServletRequest request, @PathVariable String typeId, @PathVariable String id) throws IOException {
        return read(request, typeId, id);
    }

    @RequestMapping(value = "/node/{typeId}/read.json", method = RequestMethod.GET)
    @ResponseBody
    public Object readJson(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return read(request, typeId);
    }

    private Object read(HttpServletRequest request, String typeId, String id) throws JsonProcessingException {
        return nodeService.readNode(request.getParameterMap(), typeId, id) ;
    }

    private Object read(HttpServletRequest request, String typeId) throws JsonProcessingException {
        return nodeService.readNode(request.getParameterMap(), typeId) ;
    }


    @RequestMapping(value = "/node/{typeId}", method = RequestMethod.GET)
    @ResponseBody
    public Object listRest(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return list(request, typeId);
    }

    @RequestMapping(value = "/node/{typeId}/list.json", method = RequestMethod.GET)
    @ResponseBody
    public Object listJson(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return list(request, typeId);
    }

    private Object list(HttpServletRequest request, @PathVariable String typeId) {
        RequestDataHolder.setRequestDataValue("request", request);
        RequestDataHolder.setRequestDataValue("session", getSession(request));
        try {
            QueryResult queryResult = nodeService.getQueryResult(typeId, request.getParameterMap()) ;
            return queryResult ;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                return JsonResponse.error(e);
            }
        }finally{
            RequestDataHolder.clearRequestData();
        }
    }

    @RequestMapping(value = "/node/tree/{typeId}", method = RequestMethod.GET)
    @ResponseBody
    public Object treeRest(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return tree(request, typeId);
    }

    @RequestMapping(value = "/node/{typeId}/tree.json", method = RequestMethod.GET)
    @ResponseBody
    public Object treeJson(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return tree(request, typeId);
    }

    private Object tree(HttpServletRequest request, @PathVariable String typeId) {
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
    public Object codeJson(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return code(request, typeId);
    }

    private Object code(HttpServletRequest request, @PathVariable String typeId) {
        try {
            QueryResult queryResult = nodeService.getReferenceQueryResult(typeId, request.getParameterMap()) ;
            return queryResult ;
//            SimpleQueryResult simpleQueryResult = nodeService.getNodeCode(typeId, request.getParameterMap()) ;
//            return JsonResponse.create(simpleQueryResult) ;
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
    public Object sequenceRest(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return sequence(request, typeId);
    }

    private Object sequence(HttpServletRequest request, String typeId) {
        return JsonResponse.createValueResponse(NodeUtils.getSequenceValue(typeId)) ;
    }


    @RequestMapping(value = "/node/query", method = RequestMethod.POST)
    @ResponseBody
    public Object queryeRest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return query(request, response);
    }

    private Object query(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, String[]> parameterMap = request.getParameterMap() ;
            QueryResult queryResult = nodeService.getQueryResult(request, response, parameterMap) ;
            return queryResult ;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                return JsonResponse.error(e);
            }
        }
    }

    @RequestMapping(value = "/node/event")
    @ResponseBody
    public Object eventRest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try{
            if (request instanceof MultipartHttpServletRequest) {
                return nodeService.executeResult(request, response, request.getParameterMap(), ((MultipartHttpServletRequest) request).getMultiFileMap());
            }
            return nodeService.executeResult(request, response, request.getParameterMap(), null);
        } catch (Exception e) {
            logger.error(e.getMessage());
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                return JsonResponse.error(e);
            }
        }
    }


    @RequestMapping(value = "/node/{typeId}/event/{event}")
    @ResponseBody
    public Object eventJson(HttpServletRequest request, @PathVariable String typeId, @PathVariable String event) throws IOException {
        return execute(request, typeId, event);
    }

    public Map<String, Object> getSession(HttpServletRequest request){
        try {
            return sessionService.getSession(request);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}

