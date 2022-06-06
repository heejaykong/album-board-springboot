package com.mycompany.backend.security;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private RedisTemplate redisTemplate;
  public void setRedisTemplate(RedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }
  
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    log.info("실행");
    
    // 요청 헤더로부터 Authorization 헤더값 얻기
    String authorization = request.getHeader("Authorization");
    
    // AccessToken 추출하기
    String accessToken = Jwt.getAccessToken(authorization);
    
    // 검증작업 하기
    if (accessToken != null && Jwt.validateToken(accessToken)) {
      // Redis에 존재하는지 여부 확인
      ValueOperations<String, String> vo = redisTemplate.opsForValue();
      String redisRefreshToken = vo.get(accessToken);
      if (redisRefreshToken != null) {
        // 인증 처리하기
        Map<String, String> userInfo = Jwt.getUserInfo(accessToken); // 사용자 정보 얻어내기
        String mid = userInfo.get("mid");
        String authority = userInfo.get("authority");
        
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(mid, null, AuthorityUtils.createAuthorityList(authority));
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
      }
    }
    
    // "다음 필터 실행하도록 해"
    filterChain.doFilter(request, response);
  }
}
