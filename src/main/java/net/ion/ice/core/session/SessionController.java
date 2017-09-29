package net.ion.ice.core.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Created by jaehocho on 2017. 3. 10..
 */
@Controller
public class SessionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private SessionService sessionService ;

    @RequestMapping(value = "/session/member/login", method = RequestMethod.POST)
    public Object login(HttpServletRequest request, HttpServletResponse response, @RequestParam String userId, @RequestParam String password) {
        sessionService.memberLogin(request, response, userId, password, null) ;
        return "index";
    }
}
