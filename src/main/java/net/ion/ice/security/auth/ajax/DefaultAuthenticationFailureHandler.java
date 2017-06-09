package net.ion.ice.security.auth.ajax;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ion.ice.security.common.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final ObjectMapper mapper;
    
    @Autowired
    public DefaultAuthenticationFailureHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }	
    
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException e) throws IOException, ServletException {
		
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		
		if (e instanceof BadCredentialsException) {
			mapper.writeValue(response.getWriter(), ErrorResponse.of("Invalid username or password", HttpStatus.UNAUTHORIZED));
		} else if (e instanceof AuthenticationException) {
			mapper.writeValue(response.getWriter(), ErrorResponse.of("Token has expired", HttpStatus.UNAUTHORIZED));
		} else if (e instanceof AuthenticationServiceException) {
		    mapper.writeValue(response.getWriter(), ErrorResponse.of(e.getMessage(), HttpStatus.UNAUTHORIZED));
		}

		mapper.writeValue(response.getWriter(), ErrorResponse.of("Authentication failed", HttpStatus.UNAUTHORIZED));
	}
}
