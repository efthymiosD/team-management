package com.decoupledx.teammanagement.team.domain.port;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for membership persistence. The membership PK race (joining
 * twice in parallel) converts to 409 inside the adapter; the admin locking
 * read powers the at-least-one-admin invariant in the membership service.
 */
public interface TeamMembershipRepository {

    TeamMembershipDataValue save(TeamMembershipDataValue membership);

    TeamMembershipDataValue find(UUID teamId, UUID profileId);

    List<TeamMembershipDataValue> findByTeam(UUID teamId);

    List<TeamMembershipDataValue> findByProfile(UUID profileId);

    /** Locks the team's ADMIN rows (SELECT FOR UPDATE) for the admin count check. */
    List<TeamMembershipDataValue> lockAdmins(UUID teamId);

    void delete(TeamMembershipDataValue membership);
}