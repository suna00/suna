package net.ion.ice.security.auth.ajax;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.security.UserContext;
import net.ion.ice.security.common.CookieUtil;
import net.ion.ice.security.token.JwtToken;
import net.ion.ice.security.token.JwtTokenFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private final JwtTokenFactory tokenFactory;


    @Autowired
    public DefaultAuthenticationSuccessHandler(final ObjectMapper objectMapper, final JwtTokenFactory tokenFactory) {
        this.objectMapper = objectMapper;
        this.tokenFactory = tokenFactory;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        UserContext userContext = (UserContext) authentication.getPrincipal();

//        JwtToken accessToken = tokenFactory.createAccessJwtToken(userContext);
//        JwtToken refreshToken = tokenFactory.createRefreshToken();

        Map<String, String> tokenMap = new HashMap<>();
//        tokenMap.put("iceJWT", accessToken.getToken());
//        tokenMap.put("iceRefreshJWT", refreshToken.getToken());

//        HttpSession session = request.getSession();
//        session.setAttribute("accessToken", accessToken.getToken());

//        System.out.println("sessionID::::\t" + session.getId());
//        int maxAge = 60 * 60 * 24; // 24 hour

//        CookieUtil.create(response, "iceJWT", "SDP ".concat(accessToken.getToken()), false, false, -1, request.getServerName());
//        CookieUtil.create(response, "iceRefreshJWT", "SDP ".concat(refreshToken.getToken()), true, false, -1, request.getServerName());

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), tokenMap);
    }

}
