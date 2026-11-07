package com.decoupledx.teammanagement.team.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record PlayerProfileRegistrationRequest(
        // 200 = PlayerProfile.NAME_MAX_LENGTH (synced with the DB check constraint)
        @NotBlank @Size(max = 200) String fullName,

        @Size(max = 2000) String contactDetails,

        // 100 = PlayerProfile.NATIONALITY_MAX_LENGTH
        @Size(max = 100) String nationality) {
}
