//package net.ion.ice.security.auth.ajax;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.web.authentication.logout.LogoutFilter;
//import org.springframework.security.web.authentication.logout.LogoutHandler;
//import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
//import org.springframework.security.web.util.matcher.RequestMatcher;
//import org.springframework.stereotype.Component;
//
///**
// * Created by seonwoong on 2017. 6. 21..
// */
//@Component
//public class LogoutProcessFilter extends LogoutFilter  {
//
//    private final LogoutSuccessHandler logoutSuccessHandler;
//
//    public LogoutProcessFilter(String logoutSuccessUrl, LogoutSuccessHandler logoutSuccessHandler) {
//        super(logoutSuccessUrl);
//        RequestMatcher requestMatcher = new AntPathRequestMatcher("/api/auth/logout");
//        setLogoutRequestMatcher(requestMatcher);
//        this.logoutSuccessHandler = logoutSuccessHandler;
//    }
//}
