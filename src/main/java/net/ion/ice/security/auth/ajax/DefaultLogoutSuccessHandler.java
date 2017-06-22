package net.ion.ice.security.auth.ajax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.web.WebFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by seonwoong on 2017. 6. 21..
 */

@Component
public class DefaultLogoutSuccessHandler implements LogoutSuccessHandler {
    @Autowired
    WebFilter webFilter;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        System.out.println(request);
        System.out.println(response);
        request.getSession().invalidate();
        webFilter.destroy();
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setStatus(HttpStatus.OK.value());
    }
}