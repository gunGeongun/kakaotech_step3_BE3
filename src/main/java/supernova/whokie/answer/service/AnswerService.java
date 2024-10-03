package supernova.whokie.answer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import supernova.whokie.answer.Answer;
import supernova.whokie.answer.controller.dto.AnswerResponse;
import supernova.whokie.answer.repository.AnswerRepository;
import supernova.whokie.answer.service.dto.AnswerCommand;
import supernova.whokie.answer.service.dto.AnswerModel;
import supernova.whokie.friend.Friend;
import supernova.whokie.friend.infrastructure.repository.FriendRepository;
import supernova.whokie.global.dto.PagingResponse;
import supernova.whokie.global.exception.EntityNotFoundException;
import supernova.whokie.point_record.PointRecord;
import supernova.whokie.point_record.PointRecordOption;
import supernova.whokie.point_record.event.PointRecordEventDto;
import supernova.whokie.point_record.infrastructure.repository.PointRecordRepository;
import supernova.whokie.question.Question;
import supernova.whokie.question.repository.QuestionRepository;
import supernova.whokie.user.Users;
import supernova.whokie.user.repository.UserRepository;
import supernova.whokie.user.service.dto.UserModel;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerService {
    private final AnswerRepository answerRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final PointRecordRepository pointRecordRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${answer-point}")
    private int answerPoint;
    @Value("${default-hint-count}")
    private int defaultHintCount;
    @Value("${point-earn-message}")
    private String pointEarnMessage;

    @Transactional(readOnly = true)
    public PagingResponse<AnswerResponse.Record> getAnswerRecord(Pageable pageable, Long userId) {
        Users user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("해당 유저를 찾을 수 없습니다."));

        Page<Answer> answers = answerRepository.findAllByPicker(pageable, user);

        List<AnswerResponse.Record> answerResponse = answers.stream()
                .map(AnswerResponse.Record::from)
                .toList();

        return PagingResponse.from(new PageImpl<>(answerResponse, pageable, answers.getTotalElements()));
    }

    @Transactional
    public void answerToCommonQuestion(Long userId, AnswerCommand.CommonAnswer command) {
        Users user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("해당 유저를 찾을 수 없습니다."));
        Question question = questionRepository.findById(command.questionId())
                .orElseThrow(() -> new EntityNotFoundException("해당 질문을 찾을 수 없습니다."));
        Users picked = userRepository.findById(command.pickedId())
                .orElseThrow(() -> new EntityNotFoundException("해당 유저를 찾을 수 없습니다."));

        Answer answer = command.toEntity(question, user, picked, defaultHintCount);
        answerRepository.save(answer);

        user.increasePoint(answerPoint);
        eventPublisher.publishEvent(PointRecordEventDto.Earn.toDto(userId, answerPoint, 0, PointRecordOption.CHARGED, pointEarnMessage ));

    }

    public void recordEarnPoint(PointRecordEventDto.Earn event) {
        PointRecord pointRecord = PointRecord.create(event.userId(), event.point(), event.amount(), event.option(), event.message());
        pointRecordRepository.save(pointRecord);
    }

    @Transactional(readOnly = true)
    public AnswerModel.Refresh refreshAnswerList(Long userId) {
        Users user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("유저가 존재하지 않습니다."));
        List<Friend> allFriends = friendRepository.findAllByHostUser(user);

        List<UserModel.PickedInfo> friendsInfoList = allFriends.stream().map(
                friend -> UserModel.PickedInfo.from(friend.getFriendUser())
        ).toList();

        return AnswerModel.Refresh.from(friendsInfoList);
    }
}
