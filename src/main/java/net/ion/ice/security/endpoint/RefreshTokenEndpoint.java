package net.ion.ice.security.endpoint;

import net.ion.ice.security.User.UserContext;
import net.ion.ice.security.auth.jwt.extractor.TokenExtractor;
import net.ion.ice.security.config.JwtConfig;
import net.ion.ice.security.token.JwtToken;
import net.ion.ice.security.token.JwtTokenFactory;
import net.ion.ice.security.token.RawAccessJwtToken;
import net.ion.ice.security.token.RefreshToken;
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
import java.util.Collections;

@RestController
public class RefreshTokenEndpoint {
    @Autowired
    private JwtTokenFactory tokenFactory;
    @Autowired
    private JwtConfig jwtConfig;
    @Autowired
    private TokenExtractor tokenExtractor;
    
    @RequestMapping(value="/api/auth/token", method= RequestMethod.GET, produces={ MediaType.APPLICATION_JSON_VALUE })
    public @ResponseBody
    JwtToken refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String tokenPayload = tokenExtractor.extract(request.getHeader(jwtConfig.getHeadString()));
        
        RawAccessJwtToken rawToken = new RawAccessJwtToken(tokenPayload);
        RefreshToken refreshToken = RefreshToken.create(rawToken, jwtConfig.getSecretKey()).orElseThrow(() -> new RuntimeException());

        String subject = refreshToken.getSubject();
//        User user = userService.getByUsername(subject).orElseThrow(() -> new UsernameNotFoundException("User not found: " + subject));

//        if (user.getRoles() == null) throw new InsufficientAuthenticationException("User has no roles assigned");
//        List<GrantedAuthority> authorities = user.getRoles().stream()
//                .map(authority -> new SimpleGrantedAuthority(authority.getRole().authority()))
//                .collect(Collectors.toList());

        UserContext userContext = UserContext.create(subject, Collections.emptyList());

        return tokenFactory.createAccessJwtToken(userContext);
    }
}
