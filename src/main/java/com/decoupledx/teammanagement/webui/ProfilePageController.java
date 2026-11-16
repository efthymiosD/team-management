package com.decoupledx.teammanagement.webui;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.common.i18n.Countries;
import com.decoupledx.common.i18n.WebMessages;
import com.decoupledx.common.web.Uploads;
import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.adapter.api.TeamApi;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * "My profile" page (approved mockup §6): the acting account's player profile
 * (provisioned on first visit — the profile-creation flow) and ONE form that
 * persists the WHOLE visible state on every submit — the full self-service
 * field set (name editable, phone required + validated, optional birthday,
 * nationality via the searchable country dropdown) plus the OPTIONAL photo
 * part. A photo-only submit therefore never discards pending detail edits
 * (owner feedback: separate photo/detail forms lost unsaved changes).
 * The email is shown read-only. Consumes the team module API; the upload
 * bytes are validated + stored by the web layer first (ADR-0002).
 */
@Controller
@RequiredArgsConstructor
class ProfilePageController {

    private static final String PHOTO_PREFIX = "photo-";

    private final TeamApi teamApi;
    private final WebMessages messages;
    private final Uploads uploads;
    private final Countries countries;

    @GetMapping("/players/me")
    String profile(Model model, RedirectAttributes redirect) {
        try {
            PlayerProfileInfo profile = teamApi.myProfile();
            model.addAttribute("profile", profile);
            model.addAttribute("countries", countries);
            if (profile.nationality() != null) {
                model.addAttribute("nationalityName",
                        countries.name(profile.nationality()));
            }
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
            return "redirect:/";
        }
        return "profile";
    }

    @PostMapping("/players/me/profile")
    String updateProfile(@RequestParam String fullName, @RequestParam String phone,
            @RequestParam(required = false)
            @DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate birthDate,
            @RequestParam(required = false) String nationality,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            RedirectAttributes redirect) {
        try {
            // never a half-applied profile (owner feedback).
            String photoPath = photo != null && !photo.isEmpty()
                    ? uploads.save(photo, ProfilePageController.PHOTO_PREFIX)
                    : null;
            teamApi.applyMyProfile(fullName, phone, birthDate, nationality);
            if (photoPath != null) {
                teamApi.applyMyPhoto(photoPath);
            }
            redirect.addFlashAttribute("message", messages.get("profile.saved"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
            redirect.addFlashAttribute("profileForm", new ProfileForm(fullName, phone,
                    birthDate == null ? null : birthDate.toString(), nationality));
        }
        return "redirect:/players/me";
    }
}
