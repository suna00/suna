package net.ion.ice.security.auth.ajax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.web.WebFilter;
import net.ion.ice.security.User.UserContext;
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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper mapper;
    private final JwtTokenFactory tokenFactory;
    private final CookieUtil cookieUtil;


    @Autowired
    public DefaultAuthenticationSuccessHandler(final ObjectMapper mapper, final JwtTokenFactory tokenFactory, CookieUtil cookieUtil) {
        this.mapper = mapper;
        this.tokenFactory = tokenFactory;
        this.cookieUtil = cookieUtil;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        UserContext userContext = (UserContext) authentication.getPrincipal();

        JwtToken accessToken = tokenFactory.createAccessJwtToken(userContext);
        JwtToken refreshToken = tokenFactory.createRefreshToken(userContext);

        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("accessToken", accessToken.getToken());
        tokenMap.put("refreshToken", refreshToken.getToken());

        HttpSession session = request.getSession();
        session.setAttribute("accessToken", accessToken.getToken());

        System.out.println("sessionID::::\t" + session.getId());
        int maxAge = 60 * 60 * 24; // 24 hour

        cookieUtil.create(response, "accessToken", "SDP ".concat(accessToken.getToken()), false, false, -1, request.getServerName());
        cookieUtil.create(response, "refreshToken", "SDP ".concat(refreshToken.getToken()), true, false, -1, request.getServerName());

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), tokenMap);
    }

}
