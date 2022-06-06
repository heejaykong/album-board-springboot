package com.mycompany.backend.config;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mycompany.backend.security.JwtAuthenticationFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  @Resource
  private RedisTemplate redisTemplate;
  
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.info("실행");
//    서버 세션 비활성화하기
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    
//    폼 로그인 비활성화하기
    http.formLogin().disable();
    
//    사이트간 요청 위조방지 비활성화하기
    http.csrf().disable();
    
//    요청경로 권한 설정하기
    http.authorizeRequests()
        .antMatchers("/board/list/**").permitAll()
        .antMatchers("/board/battach/**").permitAll()
        .antMatchers("/board/**").authenticated()
        .antMatchers("/**").permitAll();
    
//    JWT토큰 인증필터 추가하기
    http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    
//    CORS 설정하기
    http.cors();
  }
  
//  @Bean 사실 여기저기 쓰이는 놈이 아니기 때문에 꼭 @Bean 안 붙여도 됨.
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter();
    jwtAuthenticationFilter.setRedisTemplate(redisTemplate);
    return jwtAuthenticationFilter;
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    log.info("실행");
    
    /* MPA 방식이라 주석처리
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(new CustomUserDetailsService());
    provider.setPasswordEncoder(passwordEncoder());
    auth.authenticationProvider(provider); */
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    log.info("실행");
    DefaultWebSecurityExpressionHandler defaultWebSecurityExpressionHandler = new DefaultWebSecurityExpressionHandler();
    defaultWebSecurityExpressionHandler.setRoleHierarchy(roleHierarchyImpl());
    web.expressionHandler(defaultWebSecurityExpressionHandler);
    
    /* MPA에서 시큐리티를 적용하지 않는 경로 설정하는 방식이라 주석처리
    web.ignoring()
    .antMatchers("/images/**")
    .antMatchers("/css/**")
    .antMatchers("/js/**")
    .antMatchers("/bootstrap/**")
    .antMatchers("/jquery/**")
    .antMatchers("/favicon.ico"); */
  }
  
  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
  
  @Bean
  public RoleHierarchyImpl roleHierarchyImpl() {
    log.info("실행");
    RoleHierarchyImpl roleHierarchyImpl = new RoleHierarchyImpl();
    roleHierarchyImpl.setHierarchy("ROLE_ADMIN > ROLE_MANAGER > ROLE_USER");
    return roleHierarchyImpl;
  }
  
  @Bean //corsConfigurationSource는 메소드명 반드시 동일해야 함!
  public CorsConfigurationSource corsConfigurationSource() {
    log.info("실행");
    CorsConfiguration configuration = new CorsConfiguration();
    // 모든 요청 사이트 허용
    configuration.addAllowedOrigin("*");
    // 모든 요청 방식 허용
    configuration.addAllowedMethod("*");
    // 모든 요청 헤더 허용
    configuration.addAllowedHeader("*");
    // 모든 URL 요청에 대해서 위 내용을 적용
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
