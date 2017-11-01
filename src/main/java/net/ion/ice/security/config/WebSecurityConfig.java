package net.ion.ice.security.config;

import net.ion.ice.security.auth.ajax.DefaultAuthenticationProvider;
import net.ion.ice.security.auth.ajax.DefaultLogoutHandler;
import net.ion.ice.security.auth.ajax.DefaultLogoutSuccessHandler;
import net.ion.ice.security.auth.ajax.LoginProcessingFilter;
import net.ion.ice.security.auth.jwt.JwtAuthenticationProvider;
import net.ion.ice.security.auth.jwt.JwtTokenAuthenticationProcessingFilter;
import net.ion.ice.security.auth.jwt.SkipPathRequestMatcher;
import net.ion.ice.security.auth.jwt.extractor.TokenExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    public static final String FORM_BASED_LOGIN_ENTRY_POINT = "/login";
    public static final String FORM_BASED_LOGOUT_ENTRY_POINT = "/logout";
    public static final String TOKEN_BASED_AUTH_ENTRY_POINT = "/api/**";
//    public static final String TOKEN_BASED_AUTH_ENTRY_POINT = "/certification/**";
    public static final String TOKEN_ENTRY_POINT = "/api/auth/token";
    public static final String TOKEN_REFRESH_ENTRY_POINT = "/api/auth/refreshToken";
    public static final String TOKEN_REMOVE_ENTRY_POINT = "/api/auth/remove";

    @Autowired
    private AuthenticationSuccessHandler successHandler;
    @Autowired
    private AuthenticationFailureHandler failureHandler;
    @Autowired
    private DefaultAuthenticationProvider authenticationProvider;
    @Autowired
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Autowired
    private DefaultLogoutHandler defaultLogoutHandler;

    @Autowired
    private DefaultLogoutSuccessHandler defaultLogoutSuccessHandler;

    @Autowired
    private TokenExtractor tokenExtractor;

    @Autowired
    private AuthenticationManager authenticationManager;

//    @Autowired
//    private WebFilter webFilter;

    protected LoginProcessingFilter buildAjaxLoginProcessingFilter() throws Exception {
        LoginProcessingFilter filter = new LoginProcessingFilter(FORM_BASED_LOGIN_ENTRY_POINT, successHandler, failureHandler);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    protected JwtTokenAuthenticationProcessingFilter buildJwtTokenAuthenticationProcessingFilter() throws Exception {
        List<String> pathsToSkip = Arrays.asList(TOKEN_ENTRY_POINT,TOKEN_REFRESH_ENTRY_POINT, TOKEN_REMOVE_ENTRY_POINT,FORM_BASED_LOGIN_ENTRY_POINT, FORM_BASED_LOGOUT_ENTRY_POINT);
        SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(pathsToSkip, TOKEN_BASED_AUTH_ENTRY_POINT);
        JwtTokenAuthenticationProcessingFilter filter = new JwtTokenAuthenticationProcessingFilter(failureHandler, tokenExtractor, matcher);
        filter.setAuthenticationManager(this.authenticationManager);
        return filter;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(authenticationProvider);
        auth.authenticationProvider(jwtAuthenticationProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http    .cors().and()
                .csrf().disable() // We don't need CSRF for JWT based authentication
                .exceptionHandling()

                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(FORM_BASED_LOGIN_ENTRY_POINT).permitAll()  // Login end-point
                .antMatchers(TOKEN_ENTRY_POINT).permitAll()     // Token refresh end-point
                .antMatchers(TOKEN_REFRESH_ENTRY_POINT).permitAll()     // Token refresh end-point
                .antMatchers(FORM_BASED_LOGOUT_ENTRY_POINT).permitAll() // Logout end-point
                .and()
                .authorizeRequests()
                .antMatchers(TOKEN_BASED_AUTH_ENTRY_POINT).authenticated() // Protected API End-points
                .and()
//                .addFilterBefore(webFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildAjaxLoginProcessingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(logoutFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildJwtTokenAuthenticationProcessingFilter(), UsernamePasswordAuthenticationFilter.class);

    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(Arrays.asList("GET","POST", "PUT", "DELETE"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    LogoutFilter logoutFilter(){
        LogoutFilter logoutFilter = new LogoutFilter(defaultLogoutSuccessHandler, defaultLogoutHandler);
        AntPathRequestMatcher requestMatcher = new AntPathRequestMatcher("/logout");
        logoutFilter.setLogoutRequestMatcher(requestMatcher);
        return logoutFilter;
    }
}
