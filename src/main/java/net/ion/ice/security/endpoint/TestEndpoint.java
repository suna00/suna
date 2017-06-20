package net.ion.ice.security.endpoint;

import com.hazelcast.web.HazelcastHttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by seonwoong on 2017. 6. 19..
 */
@Controller
public class TestEndpoint {
    @RequestMapping(value="/session", method= RequestMethod.GET)
    @ResponseBody
    Object getSessionData(HttpSession httpSession, HttpServletRequest request) {
        System.out.println("request\t" + request.getCookies()[0].getValue());
//        System.out.println("JWT-TOKEN::::\t" + httpSession.getAttribute("JWT-TOKEN"));
        System.out.println("sessionID::::\t" +  httpSession.getId());
        System.out.println("original_sessionID::::\t" +  ((HazelcastHttpSession) httpSession).getOriginalSessionId());
        return httpSession.getAttribute("JWT-TOKEN");
    }
}
