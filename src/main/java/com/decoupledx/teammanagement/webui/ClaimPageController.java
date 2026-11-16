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
 * The claim page per approved mockup §12.2 (login-first) with the §13
 * per-member deeplink shape: /teams/{teamId}/members/{profileId}/claim/
 * {token} — the path names the player; the RANDOM token is the sole
 * authority and must belong to that exact team+profile. Claiming binds the
 * acting account — a fresh account keeps the pending profile (the
 * completeness gate then asks for any missing info), an account with an
 * existing profile merges (their profile wins) and lands on the team page.
 */
@Controller
@RequiredArgsConstructor
class ClaimPageController {

    private final TeamApi teamApi;
    private final WebMessages messages;

    @GetMapping("/teams/{teamId}/members/{profileId}/claim/{token}")
    String claimPage(@PathVariable UUID teamId, @PathVariable UUID profileId,
            @PathVariable String token, Model model, RedirectAttributes redirect) {
        try {
            var preview = teamApi.claimPreview(token);
            if (!preview.teamId().equals(teamId) || !preview.profileId().equals(profileId)) {
                // The deeplink names a different player/team than the token.
                return "invalid-link";
            }
            model.addAttribute("preview", preview);
            model.addAttribute("token", token);
            model.addAttribute("teamId", teamId);
            model.addAttribute("profileId", profileId);
            if (preview.teamLogoPath() == null) {
                model.addAttribute("teamInitial",
                        preview.teamName().substring(0, 1).toUpperCase());
            }
            if (preview.phone() != null) {
                model.addAttribute("phoneDisplay",
                        PhoneDisplay.display(preview.phone()));
            }
        } catch (BusinessException exception) {
            return "invalid-link";
        }
        return "claim";
    }

    @PostMapping("/teams/{teamId}/members/{profileId}/claim/{token}")
    String claim(@PathVariable UUID teamId, @PathVariable UUID profileId,
            @PathVariable String token, RedirectAttributes redirect) {
        try {
            // The POST applies the same path↔token consistency the GET
            // enforces (recorded agreement: both directions validated).
            var preview = teamApi.claimPreview(token);
            if (!preview.teamId().equals(teamId) || !preview.profileId().equals(profileId)) {
                return "invalid-link";
            }
            teamApi.claim(token);
            redirect.addFlashAttribute("message", messages.get("claim.succeeded"));
            return "redirect:/players/me";
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
            return "redirect:/";
        }
    }
}
