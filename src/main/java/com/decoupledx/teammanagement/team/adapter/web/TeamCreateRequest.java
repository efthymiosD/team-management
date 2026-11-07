package com.decoupledx.teammanagement.team.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record TeamCreateRequest(
        // 200 = Team.NAME_MAX_LENGTH (kept in sync with the DB check constraint)
        @NotBlank @Size(max = 200) String name) {
}
