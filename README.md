# album-board-springboot
### [서버] 스프링부트를 이용한 앨범게시판 만들기 미니 프로젝트

(프론트엔드 코드가 궁금하다면 이 프로젝트의 [프론트엔드 저장소](https://github.com/heejaykong/album-board-vue)를 확인해 주세요.)

1. 사용한 스택: **Spring Boot, Oracle, MyBatis, Spring Security, JWT인증, Redis**

2. API 개요:
<table>
  <thead>
    <tr>
      <th>URI</th>
      <th>HTTP<br/>메소드</th>
      <th>설명</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>/board/list?pageNo=1919</td>
      <td>GET</td>
      <td>게시글 전체 조회(페이징적용)</td>
    </tr>
    <tr>
      <td>/board/</td>
      <td>POST</td>
      <td>게시글 등록</td>
    </tr>
    <tr>
      <td>/board/:id</td>
      <td>GET</td>
      <td>게시글 bno로 조회</td>
    </tr>
    <tr>
      <td>/board/</td>
      <td>PUT</td>
      <td>게시글 수정</td>
    </tr>
    <tr>
      <td>/board/:id</td>
      <td>DELETE</td>
      <td>게시글 삭제</td>
    </tr>
  </tbody>
</table>

3. 구조: **Controller <-> Service <-> DAO <-> MyBatis mapper <-> Oracle**
