package net.ion.ice.security.User;

import lombok.Data;

/**
 * Created by seonwoong on 2017. 6. 7..
 */
@Data
public class User {
    private String userId;
    private String password;

    public User(String userId, String password){
        this.userId = userId;
        this.password = password;
    }

}