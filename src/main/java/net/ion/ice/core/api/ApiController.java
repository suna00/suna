package net.ion.ice.core.api;

import net.ion.ice.core.query.SimpleQueryResult;
import net.ion.ice.core.response.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;

/**
 * Created by jaeho on 2017. 7. 3..
 */
@Controller
public class ApiController {

    @Autowired
    private ApiService apiService ;

    @RequestMapping(value = "/api/{category}/{api}", method = RequestMethod.GET)
    @ResponseBody
    public Object get(WebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
        return api(request, category, api, "GET");
    }

    @RequestMapping(value = "/api/{category}/{api}", method = RequestMethod.POST)
    @ResponseBody
    public Object post(WebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
        return api(request, category, api, "POST");
    }

    @RequestMapping(value = "/api/{category}/{api}.json", method = RequestMethod.GET)
    @ResponseBody
    public Object getJson(WebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
        return api(request, category, api, "GET");
    }

    @RequestMapping(value = "/api/{category}/{api}.json", method = RequestMethod.POST)
    @ResponseBody
    public Object postJson(WebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
        return api(request, category, api, "POST");
    }

    @RequestMapping(value = "/testApi/{category}/{api}", method = RequestMethod.GET)
    @ResponseBody
    public Object testGet(WebRequest request, @PathVariable String category, @PathVariable String api) throws IOException {
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




    private Object api(WebRequest request, String category, String api, String method) {
        try {
            return apiService.execute(request, category, api, method) ;
        } catch (Exception e) {
            e.printStackTrace();
            if(e.getCause() instanceof ClassCastException){
                return JsonResponse.error(new Exception("형식이 맞지 않습니다."));
            }else{
                return JsonResponse.error(e);
            }
        }
    }
}
