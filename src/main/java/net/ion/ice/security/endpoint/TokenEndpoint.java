package net.ion.ice.security.endpoint;

import net.ion.ice.core.response.JsonResponse;
import net.ion.ice.core.session.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class TokenEndpoint {
    @Autowired
    SessionService sessionService;

    @RequestMapping(value = "/auth/token", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Object jwtToken(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        sessionService.putSession(request, response, null);
        return JsonResponse.create();
    }
}
