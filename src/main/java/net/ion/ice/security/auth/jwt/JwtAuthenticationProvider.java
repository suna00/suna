package net.ion.ice.security.auth.jwt;

import net.ion.ice.security.auth.JwtAuthenticationToken;
import net.ion.ice.security.config.JwtConfig;
import net.ion.ice.security.token.RawAccessJwtToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {
    private final JwtConfig jwtConfig;

    @Autowired
    public JwtAuthenticationProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    // --> successfulAuthentication
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        rawAccessToken.tokenParseClaims(jwtConfig.getSecretKey());

        return new JwtAuthenticationToken();
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (JwtAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
