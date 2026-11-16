package com.decoupledx.teammanagement.team.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

record PlayerProfileRegistrationRequest(
        @NotBlank @Size(max = 200) String fullName,

        @Size(max = 32) String phone,

        LocalDate birthDate,

        @Size(max = 2) String nationality) {
}
