package net.ion.ice.security.endpoint;

import net.ion.ice.core.response.JsonResponse;
import net.ion.ice.core.session.SessionService;
import net.ion.ice.security.common.CookieUtil;
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
import java.util.HashMap;
import java.util.Map;

@RestController
public class RemoveTokenEndpoint {
    @Autowired
    SessionService sessionService;

    @RequestMapping(value = "api/auth/remove", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Object jwtToken(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Map<String, Object> resultCodeMap = new HashMap<>();

        sessionService.removeSession(request);

        CookieUtil.clear(request, response, "iceJWT");
        CookieUtil.clear(request, response, "iceRefreshJWT");

        resultCodeMap.put("code", "200");
        resultCodeMap.put("message", "JWT Removed");

        return JsonResponse.create(resultCodeMap);
    }
}
