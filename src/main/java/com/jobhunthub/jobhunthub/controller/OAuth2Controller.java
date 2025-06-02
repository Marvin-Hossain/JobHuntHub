package com.jobhunthub.jobhunthub.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobhunthub.jobhunthub.config.UserPrincipal;
import com.jobhunthub.jobhunthub.dto.AuthenticatedUserDTO;
import com.jobhunthub.jobhunthub.service.UserService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class OAuth2Controller {

    private final UserService userService;

    public OAuth2Controller(UserService userService) {
        this.userService = userService;
    }

    // Get the current user's authentication status
    @GetMapping("/user")
    public ResponseEntity<AuthenticatedUserDTO> currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.ok(new AuthenticatedUserDTO(false));
        }
        
        AuthenticatedUserDTO userDTO = userService.getAuthenticatedUserDTO(principal.getDomainUser());
        return ResponseEntity.ok(userDTO);
    }

    // Initiate OAuth flow for linking a provider
    @GetMapping("/link/{provider}")
    public ResponseEntity<?> initiateProviderLinking(
            @PathVariable String provider, 
            HttpServletResponse response) throws IOException {
        
        if (!provider.equals("github") && !provider.equals("google")) {
            return ResponseEntity.badRequest().body("Unsupported provider: " + provider);
        }
        
        String redirectUrl = "/oauth2/authorization/" + provider + "?state=link";
        response.sendRedirect(redirectUrl);
        return ResponseEntity.ok().build();
    }
}
