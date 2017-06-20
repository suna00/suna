package net.ion.ice.security.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import net.ion.ice.security.auth.JwtAuthenticationToken;
import net.ion.ice.security.config.JwtConfig;
import net.ion.ice.security.User.UserContext;
import net.ion.ice.security.token.RawAccessJwtToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {
    private final JwtConfig jwtConfig;
    
    @Autowired
    public JwtAuthenticationProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        Jws<Claims> jwsClaims = rawAccessToken.parseClaims(jwtConfig.getSecretKey());
        String subject = jwsClaims.getBody().getSubject();
//        List<String> scopes = jwsClaims.getBody().get("scopes", List.class);
//        List<GrantedAuthority> authorities = scopes.stream()
//                .map(authority -> new SimpleGrantedAuthority(authority))
//                .collect(Collectors.toList());
        
        UserContext context = UserContext.create(subject, Collections.emptyList());
        
        return new JwtAuthenticationToken(context, Collections.emptyList());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (JwtAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
