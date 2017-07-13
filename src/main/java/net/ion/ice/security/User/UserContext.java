package net.ion.ice.security.User;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

public class UserContext {
    private final String userId;

    private UserContext(String userId) {
        this.userId = userId;
    }

    public static UserContext create(String userId) {
        if (StringUtils.isBlank(userId)) throw new IllegalArgumentException("UserID is blank: " + userId);
        return new UserContext(userId);
    }
    public String getUserId() {
        return userId;
    }

}
