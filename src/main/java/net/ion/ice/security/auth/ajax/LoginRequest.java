package net.ion.ice.security.auth.ajax;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class LoginRequest {

    private String userId;
    private String password;

    public LoginRequest(HttpServletRequest request) {
        this.userId = request.getParameter("userId");
        this.password = request.getParameter("password");
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
