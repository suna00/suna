package net.ion.ice.security.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.core.data.ResponseUtils;
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
    private JwtTokenFactory tokenFactory;
    @Autowired
    private CookieUtil cookieUtil;
    @Autowired
    ObjectMapper objectMapper;

    @RequestMapping(value = "/api/auth/token", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    Map<String, Object> jwtToken(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Map<String, Object> tokenMap = new HashMap<>();
        JwtToken accessToken = tokenFactory.createInitJwtToken();
//        JwtToken refreshToken = tokenFactory.createRefreshToken();
        tokenMap.put("accessToken", accessToken.getToken());
//        tokenMap.put("refreshToken", refreshToken.getToken());
//        HttpSession session = request.getSession();
//        session.setAttribute("accessToken", accessToken.getToken());
//        System.out.println("sessionID::::\t" + session.getId());
//        int maxAge = 60 * 60 * 24; // 24 hour

        cookieUtil.create(response, "accessToken", "SDP ".concat(accessToken.getToken()), false, false, -1, request.getServerName());
//        cookieUtil.create(response, "refreshToken", "SDP ".concat(refreshToken.getToken()), true, false, -1, request.getServerName());

        ResponseUtils.response(tokenMap);

        return ResponseUtils.response(tokenMap);
    }
}
