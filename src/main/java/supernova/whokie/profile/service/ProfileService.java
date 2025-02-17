package supernova.whokie.profile.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import supernova.whokie.global.exception.EntityNotFoundException;
import supernova.whokie.profile.Profile;
import supernova.whokie.profile.infrastructure.ProfileRepository;
import supernova.whokie.profile.service.dto.ProfileModel;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileModel.Info getProfile(Long userId) {
        Profile profile = profileRepository.findByUsersId(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return ProfileModel.Info.from(profile);
    }
}
