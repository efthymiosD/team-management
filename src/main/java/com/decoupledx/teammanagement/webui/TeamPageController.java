package com.decoupledx.teammanagement.webui;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.i18n.Countries;
import com.decoupledx.common.i18n.WebMessages;
import com.decoupledx.common.web.Uploads;
import com.decoupledx.teammanagement.team.adapter.api.TeamApi;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The team details page (approved mockup §1): team identity header (logo,
 * name, member count, creation date), the players GRID (photo + name +
 * nationality flag; each card links to the member's profile page — mockup
 * §2), per-card admin controls, and the invite section with the FULL
 * absolute join link + copy/share (mockup §4). Consumes the team module API;
 * business errors surface as translated flashes (WebMessages).
 */
@Controller
@RequiredArgsConstructor
class TeamPageController {

    private static final String LOGO_PREFIX = "logo-";

    private final TeamApi teamApi;
    private final WebMessages messages;
    private final Uploads uploads;
    private final Countries countries;
    private final Clock clock;

    /** Configured base URL so the invite link shows scheme + host (decision 7). */
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @GetMapping("/teams/{teamId}")
    String team(@PathVariable UUID teamId, Model model, RedirectAttributes redirect) {
        try {
            var team = teamApi.team(teamId);
            model.addAttribute("team", team);
            model.addAttribute("countries", countries);
            model.addAttribute("adminCount",
                    team.members().stream().filter(m -> m.admin()).count());
            // Birthday cake (owner feedback): members whose BIRTHDAY is today
            var today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
            var birthdayIds = team.members().stream()
                    .filter(m -> isBirthday(m.birthDate(), today))
                    .map(m -> m.profileId().toString())
                    .collect(Collectors.toSet());
            model.addAttribute("birthdayIds", birthdayIds);
            // §12.1: the invite card lists the ACTIVE one-time invites (no
            // raw tokens — hash-only storage); the mint flash carries the
            // freshly minted link exactly once.
            if (team.viewerIsAdmin()) {
                model.addAttribute("activeInvites",
                        teamApi.activeInvites(teamId).stream()
                                .map(invite -> java.util.Map.of(
                                        "inviteId", invite.inviteId().toString(),
                                        "createdAt", formattedDate(
                                                java.time.LocalDate.ofInstant(invite.createdAt(),
                                                        java.time.ZoneOffset.UTC).toString())))
                                .toList());
            }
            model.addAttribute("createdAt", DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(org.springframework.context.i18n.LocaleContextHolder.getLocale())
                    .format(LocalDate.ofInstant(team.createdAt(),
                            ZoneOffset.UTC)));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
            return "redirect:/";
        }
        return "team";
    }

    /**
     * Another member's profile page (mockup §2): read-only, reached from the
     * grid; contact details appear only if the domain's privacy rule allowed
     * them (team admins, the member themself). Teammates-only — the module
     * rejects outsiders with NOT_TEAM_MEMBER.
     */
    @GetMapping("/teams/{teamId}/members/{profileId}")
    String member(@PathVariable UUID teamId, @PathVariable UUID profileId, Model model,
            RedirectAttributes redirect) {
        try {
            var member = teamApi.memberProfile(teamId, profileId);
            model.addAttribute("member", member);
            model.addAttribute("teamId", teamId);
            if (member.nationality() != null) {
                model.addAttribute("nationalityName", countries.name(member.nationality()));
                model.addAttribute("nationalityFlag", Countries.flagOf(member.nationality()));
            }
            if (member.birthDate() != null) {
                // Locale-format HERE (never #temporals on a String: 500s);
                // the age renders next to the birthday (owner feedback).
                LocalDate birth = LocalDate.parse(member.birthDate());
                model.addAttribute("memberBirthDate", formattedDate(member.birthDate()));
                long age = Period.between(birth,
                        LocalDate.ofInstant(clock.instant(),
                                ZoneOffset.UTC)).getYears();
                model.addAttribute("memberAge", messages.get("member.age", age));
            }
            if (member.phone() != null) {
                // Readable spacing + a clickable tel: link (owner feedback).
                model.addAttribute("memberPhoneDisplay", PhoneDisplay.display(member.phone()));
                model.addAttribute("memberPhoneHref", PhoneDisplay.href(member.phone()));
            }
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
            return "redirect:/teams/" + teamId;
        }
        return "member";
    }

