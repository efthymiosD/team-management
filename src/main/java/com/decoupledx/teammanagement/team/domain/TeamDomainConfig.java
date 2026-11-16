package com.decoupledx.teammanagement.team.domain;

import com.decoupledx.common.transaction.TransactionRunner;
import com.decoupledx.teammanagement.identity.adapter.api.AccountDirectoryApi;
import com.decoupledx.teammanagement.identity.adapter.api.CurrentAccountApi;
import com.decoupledx.teammanagement.team.domain.port.CreateTeamService;
import com.decoupledx.teammanagement.team.domain.port.CurrentPlayerProfile;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfilePhotoService;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileRepository;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfileService;
import com.decoupledx.teammanagement.team.domain.port.ProfileClaimService;
import com.decoupledx.teammanagement.team.domain.port.TeamAdministrationService;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamInviteService;
import com.decoupledx.teammanagement.team.domain.port.TeamLogoService;
import com.decoupledx.teammanagement.team.domain.port.TeamMembershipRepository;
import com.decoupledx.teammanagement.team.domain.port.TeamProfileManagementService;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import com.decoupledx.teammanagement.team.domain.port.TeamRegistrationService;
import com.decoupledx.teammanagement.team.domain.port.TeamRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Domain-internal wiring; nothing here is visible to other modules (ADR-0001
 * rule 3: the only framework annotation inside the domain package).
 */
@Configuration
class TeamDomainConfig {

    @Bean
    CreateTeamService createTeamService(CurrentPlayerProfile currentPlayerProfile,
            TeamRepository teams, TeamMembershipRepository memberships, TeamQueryService teamQuery,
            Clock clock, TransactionRunner tx) {
        return new CreateTeamServiceImpl(currentPlayerProfile, teams, memberships, teamQuery,
                clock, tx);
    }

    @Bean
    TeamQueryService teamQueryService(TeamPermissionsService permissions, TeamRepository teams,
            TeamMembershipRepository memberships, PlayerProfileRepository profiles,
            TeamViewsService views) {
        return new TeamQueryServiceImpl(permissions, teams, memberships, profiles, views);
    }

    @Bean
    TeamProfileManagementService teamProfileManagementService(TeamPermissionsService permissions,
            TeamRepository teams, TeamQueryService teamQuery, Clock clock, TransactionRunner tx) {
        return new TeamProfileManagementServiceImpl(permissions, teams, teamQuery, clock, tx);
    }

    @Bean
    TeamRegistrationService teamRegistrationService(TeamPermissionsService permissions,
            CurrentPlayerProfile currentPlayerProfile, TeamRepository teams,
            TeamMembershipRepository memberships, PlayerProfileRepository profiles,
            TeamViewsService views, Clock clock, TransactionRunner tx) {
        return new TeamRegistrationServiceImpl(permissions, currentPlayerProfile, teams,
                memberships, profiles, views, clock, tx);
    }

    @Bean
    TeamAdministrationService teamAdministrationService(TeamPermissionsService permissions,
            PlayerProfileRepository profiles, TeamMembershipRepository memberships,
            TeamViewsService views, TransactionRunner tx) {
        return new TeamAdministrationServiceImpl(permissions, profiles, memberships, views, tx);
    }

    @Bean
    TeamLogoService teamLogoService(TeamPermissionsService permissions, TeamRepository teams,
            TeamQueryService teamQuery, Clock clock, TransactionRunner tx) {
        return new TeamLogoServiceImpl(permissions, teams, teamQuery, clock, tx);
    }

    @Bean
    PlayerProfilePhotoService playerProfilePhotoService(CurrentPlayerProfile currentPlayerProfile,
            PlayerProfileRepository profiles, TeamViewsService views, Clock clock) {
        return new PlayerProfilePhotoServiceImpl(currentPlayerProfile, profiles, views, clock);
    }

    @Bean
    PlayerProfileService playerProfileService(CurrentPlayerProfile currentPlayerProfile,
            PlayerProfileRepository profiles, TeamViewsService views, Clock clock) {
        return new PlayerProfileServiceImpl(currentPlayerProfile, profiles, views, clock);
    }

    @Bean
    ProfileClaimService profileClaimService(CurrentAccountApi accounts, PlayerProfileRepository profiles,
            TeamMembershipRepository memberships, TeamRepository teams,
            TeamPermissionsService permissions, TeamViewsService views, Clock clock,
            TransactionRunner tx) {
        return new ProfileClaimServiceImpl(accounts, profiles, memberships, teams, permissions,
                views, clock, tx);
    }

    @Bean
    TeamViewsService teamViews(AccountDirectoryApi directory) {
        return new TeamViewsService(directory);
    }

    @Bean
    CurrentPlayerProfile currentPlayerProfile(CurrentAccountApi accounts,
            AccountDirectoryApi directory, PlayerProfileRepository profiles, Clock clock) {
        return new CurrentPlayerProfileImpl(accounts, directory, profiles, clock);
    }

    @Bean
    TeamPermissionsService teamPermissions(CurrentAccountApi accounts, PlayerProfileRepository profiles,
            TeamMembershipRepository memberships) {
        return new TeamPermissionsService(accounts, profiles, memberships);
    }

    @Bean
    TeamInviteService teamInviteService(TeamPermissionsService permissions,
            CurrentPlayerProfile currentPlayerProfile, TeamRepository teams,
            TeamMembershipRepository memberships, TeamInviteRepository invites,
            TeamViewsService views, Clock clock, TransactionRunner tx) {
        return new TeamInviteServiceImpl(permissions, currentPlayerProfile, teams, memberships,
                invites, views, clock, tx);
    }
}
