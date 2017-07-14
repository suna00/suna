package net.ion.ice.core.data.bind;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.core.data.ResponseUtils;
import net.ion.ice.core.query.SimpleQueryResult;
import net.ion.ice.core.response.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by seonwoong on 2017. 7. 4..
 */
@Controller
public class NodeBindingController {
    private static Logger logger = LoggerFactory.getLogger(NodeBindingController.class);

    @Autowired
    NodeBindingService nodeBindingService;


    @RequestMapping(value = "/data/create/{tid}", method = RequestMethod.GET)
    @ResponseBody
    public Object createTable(WebRequest request, HttpServletResponse response, @PathVariable String tid) throws IOException {
        try {
            nodeBindingService.createTable(tid, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping(value = "/data/{typeId}/list.json", method = RequestMethod.GET)
    @ResponseBody
    public Object listJson(WebRequest request, @PathVariable String typeId) throws IOException {
        return list(request, typeId);
    }

    private Object list(WebRequest request, @PathVariable String typeId) {
        return ResponseUtils.response(nodeBindingService.list(typeId));
    }


    /**
     * 이하 NodeController 로 이동
     */
//    @RequestMapping(value = "/data/{typeId}/{id}", method = RequestMethod.GET)
//    @ResponseBody
//    public Object readRest(WebRequest request, @PathVariable String typeId, @PathVariable String id) throws IOException {
//        return read(request, typeId, id);
//    }
//
//    @RequestMapping(value = "/data/{typeId}/read.json", method = RequestMethod.GET)
//    @ResponseBody
//    public Object readJson(WebRequest request, @PathVariable String typeId) throws IOException {
//        return read(request, typeId);
//    }
//
//    private Object read(WebRequest request, String typeId, String id) throws JsonProcessingException {
//        return ResponseUtils.response(nodeBindingService.read(typeId, id));
//    }
//
//    private Object read(WebRequest request, String typeId) {
//        return JsonResponse.create(nodeService.readNode(request.getParameterMap(), typeId)) ;
//    }

}
