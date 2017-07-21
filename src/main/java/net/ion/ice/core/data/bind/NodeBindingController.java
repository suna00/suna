package net.ion.ice.core.data.bind;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ion.ice.core.data.ResponseUtils;
import net.ion.ice.core.response.JsonErrorResponse;
import net.ion.ice.core.response.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

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
            return JsonResponse.create();
        } catch (Exception e) {
            e.printStackTrace();
            return JsonErrorResponse.error(e);
        }
    }

    @RequestMapping(value = "/data/{typeId}", method = RequestMethod.PUT)
    @ResponseBody
    public Object saveRest(HttpServletRequest request, @PathVariable String typeId) throws IOException {
        return save(request, typeId);
    }

    @RequestMapping(value = "/data/{typeId}/save.json", method = RequestMethod.POST)
    @ResponseBody
    public Object saveJon(HttpServletRequest request, @PathVariable String typeId) {
        return save(request, typeId);
    }

    public Object save(HttpServletRequest request, String typeId) {
        try {
            nodeBindingService.save(request.getParameterMap(), typeId);
            return JsonResponse.create();
        } catch (Exception e) {
            e.printStackTrace();
            return JsonErrorResponse.error(e);
        }
    }

    @RequestMapping(value = "/data/{typeId}/list.json", method = RequestMethod.GET)
    @ResponseBody
    public Object listJson(WebRequest request, @PathVariable String typeId) throws IOException {
        return list(request, typeId);
    }

    private Object list(@PathVariable String typeId) {
        return ResponseUtils.response(nodeBindingService.list(typeId));
    }

    private Object list(WebRequest request, @PathVariable String typeId) {
        return ResponseUtils.response(nodeBindingService.list(typeId, request));
    }


    @RequestMapping(value = "/data/{typeId}/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Object readRest(WebRequest request, @PathVariable String typeId, @PathVariable String id) throws IOException {
        return read(request, typeId, id);
    }

    @RequestMapping(value = "/data/{typeId}/read.json", method = RequestMethod.GET)
    @ResponseBody
    public Object readJson(WebRequest request, @PathVariable String typeId) throws IOException {

        return read(request, typeId);
    }

    private Object read(WebRequest request, String typeId, String id) throws JsonProcessingException {
        try {
            return JsonResponse.create(nodeBindingService.read(typeId, id));
        } catch (EmptyResultDataAccessException e) {
            return JsonErrorResponse.error(e);
        }
    }

    private Object read(WebRequest request, String typeId) throws JsonProcessingException {
        try {

            return JsonResponse.create(nodeBindingService.read(request.getParameterMap(), typeId));
        } catch (EmptyResultDataAccessException e) {
            return JsonErrorResponse.error(e);
        }
    }
}
