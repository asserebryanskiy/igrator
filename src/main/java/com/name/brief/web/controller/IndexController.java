package com.name.brief.web.controller;

import com.name.brief.config.authentication.PlayerAuthenticationFilter;
import com.name.brief.model.Player;
import com.name.brief.model.Role;
import com.name.brief.model.User;
import com.name.brief.service.PlayerAuthenticationService;
import com.name.brief.web.dto.PlayerLoginDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Arrays;

@Controller
public class IndexController {

    private final PlayerAuthenticationService playerAuthenticationService;
    private final RememberMeServices playerRememberMeServices;

    @Autowired
    public IndexController(PlayerAuthenticationService playerAuthenticationService,
                           RememberMeServices playerRememberMeServices) {
        this.playerAuthenticationService = playerAuthenticationService;
        this.playerRememberMeServices = playerRememberMeServices;
    }

    @RequestMapping("/")
    public String getMain(HttpServletRequest request,
                          HttpServletResponse response,
                          Model model,
                          Principal principal) {
        // if user is already authenticated redirect it to appropriate page
        String redirectPath = getRedirectPathIfAuthenticated((Authentication) principal, request, response);
        if (redirectPath != null) return redirectPath;

        addFlashAttribute(PlayerAuthenticationFilter.DTO_ATTRIBUTE_NAME, model, request.getSession());
        addFlashAttribute(PlayerAuthenticationFilter.ERRORS_ATTRIBUTE_NAME, model, request.getSession());

        if (!model.containsAttribute("playerLoginDto")) {
            model.addAttribute("playerLoginDto", new PlayerLoginDto());
        }

        return "index";
    }

    private String getRedirectPathIfAuthenticated(Authentication authentication,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        if (authentication != null) {
            Object user = authentication.getPrincipal();
            // if player is already authenticated redirect him to game
            if (user instanceof Player
                    && playerAuthenticationService.isLoggedIn((Player) authentication.getPrincipal())) {
                return "redirect:/game";
            }
            // if moderator is authenticated redirect it to index
            if (user instanceof User && ((User) user).getRole().equals(Role.MODERATOR.getRole())) {
                return "redirect:/moderator";
            }
        }

        return null;
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String getPlayerDetailsPage(Model model, HttpServletRequest request) {
        addFlashAttribute(PlayerAuthenticationFilter.DTO_ATTRIBUTE_NAME, model, request.getSession());
        addFlashAttribute(PlayerAuthenticationFilter.ERRORS_ATTRIBUTE_NAME, model, request.getSession());

        if (!model.containsAttribute("playerLoginDto")) return "redirect:/";
        return "playerDetails";
    }

    private void addFlashAttribute(String attr, Model model, HttpSession session) {
        Object obj = session.getAttribute(attr);
        if (obj != null) {
            model.addAttribute(attr, obj);
            session.removeAttribute(attr);
        }
    }
}
