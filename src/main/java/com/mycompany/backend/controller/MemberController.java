package com.mycompany.backend.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.json.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.backend.dto.Member;
import com.mycompany.backend.security.Jwt;
import com.mycompany.backend.service.MemberService;
import com.mycompany.backend.service.MemberService.JoinResult;
import com.mycompany.backend.service.MemberService.LoginResult;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/member")
public class MemberController {
  @Resource
  private MemberService memberService;

  @Resource
  private PasswordEncoder passwordEncoder;
  
  @Resource
  private RedisTemplate<String, String> redisTemplate;

  @PostMapping("/join")
  public Map<String, Object> join(@RequestBody Member member) {
    log.info("실행");
    // 지금 막 회원가입한 거니까 계정 활성화하기.
    member.setMenabled(true);
    // 비밀번호 암호화하기.
    member.setMpassword(passwordEncoder.encode(member.getMpassword()));
    // 회원가입 시키기.
    JoinResult joinResult = memberService.join(member);
    // 응답내용 설정하기.
    Map<String, Object> map = new HashMap<>();
    if (joinResult == JoinResult.SUCCESSFUL) {
      map.put("result", "successful");
    }
    if (joinResult == JoinResult.DUPLICATED) {
      map.put("result", "duplicated");
    }
    if (joinResult == JoinResult.FAILED) {
      map.put("result", "failed");
    }
    return map;
  }
  
  @PostMapping("/login")
  public ResponseEntity<String> login(@RequestBody Member member) {
    log.info("실행");
    // 1. 아이디나 비번 중 하나라도 빈 문자열이 들어왔을 때
    if (member.getMid() == null || member.getMpassword() == null) {
      return ResponseEntity.status(401).body("mid or mpassword cannot be null");
    }
    // 2. 아이디나 비번 중 하나라도 틀렸을 때
    LoginResult loginResult = memberService.login(member);
    if (loginResult != LoginResult.SUCCESSFUL) {
      return ResponseEntity.status(401).body("mid or mpassword is wrong");
    }
    
    // 3. 로그인 성공했으면 이제 Access/Refresh 토큰을 쿠키에 담아 응답을 보내주자..
    Member dbMember = memberService.getMember(member.getMid());
    // 토큰 얻기
    String accessToken = Jwt.createAccessToken(member.getMid(), dbMember.getMrole());
    String refreshToken = Jwt.createRefreshToken(member.getMid(), dbMember.getMrole());
    
    // Redis에 저장하기
    ValueOperations<String, String> vo = redisTemplate.opsForValue();
    vo.set(accessToken, refreshToken, Jwt.REFRESH_TOKEN_DURATION, TimeUnit.MILLISECONDS);
    
    // 쿠키 생성하기
    String refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                                              .httpOnly(true)
                                              .secure(false)
                                              .path("/")
                                              .maxAge(Jwt.REFRESH_TOKEN_DURATION / 1000)
                                              .domain("localhost")
                                              .build()
                                              .toString();
    
    // 본문 생성하기
    String json = new JSONObject().put("accessToken", accessToken)
                                  .put("mid", member.getMid())
                                  .toString();
    
    // 응답 설정하기
    return ResponseEntity.ok()
                         .header(HttpHeaders.SET_COOKIE, refreshTokenCookie)
                         .header(HttpHeaders.CONTENT_TYPE, "application/json;")
                         .body(json);
  }
  
  @GetMapping("/refreshToken")
  public ResponseEntity<String> refreshToken(
      @RequestHeader("Authorization") String authorization,
      @CookieValue("refreshToken") String refreshToken
  ) {
    // Access Token 얻기
    String accessToken = Jwt.getAccessToken(authorization);
    
    // Access Token 없을 경우
    if (accessToken == null) {
      return ResponseEntity.status(401).body("no access token");
    }
    // Refresh Token 없을 경우
    if (refreshToken == null) {
      return ResponseEntity.status(401).body("no refresh token");
    }
    
    // 동일한 토큰인지 확인하기.(Redis에 저장된 토큰과 비교)
    ValueOperations<String, String> vo = redisTemplate.opsForValue();
    String redisRefreshToken = vo.get(accessToken);
    if (redisRefreshToken == null) {
      return ResponseEntity.status(401).body("wrong access token");
    }
    if (!refreshToken.equals(redisRefreshToken)) {
      return ResponseEntity.status(401).body("invalid refresh token");
    }
    // refreshToken의 유효성을 검증할 필요는 없다. 왜냐하면 refreshToken의 duration과 동일하게 redis도 설정했기 때문에, 이미 지금 검증중이라는건 valid하다는 뜻이니까.
    // 그래도 검증하고 싶다면 다음과 같이 하면 됨.
    // if (!Jwt.validateToken(refreshToken)) {
    //   return ResponseEntity.status(401).body("invalid refresh token");
    // }
    
    // 새로운 AccessToken 생성하기
    Map<String, String> userInfo = Jwt.getUserInfo(refreshToken);
    String mid       = userInfo.get("mid");
    String authority = userInfo.get("authority");
    String newAccessToken = Jwt.createAccessToken(mid, authority);
    
    // 새로 만든 토큰을 Redis에도 저장해주기.
    // 1. Redis에 저장되어있는 기존 정보를 우선 삭제.
    redisTemplate.delete(accessToken);
    // 2. Redis에 새로운 정보 저장.
    vo.set(accessToken, refreshToken, Jwt.REFRESH_TOKEN_DURATION, TimeUnit.MILLISECONDS);
    Date expiration = Jwt.getExpiration(refreshToken);
    vo.set(newAccessToken, refreshToken, expiration.getTime() - new Date().getTime(), TimeUnit.MILLISECONDS);
    
    // 본문 생성하기.
    String json = new JSONObject().put("newAccessToken", newAccessToken)
                                  .put("mid", mid)
                                  .toString();
    // 응답 설정하기.
    return ResponseEntity.ok()
                         .header(HttpHeaders.CONTENT_TYPE, "application/json;")
                         .body(json);
  }
  
  @GetMapping("/logout")
  public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorization) {
    // Access Token 얻기
    String accessToken = Jwt.getAccessToken(authorization);
    if (accessToken == null) {
      return ResponseEntity.status(401).body("invalid access token");
    }
    
    // Access Token이 유효할 경우
    // 우선 Redis에 저장된 인증 정보 먼저 삭제하기
    redisTemplate.delete(accessToken);
    
    // 클라이언트로 보냈던 refreshToken 쿠키 삭제하기. (max age를 0으로 설정하면 쿠키가 삭제된다.)
    String refreshTokenCookie = ResponseCookie.from("refreshToken", "") // 어차피 삭제할거라 빈값주면 됨
                                              .httpOnly(true)
                                              .secure(false)
                                              .path("/")
                                              .maxAge(0)
                                              .domain("localhost")
                                              .build()
                                              .toString();
    // 응답 설정하기
    return ResponseEntity.ok()
                         .header(HttpHeaders.SET_COOKIE, refreshTokenCookie)
                         .body("success");
  }
}
