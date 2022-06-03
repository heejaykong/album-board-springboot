package com.mycompany.backend.service;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.mycompany.backend.dao.BoardDao;
import com.mycompany.backend.dto.Board;
import com.mycompany.backend.dto.Pager;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class BoardService {
  @Resource
  private BoardDao boardDao;

  // total boards count
  public int getTotalBoardNum() {
    log.info("실행");
    return boardDao.count();
  }

  // get list
  public List<Board> getBoards(Pager pager) {
    log.info("실행");
    return boardDao.selectByPage(pager);
  }

  // Create
  public int writeBoard(Board board) {
    log.info("실행");
    return boardDao.insert(board);
  }

  // Read
  public Board getBoard(int bno, boolean hit) {
    log.info("실행");
    if (hit) { // 조회수를 증가시켜야하면 true, 아니면 false
      boardDao.updateBhitcount(bno);
    }
    return boardDao.selectByBno(bno);
  }

  // Update
  public int updateBoard(Board board) {
    log.info("실행");
    return boardDao.update(board);
  }

  // Delete
  public int removeBoard(int bno) {
    log.info("실행");
    return boardDao.deleteByBno(bno);
  }
}
