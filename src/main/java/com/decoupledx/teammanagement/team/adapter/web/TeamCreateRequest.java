package com.decoupledx.teammanagement.team.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record TeamCreateRequest(
        @NotBlank @Size(max = 200) String name) {
}
