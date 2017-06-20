package net.ion.ice.security.endpoint;

import com.hazelcast.web.HazelcastHttpSession;
import net.ion.ice.security.auth.JwtAuthenticationToken;
import net.ion.ice.security.User.UserContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
public class ProfileEndpoint {
    @RequestMapping(value="/api/me", method= RequestMethod.GET)
    public @ResponseBody UserContext get(JwtAuthenticationToken token, HttpSession httpSession, HttpServletRequest request) {
        System.out.println("request\t" + request.getCookies()[0].getValue());
        System.out.println("sessionID::::\t" +  httpSession.getId());
        System.out.println("original_sessionID::::\t" +  ((HazelcastHttpSession) httpSession).getOriginalSessionId());
        return (UserContext) token.getPrincipal();
    }

}
