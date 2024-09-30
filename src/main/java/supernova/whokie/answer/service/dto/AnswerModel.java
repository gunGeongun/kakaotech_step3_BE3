package supernova.whokie.answer.service.dto;

import lombok.Builder;
import supernova.whokie.user.service.dto.UserModel;

import java.util.List;

public class AnswerModel {

    @Builder
    public record Refresh(
            List<UserModel.PickedInfo> users

    ) {
        public static AnswerModel.Refresh from(List<UserModel.PickedInfo> friendsInfoList) {
            return Refresh.builder()
                    .users(friendsInfoList)
                    .build();
        }

    }
}
