package com.jobhunthub.jobhunthub.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.jobhunthub.jobhunthub.config.UserPrincipal;
import com.jobhunthub.jobhunthub.model.User;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class OAuth2Service {
    
    private final UserService userService;
    private final DefaultOAuth2UserService defaultService = new DefaultOAuth2UserService();
    private final OidcUserService oidcService = new OidcUserService();
    
    public OAuth2Service(UserService userService) {
        this.userService = userService;
    }
    
    // Handle OAuth2 authentication
    public OAuth2User handleOAuth2Authentication(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = defaultService.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        User domainUser = processOAuth2Request(oauth2User, provider);
        return new UserPrincipal(oauth2User, domainUser);
    }

    // Handle OIDC authentication
    public OidcUser handleOidcAuthentication(OidcUserRequest userRequest) {
        OidcUser oidcUser = oidcService.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        User domainUser = processOAuth2Request(oidcUser, provider);
        return new UserPrincipal(oidcUser, domainUser);
    }
    
    // Decide if the request is for linking or authenticating
    private User processOAuth2Request(OAuth2User oauth2User, String provider) {
        boolean isLinking = isLinkingRequest();
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        
        if (isLinking) {
            if (current != null && current.getPrincipal() instanceof UserPrincipal up) {
                User currentUser = up.getDomainUser();
                return userService.linkProviderToUser(currentUser, oauth2User, provider);
            } else {
                throw new IllegalStateException("Account linking requested but no authenticated user found.");
            }
        } else {
            return userService.authenticateUser(oauth2User, provider);
        }
    }
    
    // Helper method to check if the request is for linking
    private boolean isLinkingRequest() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            String state = request.getParameter("state");
            return "link".equals(state);
        } catch (Exception e) {
            return false;
        }
    }
}
