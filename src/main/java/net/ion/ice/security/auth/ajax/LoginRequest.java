package net.ion.ice.security.auth.ajax;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Data
public class LoginRequest {

    private String userId;
    private String password;

    public LoginRequest(HttpServletRequest request) {
        this.userId = request.getParameter("userId");
        this.password = request.getParameter("password");
    }
}
