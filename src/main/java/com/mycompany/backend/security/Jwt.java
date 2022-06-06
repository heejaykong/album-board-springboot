package com.mycompany.backend.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Jwt {
  private static final String JWT_SECRET_KEY = "kosa12345";
  private static final long ACCESS_TOKEN_DURATION = 1000 * 60 * 30; // 30분
  public static final long REFRESH_TOKEN_DURATION = 1000 * 60 * 60 * 24;

  // 1. AccessToken 생성하기
  public static String createAccessToken(String mid, String authority) {
    log.info("실행");
    String accessToken = null;
    try {
      accessToken = Jwts.builder()
                        // 헤더 설정
                        .setHeaderParam("alg", "HS256")
                        .setHeaderParam("typ", "JWT")
                        // 토큰의 유효기간 설정
                        .setExpiration(new Date(new Date().getTime() + ACCESS_TOKEN_DURATION))
                        // 페이로드 설정
                        .claim("mid", mid)
                        .claim("authority", authority)
                        // 서명 설정
                        .signWith(SignatureAlgorithm.HS256, JWT_SECRET_KEY.getBytes("UTF-8"))
                        // 토큰 생성(문자열로 리턴)
                        .compact();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return accessToken;
  }

  // 2. RefreshToken 생성하기
  public static String createRefreshToken(String mid, String authority) {
    log.info("실행");
    String refreshToken = null;
    try {
      refreshToken = Jwts.builder()
                        // 헤더 설정
                        .setHeaderParam("alg", "HS256")
                        .setHeaderParam("typ", "JWT")
                        // 토큰의 유효기간 설정
                        .setExpiration(new Date(new Date().getTime() + REFRESH_TOKEN_DURATION))
                        // 페이로드 설정
                        .claim("mid", mid)
                        .claim("authority", authority)
                        // 서명 설정
                        .signWith(SignatureAlgorithm.HS256, JWT_SECRET_KEY.getBytes("UTF-8"))
                        // 토큰 생성(문자열로 리턴)
                        .compact();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return refreshToken;
  }
  
  // 3. 요청헤더인 Authorization으로부터 Access Token 얻기
  public static String getAccessToken(String authorization) {
    log.info("실행");
    String accessToken = null;
    if (authorization != null && authorization.startsWith("Bearer ")) {
      accessToken = authorization.substring(7); //Bearer 다음에 나오는 문자열(=토큰)을 가져오려는 것임
    }
    return accessToken;
  }
  
  // 4. 토큰(Access 및 Refresh) 유효성 검사하기
  public static boolean validateToken(String token) {
    log.info("실행");
    boolean result = false;
    try {
      result = Jwts.parser()
                    // 애초에 설정했던 시크릿키와 일치하는지 확인
                    .setSigningKey(JWT_SECRET_KEY.getBytes("UTF-8"))
                    // payload를 담고 있는 claim객체를 리턴
                    .parseClaimsJws(token)
                    // claim객체 안에서 찐 claim 비로소 얻기
                    .getBody()
                    // 유효기간 얻기(Date타입)
                    .getExpiration()
                    // "지금보다 더 나중인가요?(=토큰이 아직 유효한가요?)"
                    .after(new Date());
                    ;
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return result;
  }
  
  // 5. 토큰의 만료시간 얻기
  public static Date getExpiration(String token) {
    log.info("실행");
    Date expiration = null;
    try {
      expiration = Jwts.parser()
                          // 애초에 설정했던 시크릿키와 일치하는지 확인
                          .setSigningKey(JWT_SECRET_KEY.getBytes("UTF-8"))
                          // payload를 담고 있는 claim객체를 리턴
                          .parseClaimsJws(token)
                          // claim객체 안에서 찐 claim 비로소 얻기
                          .getBody()
                          // 유효기간 얻기(Date타입)
                          .getExpiration();
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return expiration;
  }
  
  // 6. 사용자 정보 얻기
  public static Map<String, String> getUserInfo(String token) {
    log.info("실행");
    Map<String, String> userInfo = new HashMap<>();
    try {
      Claims claims = Jwts.parser()
                          // 애초에 설정했던 시크릿키와 일치하는지 확인
                          .setSigningKey(JWT_SECRET_KEY.getBytes("UTF-8"))
                          // payload를 담고 있는 claim객체를 리턴
                          .parseClaimsJws(token)
                          // claim객체 안에서 찐 claim 비로소 얻기
                          .getBody();
      userInfo.put("mid", claims.get("mid", String.class));
      userInfo.put("authority", claims.get("authority", String.class));
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return userInfo;
  }
  
  public static void main(String[] args) {
    // 테스트
    String accessToken = createAccessToken("user", "ROLE_USER");
    
    log.info(accessToken);
//  Thread.sleep(2000); 유효기간을 1초로 하고 2초 쉬게 한 뒤 false로 잘 뜨는지 테스팅해본 흔적
//  System.out.println(validateToken(accessToken));

    Date expiration = getExpiration(accessToken);
    Map<String, String> userInfo = getUserInfo(accessToken);
    System.out.println(expiration + " " + userInfo);
  }
}
