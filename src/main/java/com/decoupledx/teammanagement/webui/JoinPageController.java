package com.decoupledx.teammanagement.webui;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.i18n.WebMessages;
import com.decoupledx.teammanagement.team.adapter.api.TeamApi;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The join flow per approved mockup §12: joining happens ONLY through a
 * one-time invite link — the hero shows the team identity (joinPreview,
 * un-gated) and the POST consumes the token atomically. The legacy open
 * link and any used/revoked/unknown token render the invalid-link page
 * (§12.3) — no oracle between revoked and unknown.
 */
@Controller
@RequiredArgsConstructor
class JoinPageController {

    private final TeamApi teamApi;
    private final WebMessages messages;

    @GetMapping("/teams/{teamId}/join")
    String legacyJoin(@PathVariable UUID teamId) {
        return "invalid-link";
    }

    @GetMapping("/teams/{teamId}/join/{token}")
    String joinPage(@PathVariable UUID teamId, @PathVariable String token, Model model,
            RedirectAttributes redirect) {
        // Cheap validation for a friendly page: team identity + the token
        // known and unused (the authoritative consume happens on POST).
        if (!teamApi.inviteActive(teamId, token)) {
            return "invalid-link";
        }
        try {
            model.addAttribute("team", teamApi.joinPreview(teamId));
            model.addAttribute("token", token);
        } catch (BusinessException exception) {
            return "invalid-link";
        }
        return "join";
    }

    @PostMapping("/teams/{teamId}/join/{token}")
    String join(@PathVariable UUID teamId, @PathVariable String token,
            RedirectAttributes redirect) {
        try {
            teamApi.joinByInvite(teamId, token);
            redirect.addFlashAttribute("message", messages.get("team.joined"));
            return "redirect:/teams/" + teamId;
        } catch (BusinessException exception) {
            if (exception.errorCode().name().equals("ALREADY_MEMBER")) {
                return "redirect:/teams/" + teamId;
            }
            return "invalid-link";
        }
    }
}
