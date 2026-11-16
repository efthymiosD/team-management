package com.decoupledx.teammanagement.team.domain;

import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.domain.port.CurrentPlayerProfile;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileService;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class PlayerProfileServiceImpl implements PlayerProfileService {

    private final CurrentPlayerProfile currentPlayer;
    private final PlayerProfileRepository profiles;
    private final TeamViewsService views;
    private final Clock clock;

    @Override
    public PlayerProfileInfo applyMyProfile(String fullName, String phone, LocalDate birthDate,
            String nationality) {
        PlayerProfileDataValue profile = currentPlayer.current();
        PlayerProfile changed = PlayerProfile.from(profile);
        changed.updateDetails(fullName, phone, birthDate, nationality, clock.instant());
        return views.profileInfo(profiles.save(changed.toData()));
    }
}
