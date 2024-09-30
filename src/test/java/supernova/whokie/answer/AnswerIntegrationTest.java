package supernova.whokie.answer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import supernova.whokie.answer.repository.AnswerRepository;
import supernova.whokie.friend.Friend;
import supernova.whokie.friend.infrastructure.repository.FriendRepository;
import supernova.whokie.question.Question;
import supernova.whokie.question.repository.QuestionRepository;
import supernova.whokie.user.Gender;
import supernova.whokie.user.Role;
import supernova.whokie.user.Users;
import supernova.whokie.user.repository.UserRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.profiles.active=default",
        "jwt.secret=abcd"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AnswerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FriendRepository friendRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AnswerRepository answerRepository;

    @BeforeEach
    void setUp() {
        Users user = Users.builder()
                .name("Test User")
                .email("test@example.com")
                .point(0)
                .age(20)
                .kakaoId(1234567890L)
                .gender(Gender.M)
                .imageUrl("default_image_url.jpg")
                .role(Role.USER)
                .build();
        userRepository.save(user);

        for (int i = 1; i <= 5; i++) {
            Users friendUser = Users.builder()
                    .name("Friend " + i)
                    .email("friend" + i + "@example.com")
                    .point(0)
                    .age(20)
                    .kakaoId(1234567890L + i)
                    .gender(Gender.F)
                    .imageUrl("default_image_url_friend_" + i + ".jpg")
                    .role(Role.USER)
                    .build();

            userRepository.save(friendUser);
        }

        for (int i = 1; i <= 5; i++) {
            Users friendUser = userRepository.findById((long) i).orElseThrow();
            Friend friend = Friend.builder()
                    .hostUser(user)
                    .friendUser(friendUser)
                    .build();
            friendRepository.save(friend);
        }

        for (int i = 1; i <= 5; i++) {
            Question question = Question.builder()
                    .content("Test Question " + i)
                    .build();
            questionRepository.save(question);

            // 답변 설정
            Answer answer = Answer.builder()
                    .question(question)
                    .picker(user)
                    .picked(userRepository.findById(2L).orElseThrow())
                    .hintCount(0)
                    .build();
            ReflectionTestUtils.setField(answer, "createdAt", LocalDateTime.of(2024, 9, 19, 0, 0));
            answerRepository.save(answer);
        }

    }

    @Test
    @DisplayName("답변 목록 새로고침 테스트")
    void refreshAnswerListTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "1");

        mockMvc.perform(get("/api/answer/refresh")
                        .requestAttr("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andDo(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    System.out.println("users 내용: " + responseContent);
                });

    }

    @Test
    @DisplayName("공통 질문에 답변하기 테스트")
    void answerToCommonQuestionTest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", "1");

        Long questionId = 1L;
        Long pickedId = 2L;

        String requestBody = String.format("{\"questionId\": %d, \"pickedId\": %d}", questionId, pickedId);

        mockMvc.perform(post("/api/answer/common")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .requestAttr("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("답변 완료"));

    }

    @Test
    @DisplayName("전체 질문 기록 조회 테스트")
    void getAnswerRecordTest() throws Exception {
        mockMvc.perform(get("/api/answer/record")
                        .requestAttr("userId", "1")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andDo(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    System.out.println("전체 질문 기록 내용: " + responseContent);
                });
    }
}
