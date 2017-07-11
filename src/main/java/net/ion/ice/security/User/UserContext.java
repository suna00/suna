package net.ion.ice.security.User;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

public class UserContext {
    private final String userId;
    private final String name;

    private UserContext(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public static UserContext create(String userId, String name) {
        if (StringUtils.isBlank(userId)) throw new IllegalArgumentException("UserID is blank: " + userId);
        return new UserContext(userId, name);
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }
}
