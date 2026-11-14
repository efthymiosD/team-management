package com.decoupledx.teammanagement.webui;

import com.decoupledx.common.error.BusinessException;
import com.decoupledx.teammanagement.team.adapter.api.TeamApi;
import com.decoupledx.teammanagement.team.adapter.api.TeamInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The home page: for a signed-in member, the "my teams" list plus the
 * create-team form; for an anonymous visitor, the login call to action.
 * Consumes the team module API (ADR-0001 rule 5: webui never touches domain
 * ports or adapters of other modules). Name validation + duplicate-name
 * handling are the domain's; flashes are translated via {@link WebMessages}.
 */
@Controller
@RequiredArgsConstructor
class HomeWebController {

    private final TeamApi teamApi;
    private final WebMessages messages;

    @GetMapping("/")
    String home(Model model, @RequestParam(required = false) String loginFailed) {
        var currentUser = currentUserFrom(model);
        if (currentUser.authenticated()) {
            List<TeamInfo> myTeams = teamApi.myTeams();
            model.addAttribute("myTeams", myTeams);
            model.addAttribute("teamCount", myTeams.size());
        }
        if (loginFailed != null) {
            model.addAttribute("error", messages.get("home.loginFailed"));
        }
        return "home";
    }

    @PostMapping("/teams")
    String createTeam(@RequestParam String name, RedirectAttributes redirect) {
        try {
            teamApi.create(name.strip());
            redirect.addFlashAttribute("message", messages.get("home.teamCreated"));
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", messages.errorMessage(exception));
        }
        return "redirect:/";
    }

    private com.decoupledx.teammanagement.webui.CurrentUser currentUserFrom(Model model) {
        return (com.decoupledx.teammanagement.webui.CurrentUser)
                model.getAttribute("currentUser");
    }
}
