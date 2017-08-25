package net.ion.ice.security.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.core.cluster.ClusterService;
import net.ion.ice.core.data.ResponseUtils;
import net.ion.ice.core.response.JsonResponse;
import net.ion.ice.core.session.SessionService;
import net.ion.ice.security.common.CookieUtil;
import net.ion.ice.security.token.JwtToken;
import net.ion.ice.security.token.JwtTokenFactory;
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
public class TokenEndpoint {
    @Autowired
    SessionService sessionService;
    @Autowired
    private JwtTokenFactory tokenFactory;
    @Autowired
    private CookieUtil cookieUtil;
    @Autowired
    ObjectMapper objectMapper;

    @RequestMapping(value = "/api/auth/token", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Object jwtToken(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Map<String, Object> data = new HashMap<>();

        String token = cookieUtil.getValue(request, "iceJWT");

        if (token == null) {
            JwtToken accessToken = tokenFactory.createInitJwtToken();
//        JwtToken refreshToken = tokenFactory.createRefreshToken();
            token = accessToken.getToken();
            data.put("iceJWT", accessToken.getToken());
//        data.put("refreshToken", refreshToken.getToken());
//        int maxAge = 60 * 60 * 24; // 24 hour
//            clusterService.putSession(accessToken.getToken());
            cookieUtil.create(response, "iceJWT", "SDP ".concat(accessToken.getToken()), false, false, -1, request.getServerName());
            sessionService.putSession(token);
        }
//        cookieUtil.create(response, "refreshToken", "SDP ".concat(refreshToken.getToken()), true, false, -1, request.getServerName());


        return JsonResponse.create(data);
    }
}
