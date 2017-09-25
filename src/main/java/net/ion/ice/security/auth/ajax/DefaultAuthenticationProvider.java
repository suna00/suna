package net.ion.ice.security.auth.ajax;

import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import net.ion.ice.security.user.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class DefaultAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private NodeService nodeService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.notNull(authentication, "No authentication data provided");

        String userId = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();
        Node userNode = nodeService.getNode("user", userId);

        if (!userNode.get("password").equals(password)) {
            throw new BadCredentialsException("아이디/패스워드가 맞지 않습니다.");
        }

        UserContext userContext = UserContext.create(userId);

        return new UsernamePasswordAuthenticationToken(userContext, password);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
