package com.decoupledx.teammanagement.team.adapter.api;

import com.decoupledx.teammanagement.team.domain.port.CreateTeamService;
import com.decoupledx.teammanagement.team.domain.port.TeamQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TeamApiConfig {

    @Bean
    TeamApi teamApi(CreateTeamService createTeamService, TeamQueryService teamQuery) {
        return new TeamApiImpl(createTeamService, teamQuery);
    }
}
