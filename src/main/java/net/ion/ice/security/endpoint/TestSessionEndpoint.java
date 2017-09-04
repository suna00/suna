package net.ion.ice.security.endpoint;

import com.hazelcast.web.HazelcastHttpSession;
import net.ion.ice.core.response.JsonResponse;
import net.ion.ice.core.session.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by seonwoong on 2017. 6. 19..
 */
@Controller
public class TestSessionEndpoint {

    @Autowired
    SessionService sessionService;

    @RequestMapping(value = "/session/put", method = RequestMethod.GET)
    @ResponseBody
    Object setSessionData(HttpServletRequest request) throws UnsupportedEncodingException {
        try {
            for (String key : request.getParameterMap().keySet()) {
                sessionService.setSessionValue(request, key, request.getParameterMap().get(key));
            }
            return JsonResponse.create();
        } catch (NullPointerException e) {
            return JsonResponse.error(e);
        }
    }

    @RequestMapping(value = "/session/get", method = RequestMethod.GET)
    @ResponseBody
    Object getSessionData(HttpServletRequest request) throws UnsupportedEncodingException {
        try {
            return JsonResponse.create(sessionService.getSession(request));
        } catch (NullPointerException e) {
            return JsonResponse.error(e);
        }
    }
}
