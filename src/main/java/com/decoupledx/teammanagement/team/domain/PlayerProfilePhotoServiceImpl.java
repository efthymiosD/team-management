package com.decoupledx.teammanagement.team.domain;
import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.domain.port.CurrentPlayerProfile;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileDataValue;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfilePhotoService;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class PlayerProfilePhotoServiceImpl implements PlayerProfilePhotoService {

    private final CurrentPlayerProfile currentPlayer;
    private final PlayerProfileRepository profiles;
    private final Clock clock;

    @Override
    public PlayerProfileInfo applyMyPhoto(String photoPath) {
        // The upload endpoint already validated + stored the bytes; this unit
        // only binds the web path to the acting account's profile. Replacing
        // an existing photo is allowed; the old file is left on disk (ops
        // reaps the directory — never delete on the request path).
        PlayerProfileDataValue profile = currentPlayer.current();
        PlayerProfile changed = PlayerProfile.from(profile);
        changed.applyPhoto(photoPath, clock.instant());
        return TeamRegistrationServiceImpl.profileInfo(profiles.save(changed.toData()));
    }
}
