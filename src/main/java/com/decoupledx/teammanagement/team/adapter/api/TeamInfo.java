package com.decoupledx.teammanagement.team.adapter.api;

import java.util.UUID;

public record TeamInfo(UUID teamId, String name, String logoPath) {
}
