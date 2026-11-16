package com.decoupledx.teammanagement.team.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record TeamRenameRequest(
        @NotBlank @Size(max = 200) String name) {
}
