package com.mycompany.backend.controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mycompany.backend.dto.Board;
import com.mycompany.backend.dto.Pager;
import com.mycompany.backend.service.BoardService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/board")
public class BoardController {
  private static final String SUCCESS_MESSAGE = "successful";
  private static final String FAIL_MESSAGE = "failed";
  @Resource
  private BoardService boardService;

  // get list
  @GetMapping("/list")
  public Map<String, Object> list(@RequestParam(defaultValue = "1") int pageNo) {
    log.info("실행");

    Map<String, Object> map = new HashMap<>();

    Pager pager = new Pager(5, 5, boardService.getTotalBoardNum(), pageNo);
    List<Board> boards = boardService.getBoards(pager);

    map.put("boards", boards);
    map.put("pager", pager);

    return map;
  }

  // Create
  @PostMapping("/")
  public Board create(Board board) {
    log.info("실행");
    // 프론트에서 넘어온 첨부파일이 있는지 확인
    if (board.getBattach() != null && !board.getBattach().isEmpty()) {
      MultipartFile mf = board.getBattach();
      board.setBattachoname(mf.getOriginalFilename());
      board.setBattachsname(new Date().getTime() + "-" + mf.getOriginalFilename());
      board.setBattachtype(mf.getContentType());
      try { // 이미지 저장
        File file = new File("C:/Temp/uploadedfiles/" + board.getBattachsname());
        mf.transferTo(file);
      } catch (Exception e) {
        log.error(e.getMessage()); // error 레벨 로깅
      }
    }

    boardService.writeBoard(board); // writeBoard()를 실행하고나서는 매퍼의 <selectKey> 덕분에 아래처럼 getBno()를 사용할 수 있어짐
    return boardService.getBoard(board.getBno(), false);
  }

  // Read
  @GetMapping("/{bno}")
  public ResponseEntity<Board> read(@PathVariable int bno, @RequestParam(defaultValue="false") boolean hit) {
    log.info("실행");
    return ResponseEntity.ok(boardService.getBoard(bno, hit));
  }

  // Update
  @PutMapping("/")
  public Board put(Board board) {
    log.info("실행");
    // 첨부파일까지 수정했는지 확인
    if (board.getBattach() != null && !board.getBattach().isEmpty()) {
      MultipartFile mf = board.getBattach();
      board.setBattachoname(mf.getOriginalFilename());
      board.setBattachsname(new Date().getTime() + "-" + mf.getOriginalFilename());
      board.setBattachtype(mf.getContentType());
      try {
        File file = new File("C:/Temp/uploadedfiles/" + board.getBattachsname());
        mf.transferTo(file);
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }
    boardService.updateBoard(board);
    return boardService.getBoard(board.getBno(), false);
  }

  // Delete
  @DeleteMapping("/{bno}")
  public Map<String, String> delete(@PathVariable int bno) {
    Map<String, String> map = new HashMap<>();
    int rows = boardService.removeBoard(bno);
    if (rows == 0) {
      map.put("result", FAIL_MESSAGE);
      return map;
    }
    map.put("result", SUCCESS_MESSAGE);
    return map;
  }
  
  // 첨부파일 다운로드
  @GetMapping("/battach/{bno}")
  public ResponseEntity<InputStreamResource> download(@PathVariable int bno) {
    Board board = boardService.getBoard(bno, false);
    if (board.getBattachoname() == null) return null;
    
    String battachoname = null;
    InputStreamResource resource = null;
    try {
      // 첨부파일이 있으면
      // 가져온 파일명이 한글인 경우:
      battachoname = new String(board.getBattachoname().getBytes("UTF-8"), "ISO-8859-1");
      File file = new File("C:/Temp/uploadedfiles/" + board.getBattachsname());
      FileInputStream fis = new FileInputStream(file);
      resource = new InputStreamResource(fis);
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    return ResponseEntity.ok()
           .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + battachoname + "\";")
           .header(HttpHeaders.CONTENT_TYPE, board.getBattachtype())
           .body(resource);
  }
}
