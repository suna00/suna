package net.ion.ice.core.session;

import net.ion.ice.core.api.ApiException;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.response.JsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jaehocho on 2017. 3. 10..
 */
@Controller
public class SessionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private SessionService sessionService ;

    @RequestMapping(value = "/session/login", method = RequestMethod.POST)
    @ResponseBody
    public Object login(HttpServletRequest request, HttpServletResponse response, @RequestParam String userId, @RequestParam String password) {
        try {
            return JsonResponse.create(sessionService.userLogin(request, response, userId, password));
        }catch(ApiException e){
            return JsonResponse.error(e) ;
        }
    }

    @RequestMapping(value = "/session/logout", method = RequestMethod.POST)
    @ResponseBody
    public Object logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            sessionService.removeSession(request);
            return JsonResponse.create();
        }catch(Exception e){
            return JsonResponse.error(e) ;
        }
    }

    @RequestMapping(value = "/session/me", method = RequestMethod.GET)
    @ResponseBody
    public Object login(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> session = null;
        try {
            session = sessionService.getSession(request);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("result", "400") ;
        extraData.put("resultMessage", "Authentication Error") ;

        if (session != null) {
            Node userNode = (Node) session.get("user");
            if (null != userNode) {
                extraData.put("result", "200");
                extraData.put("resultMessage", "SUCCESS") ;

                extraData.put("userId", userNode.get("userId"));
                extraData.put("name", userNode.get("name"));
                extraData.put("role", session.get("role"));
                extraData.put("user", userNode);
            }
        }
        return extraData;
    }
}
