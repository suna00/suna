package net.ion.ice.security.auth.jwt;

import net.ion.ice.security.auth.JwtAuthenticationToken;
import net.ion.ice.security.auth.jwt.extractor.TokenExtractor;
import net.ion.ice.security.common.CookieUtil;
import net.ion.ice.security.config.JwtConfig;
import net.ion.ice.security.token.JwtTokenFactory;
import net.ion.ice.security.token.RawAccessJwtToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtTokenAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {
    private final AuthenticationFailureHandler failureHandler;
    private final TokenExtractor tokenExtractor;
    private final JwtConfig jwtConfig;
    private final CookieUtil cookieUtil;

    @Autowired
    public JwtTokenAuthenticationProcessingFilter(AuthenticationFailureHandler failureHandler,
                                                  TokenExtractor tokenExtractor, RequestMatcher matcher, JwtConfig jwtConfig, CookieUtil cookieUtil) {
        super(matcher);

        this.failureHandler = failureHandler;
        this.tokenExtractor = tokenExtractor;
        this.jwtConfig = jwtConfig;
        this.cookieUtil = cookieUtil;
    }

    /*
     * -->JwtAuthenticationProvider
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
//        String tokenPayload = request.getHeader(jwtConfig.getHeadString());
        String tokenPayload = cookieUtil.getValue(request, "accessToken");
        RawAccessJwtToken token = new RawAccessJwtToken(tokenExtractor.extract(tokenPayload));
        Object sessionToken = null;
        try {
            if (request.getSession().getAttribute("accessToken").equals(token.getToken())) {
                sessionToken = token;
            }
            return getAuthenticationManager().authenticate(new JwtAuthenticationToken((RawAccessJwtToken) sessionToken));
        } catch (NullPointerException ex) {
            throw new AuthenticationServiceException("Token is not exist in session");
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
