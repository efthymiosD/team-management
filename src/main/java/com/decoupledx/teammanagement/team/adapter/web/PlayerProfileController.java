package com.decoupledx.teammanagement.team.adapter.web;

import com.decoupledx.teammanagement.team.adapter.api.PlayerProfileInfo;
import com.decoupledx.teammanagement.team.domain.port.PlayerProfilePhotoService;
import com.decoupledx.teammanagement.team.domain.port.ProfileClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * "me"-scoped profile flows: photo upload (the acting account's own profile)
 * and claim redemption (the invite token binds the acting account to the
 * unclaimed player profile). Pure delegation (ADR-0002: no threading of the
 * account by adapters; the upload validates + stores first, the domain only
 * ever sees the web path).
 */
@RestController
@RequiredArgsConstructor
class PlayerProfileController {

    private static final String PHOTO_PREFIX = "photo-";

    private final PlayerProfilePhotoService photos;
    private final ProfileClaimService claims;
    private final Uploads uploads;

    @PostMapping("/api/players/me/photo")
    PlayerProfileInfo uploadMyPhoto(@RequestParam("photo") MultipartFile photo) {
        return photos.applyMyPhoto(uploads.save(photo, PHOTO_PREFIX));
    }

    @PostMapping("/api/claims")
    PlayerProfileInfo claim(@Valid @RequestBody ProfileClaimRequest request) {        return claims.claim(request.claimToken());
    }
}
