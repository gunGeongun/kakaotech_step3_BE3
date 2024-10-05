package supernova.whokie.question.service.dto;

import lombok.Builder;
import supernova.whokie.question.Question;
import supernova.whokie.question.QuestionStatus;
import supernova.whokie.user.Users;

public class QuestionCommand {

    @Builder
    public record Create(
        Long groupId,
        String content
    ) {

        public Question toEntity(Users user) {
            return Question.builder()
                .content(content)
                .questionStatus(QuestionStatus.READY)
                .groupId(groupId)
                .writer(user)
                .build();
        }
    }
}
