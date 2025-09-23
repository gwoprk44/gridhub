package com.gridhub.gridhub.domain.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gridhub.gridhub.domain.comment.dto.CommentCreateRequest;
import com.gridhub.gridhub.domain.comment.dto.CommentUpdateRequest;
import com.gridhub.gridhub.domain.comment.entity.Comment;
import com.gridhub.gridhub.domain.comment.repository.CommentRepository;
import com.gridhub.gridhub.domain.post.entity.Post;
import com.gridhub.gridhub.domain.post.entity.PostCategory;
import com.gridhub.gridhub.domain.post.repository.PostRepository;
import com.gridhub.gridhub.domain.user.entity.User;
import com.gridhub.gridhub.domain.user.entity.UserRole;
import com.gridhub.gridhub.domain.user.repository.UserRepository;
import com.gridhub.gridhub.global.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private JwtUtil jwtUtil;

    private String userToken;
    private Post testPost;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
        commentRepository.deleteAllInBatch();

        User user = User.builder().email("user@test.com").password("encoded").nickname("user").role(UserRole.USER).build();
        userRepository.save(user);
        userToken = jwtUtil.createToken(user.getEmail(), user.getRole());

        testPost = Post.builder().title("test post").content("test content").category(PostCategory.FREE).author(user).build();
        postRepository.save(testPost);
    }

    @DisplayName("댓글 및 대댓글 생성 후 계층 구조 조회")
    @Test
    void createAndGetComments_Hierarchical_Success() throws Exception {
        // given
        // 1. 부모 댓글 생성
        CommentCreateRequest parentRequest = new CommentCreateRequest("parent comment", null);
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/comments")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentRequest)))
                .andExpect(status().isCreated());

        Comment parentComment = commentRepository.findAll().get(0);

        // 2. 자식 댓글(대댓글) 생성
        CommentCreateRequest childRequest = new CommentCreateRequest("child comment", parentComment.getId());
        mockMvc.perform(post("/api/posts/" + testPost.getId() + "/comments")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childRequest)))
                .andExpect(status().isCreated());

        // when & then
        // 3. 전체 댓글 목록 조회
        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(parentComment.getId())) // 첫 번째는 부모 댓글
                .andExpect(jsonPath("$[0].content").value("parent comment"))
                .andExpect(jsonPath("$[0].replies[0].content").value("child comment")) // 부모 댓글의 replies에 자식 댓글이 있는지 확인
                .andDo(print());
    }

    @DisplayName("댓글 수정 성공")
    @Test
    void updateComment_Success() throws Exception {
        // given
        Comment comment = Comment.builder().content("original").post(testPost).author(testPost.getAuthor()).build();
        commentRepository.save(comment);

        CommentUpdateRequest updateRequest = new CommentUpdateRequest("updated content");

        // when & then
        mockMvc.perform(put("/api/posts/" + testPost.getId() + "/comments/" + comment.getId())
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @DisplayName("부모 댓글 삭제 (소프트 삭제) 후 자식 댓글 유지 확인")
    @Test
    void deleteParentComment_SoftDelete_And_CheckChildren() throws Exception {
        // given
        Comment parent = Comment.builder().content("parent").post(testPost).author(testPost.getAuthor()).build();
        commentRepository.save(parent);
        Comment child = Comment.builder().content("child").post(testPost).author(testPost.getAuthor()).parent(parent).build();
        commentRepository.save(child);

        // when
        // 1. 부모 댓글 삭제 (자식이 있으므로 소프트 삭제)
        mockMvc.perform(delete("/api/posts/" + testPost.getId() + "/comments/" + parent.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isNoContent());

        // then
        // 2. 목록 조회 시, 부모는 "삭제된 댓글"로, 자식은 그대로 보여야 함
        mockMvc.perform(get("/api/posts/" + testPost.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("[삭제된 댓글입니다.]"))
                .andExpect(jsonPath("$[0].replies[0].content").value("child"))
                .andDo(print());
    }
}