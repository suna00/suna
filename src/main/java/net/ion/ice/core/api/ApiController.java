package net.ion.ice.core.api;

import net.ion.ice.core.query.SimpleQueryResult;
import net.ion.ice.core.response.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by jaeho on 2017. 7. 3..
 */
@Controller
public class ApiController {
    private static Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    private ApiService apiService ;

    @RequestMapping(value = "/api/{category}/{api}", method = RequestMethod.GET)
    @ResponseBody
    public Object get(NativeWebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
        return api(request, category, api, "GET");
    }

    @RequestMapping(value = "/api/{category}/{api}", method = RequestMethod.POST)
    @ResponseBody
    public Object post(NativeWebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
        return api(request, category, api, "POST");
    }


    @RequestMapping(value = "/api/node/{typeId}/{api}", method = RequestMethod.GET)
    @ResponseBody
    public Object getNode(NativeWebRequest request, @PathVariable String typeId, @PathVariable String api) throws IOException {
        return nodeApi(request, typeId, api, "GET");
    }

    @RequestMapping(value = "/api/node/{typeId}/{api}", method = RequestMethod.POST)
    @ResponseBody
    public Object eventNode(NativeWebRequest request, @PathVariable String typeId, @PathVariable String api) throws IOException {
        return nodeApi(request, typeId, api, "POST");
    }

    @RequestMapping(value = "/api/{category}/{api}.json", method = RequestMethod.GET)
    @ResponseBody
    public Object getJson(NativeWebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
        return api(request, category, api, "GET");
    }

    @RequestMapping(value = "/api/{category}/{api}.json", method = RequestMethod.POST)
    @ResponseBody
    public Object postJson(NativeWebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
        return api(request, category, api, "POST");
    }


    @RequestMapping(value = "/api/node/{typeId}/{api}.json", method = RequestMethod.GET)
    @ResponseBody
    public Object getNodeJson(NativeWebRequest request, @PathVariable String typeId, @PathVariable String api) throws IOException {
        return nodeApi(request, typeId, api, "GET");
    }

    @RequestMapping(value = "/api/node/{typeId}/{api}.json", method = RequestMethod.POST)
    @ResponseBody
    public Object eventNodeJson(NativeWebRequest request, @PathVariable String typeId, @PathVariable String api) throws IOException {
        return nodeApi(request, typeId, api, "POST");
    }



    @RequestMapping(value = "/testApi/{category}/{api}", method = RequestMethod.GET)
    @ResponseBody
    public Object testGet(NativeWebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
        return api(request, category, api, "GET");
    }

//
//    @RequestMapping(value = "/api/{category}/{api}", method = RequestMethod.PUT)
//    @ResponseBody
//    public Object put(WebRequest request, @PathVariable String typeId) throws IOException {
//        return api(request, "PUT");
//    }
//
//    @RequestMapping(value = "/api/{category}/{api}", method = RequestMethod.DELETE)
//    @ResponseBody
//    public Object delete(WebRequest request, @PathVariable String typeId) throws IOException {
//        return api(request, "DELETE");
//    }
//
//    @RequestMapping(value = "/api/{category}/{api}", method = RequestMethod.PATCH)
//    @ResponseBody
//    public Object patch(WebRequest request, @PathVariable String typeId) throws IOException {
//        return api(request, "PATCH");
//    }


    private Object nodeApi(NativeWebRequest request, String typeId, String api, String method) {
        try {
            return apiService.execute(request, "node", api, method, typeId) ;
        } catch (Exception e) {
            e.printStackTrace();
            return JsonResponse.error(e);
        }
    }

    private Object api(NativeWebRequest request, String category, String api, String method) {
        try {
            return apiService.execute(request, category, api, method) ;
        } catch (Exception e) {
            logger.error("api error : " + request.getNativeRequest(HttpServletRequest.class).getRequestURI()) ;
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                if(! (e instanceof ApiException)){
                    e.printStackTrace();
                }
                return JsonResponse.error(e);
            }
        }
    }
}
