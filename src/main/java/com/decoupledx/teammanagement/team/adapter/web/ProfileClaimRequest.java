package com.decoupledx.teammanagement.team.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ProfileClaimRequest(
        @NotBlank @Size(max = 64) String claimToken) {
}
