package com.decoupledx.teammanagement.team.adapter.web;

import com.decoupledx.teammanagement.team.adapter.api.TeamWithMembersInfo;
import com.decoupledx.teammanagement.team.domain.port.TeamLogoService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Admin-only team media: replace the team logo (ADR-0002). */
@RestController
@RequiredArgsConstructor
class TeamLogoController {

    private static final String LOGO_PREFIX = "logo-";

    private final TeamLogoService logos;
    private final Uploads uploads;

    @PostMapping("/api/teams/{teamId}/logo")
    TeamWithMembersInfo applyLogo(@PathVariable UUID teamId,
                                  @RequestParam("logo") MultipartFile logo) {
        return logos.applyLogo(teamId, uploads.save(logo, LOGO_PREFIX));
    }
}
