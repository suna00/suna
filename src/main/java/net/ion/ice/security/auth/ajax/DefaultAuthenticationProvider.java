package net.ion.ice.security.auth.ajax;

import net.ion.ice.security.User.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collections;

@Component
public class DefaultAuthenticationProvider implements AuthenticationProvider {
    private final BCryptPasswordEncoder encoder;

    @Autowired
    public DefaultAuthenticationProvider(final BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");

        String userId = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();


//        if (!encoder.matches(password, user.getPassword())) {
//            throw new BadCredentialsException("Authentication Failed. Username or Password not valid.");
//        }

//        if (user.getRoles() == null) throw new InsufficientAuthenticationException("User has no roles assigned");
        
//        List<GrantedAuthority> authorities = user.getRoles().stream()
//                .map(authority -> new SimpleGrantedAuthority(authority.getRole().authority()))
//                .collect(Collectors.toList());
        
        UserContext userContext = UserContext.create(userId, Collections.emptyList());
        
        return new UsernamePasswordAuthenticationToken(userContext, null, Collections.emptyList());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
