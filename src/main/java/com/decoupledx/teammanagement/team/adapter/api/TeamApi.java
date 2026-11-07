package com.decoupledx.teammanagement.team.adapter.api;

import java.util.List;
import java.util.UUID;

/**
 * Module API of the team module (the single cross-module surface): team
 * reads. Mutations stay on inbound domain ports driven by this module's web
 * adapters (ADR-0001 rule 9).
 */
public interface TeamApi {

    TeamWithMembersInfo team(UUID teamId);

    List<TeamInfo> myTeams();
}
