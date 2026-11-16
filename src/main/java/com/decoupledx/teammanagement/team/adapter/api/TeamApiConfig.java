package com.decoupledx.teammanagement.team.adapter.api;

import com.decoupledx.teammanagement.team.domain.port.CreateTeamService;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfilePhotoService;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileService;
import com.decoupledx.teammanagement.team.domain.port.ProfileClaimService;
import com.decoupledx.teammanagement.team.domain.port.TeamAdministrationService;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteService;
import com.decoupledx.teammanagement.team.domain.port.TeamLogoService;
import com.decoupledx.teammanagement.team.domain.port.TeamProfileManagementService;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import com.decoupledx.teammanagement.team.domain.port.TeamRegistrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TeamApiConfig {

    @Bean
    TeamApi teamApi(CreateTeamService createTeamService, TeamQueryService teamQuery,
            TeamRegistrationService registrations, PlayerProfilePhotoService photos,
            PlayerProfileService profiles, TeamLogoService logos,
            TeamAdministrationService administrations,
            TeamProfileManagementService profileManagement, TeamInviteService invites,
            ProfileClaimService claims) {
        return new TeamApiImpl(createTeamService, teamQuery, registrations, photos, profiles,
                logos, administrations, profileManagement, invites, claims);
    }
}
