package com.decoupledx.teammanagement.team.domain.port;

import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import java.time.LocalDate;

/**
 * The acting account manages ITS OWN player profile — the full self-service
 * field set (name, phone, birthday, nationality). "me" variant: the profile
 * is resolved inside (ADR-0002) and provisioned on first use. Phone is
 * required and validated (liberal international format); nationality is an
 * optional ISO 3166-1 alpha-2 code.
 */
public interface PlayerProfileService {

    PlayerProfileInfo applyMyProfile(String fullName, String phone, LocalDate birthDate,
            String nationality);
}
