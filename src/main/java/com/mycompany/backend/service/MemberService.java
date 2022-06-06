package com.mycompany.backend.service;

import javax.annotation.Resource;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mycompany.backend.dao.MemberDao;
import com.mycompany.backend.dto.Member;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class MemberService {
  public enum JoinResult {
    SUCCESSFUL,
    DUPLICATED,
    FAILED
  }
  public enum LoginResult {
    SUCCESSFUL,
    FAILED
  }
  
  @Resource
  private MemberDao memberDao;
  @Resource
  private PasswordEncoder passwordEncoder;

  // 회원가입
  public JoinResult join(Member member) {
    Member dbMember = memberDao.selectByMid(member.getMid());
    try {
      // 이미 존재하는 아이디인지 확인하기
      if (dbMember != null) return JoinResult.DUPLICATED;
      int rows = memberDao.insert(member);
      if (rows <= 0) return JoinResult.FAILED;
    } catch (Exception e) {
      log.error(e.getMessage());
      return JoinResult.FAILED;
    }
    return JoinResult.SUCCESSFUL;
  }
  
  // 로그인
  public LoginResult login(Member member) {
    Member dbMember = memberDao.selectByMid(member.getMid());
    try {
      // 틀린 아이디인지 확인하기
      if (dbMember == null) return LoginResult.FAILED;
      // 틀린 비밀번호인지 확인하기
      if (!passwordEncoder.matches(member.getMpassword(), dbMember.getMpassword()))  return LoginResult.FAILED;
    } catch (Exception e) {
      log.error(e.getMessage());
      return LoginResult.FAILED;
    }
    return LoginResult.SUCCESSFUL;
  }
  
  public Member getMember(String mid) {
    return memberDao.selectByMid(mid);
  }
}