    /**
     * ONE submit for the team header's staged changes (owner feedback: a
     * name edit + a picked logo were lost when only one form was saved):
     * applies BOTH the optional new logo and the rename, with ALL validation
     * before ANYTHING is applied — never a half-applied team.
     */
    @PostMapping("/teams/{teamId}/details")
    String updateDetails(@PathVariable UUID teamId, @RequestParam String name,
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            RedirectAttributes redirect) {
        try {
            String logoPath = logo != null && !logo.isEmpty()
                    ? uploads.save(logo, LOGO_PREFIX)
                    : null;
            teamApi.renameTeam(teamId, name.strip());
            if (logoPath != null) {
                teamApi.applyLogo(teamId, logoPath);
            }
            redirect.addFlashAttribute("message", messages.get("team.saved"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
            redirect.addFlashAttribute("teamRenameDraft", name);
        }
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/teams/{teamId}/invites")
    String mintInvite(@PathVariable UUID teamId, RedirectAttributes redirect) {
        try {
            var invite = teamApi.mintInvite(teamId);
            redirect.addFlashAttribute("inviteLink",
                    baseUrl + "/teams/" + teamId + "/join/" + invite.rawToken());
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/teams/{teamId}/invites/{inviteId}/revoke")
    String revokeInvite(@PathVariable UUID teamId, @PathVariable UUID inviteId,
            RedirectAttributes redirect) {
        try {
            teamApi.revokeInvite(teamId, inviteId);
            redirect.addFlashAttribute("message", messages.get("team.invite.revoked"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/teams/{teamId}/members")
    String addMember(@PathVariable UUID teamId, @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate birthDate,
            @RequestParam(required = false) String nationality,
            RedirectAttributes redirect) {
        try {
            var member = teamApi.registerMember(teamId, fullName, phone, birthDate, nationality);
            redirect.addFlashAttribute("claimLink", baseUrl + "/teams/" + teamId + "/members/"
                    + member.profileId() + "/claim/" + member.claimToken());
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
            redirect.addFlashAttribute("memberDraft", new MemberDraft(fullName, phone,
                    birthDate == null ? null : birthDate.toString(), nationality));
        }
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/teams/{teamId}/members/{profileId}/claim-link")
    String regenerateClaimLink(@PathVariable UUID teamId, @PathVariable UUID profileId,
            @RequestParam(defaultValue = "tile") String source, // whitelisted below
            RedirectAttributes redirect) {
        if (!"member".equals(source)) {
            source = "tile"; // any other value must not swallow the flash display
        }
        try {
            var member = teamApi.regenerateClaimLink(teamId, profileId);
            redirect.addFlashAttribute("claimLink",
                    baseUrl + "/teams/" + teamId + "/members/" + profileId + "/claim/"
                            + member.claimToken());
            redirect.addFlashAttribute("claimLinkFor", profileId.toString());
            redirect.addFlashAttribute("claimLinkName", member.fullName());
            redirect.addFlashAttribute("claimLinkSource", source);
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/teams/{teamId}/logo")
    String applyLogo(@PathVariable UUID teamId, @RequestParam("logo") MultipartFile logo,
            RedirectAttributes redirect) {
        try {
            teamApi.applyLogo(teamId, uploads.save(logo, LOGO_PREFIX));
            redirect.addFlashAttribute("message", messages.get("team.logoUpdated"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/teams/{teamId}/members/{profileId}/promote")
    String promote(@PathVariable UUID teamId, @PathVariable UUID profileId, RedirectAttributes redirect) {
        try {
            teamApi.promote(teamId, profileId);
            redirect.addFlashAttribute("message", messages.get("team.memberPromoted"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/teams/{teamId}/members/{profileId}/demote")
    String demote(@PathVariable UUID teamId, @PathVariable UUID profileId, RedirectAttributes redirect) {
        try {
            teamApi.demote(teamId, profileId);
            redirect.addFlashAttribute("message", messages.get("team.memberDemoted"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/teams/" + teamId;
    }

    @PostMapping("/teams/{teamId}/members/{profileId}/remove")
    String remove(@PathVariable UUID teamId, @PathVariable UUID profileId, RedirectAttributes redirect) {
        try {
            teamApi.removeMember(teamId, profileId);
            redirect.addFlashAttribute("message", messages.get("team.memberRemoved"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/teams/" + teamId;
    }

    /** ISO string → locale-formatted date (raw fallback for odd values). */
    private static String formattedDate(String isoDate) {
        try {
            return DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(org.springframework.context.i18n.LocaleContextHolder.getLocale())
                    .format(LocalDate.parse(isoDate));
        } catch (java.time.format.DateTimeParseException malformed) {
            return isoDate;
        }
    }

    /** Same month + day as today (any year) — the birthday badge rule. */
    private static boolean isBirthday(String isoBirthDate, LocalDate today) {
        if (isoBirthDate == null) {
            return false;
        }
        try {
            var birth = LocalDate.parse(isoBirthDate);
            return birth.getMonth() == today.getMonth()
                    && birth.getDayOfMonth() == today.getDayOfMonth();
        } catch (java.time.format.DateTimeParseException malformed) {
            return false;
        }
    }
}
