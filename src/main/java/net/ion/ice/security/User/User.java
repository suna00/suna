package net.ion.ice.security.User;

import lombok.Data;

/**
 * Created by seonwoong on 2017. 6. 7..
 */
@Data
public class User {
    private String username;
    private String password;

    public User(String username, String password){
        this.username = username;
        this.password = password;
    }

}