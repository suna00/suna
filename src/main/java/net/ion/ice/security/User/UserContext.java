package net.ion.ice.security.User;

import org.springframework.security.core.GrantedAuthority;

import java.util.List;

public class UserContext {
    private final String userId;
    private final List<GrantedAuthority> authorities;

    private UserContext(String userId, List<GrantedAuthority> authorities) {
        this.userId = userId;
        this.authorities = authorities;
    }
    
    public static UserContext create(String userId, List<GrantedAuthority> authorities) {
//        if (StringUtils.isBlank(userId)) throw new IllegalArgumentException("Username is blank: " + userId);
        return new UserContext(userId, authorities);
    }

    public String getUserId() {
        return userId;
    }

    public List<GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
