package net.ion.ice.security.endpoint;

import net.ion.ice.security.User.UserContext;
import net.ion.ice.security.auth.jwt.extractor.TokenExtractor;
import net.ion.ice.security.common.CookieUtil;
import net.ion.ice.security.config.JwtConfig;
import net.ion.ice.security.token.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collections;

@RestController
public class RefreshTokenEndpoint {
    @Autowired
    private JwtTokenFactory tokenFactory;
    @Autowired
    private JwtConfig jwtConfig;
    @Autowired @Qualifier("jwtHeaderTokenExtractor") private TokenExtractor tokenExtractor;
    
    @RequestMapping(value="/auth/refreshToken", method= RequestMethod.GET, produces={ MediaType.APPLICATION_JSON_VALUE })
    public @ResponseBody
    JwtToken refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String tokenPayload = CookieUtil.getValue(request, "refreshToken");
        RawAccessJwtToken rawToken = new RawAccessJwtToken(tokenExtractor.extract(tokenPayload));
        RefreshToken refreshToken = RefreshToken.create(rawToken, jwtConfig.getSecretKey()).orElseThrow(() -> new RuntimeException());

        String subject = refreshToken.getSubject();
//        User user = userService.getByUsername(subject).orElseThrow(() -> new UsernameNotFoundException("User not found: " + subject));

//        if (user.getRoles() == null) throw new InsufficientAuthenticationException("User has no roles assigned");
//        List<GrantedAuthority> authorities = user.getRoles().stream()
//                .map(authority -> new SimpleGrantedAuthority(authority.getRole().authority()))
//                .collect(Collectors.toList());

        UserContext userContext = UserContext.create(subject);
        HttpSession  session = request.getSession();
        AccessJwtToken accessToken = tokenFactory.createAccessJwtToken(userContext);
        session.setAttribute("accessToken", accessToken.getToken());
        CookieUtil.create(response, "accessToken", "SDP ".concat(accessToken.getToken()), false, false, -1, request.getServerName());

        return accessToken;
    }
}
