package com.mycompany.backend.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.mycompany.backend.dto.Board;
import com.mycompany.backend.dto.Pager;

@Mapper
public interface BoardDao {
  public int count();
  // get list
  public List<Board> selectByPage(Pager pager);
  // create
  public int insert(Board board);
  // read
  public Board selectByBno(int bno);
  // update
  public int update(Board board);
  // 조회수 update
  public int updateBhitcount(int bno);
  // delete
  public int deleteByBno(int bno);
}
