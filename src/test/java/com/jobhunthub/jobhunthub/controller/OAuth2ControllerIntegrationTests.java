package com.jobhunthub.jobhunthub.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.jobhunthub.jobhunthub.config.UserPrincipal;
import com.jobhunthub.jobhunthub.model.Profile;
import com.jobhunthub.jobhunthub.model.User;
import com.jobhunthub.jobhunthub.repository.ProfileRepository;
import com.jobhunthub.jobhunthub.repository.UserRepository;

/**
 * Integration tests for OAuth2Controller.
 * Tests OAuth2 authentication flow and provider linking:
 * - Current user authentication status
 * - Provider linking functionality
 * - Error handling for invalid requests
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class OAuth2ControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private UserPrincipal githubOnlyPrincipal;
    private UserPrincipal googleOnlyPrincipal;
    private UserPrincipal bothProvidersPrincipal;
    private UserPrincipal noProvidersPrincipal;

    @BeforeEach
    public void setUp() {
        setupGithubOnlyUser();
        setupGoogleOnlyUser();
        setupBothProvidersUser();
        setupNoProvidersUser();
    }

    private void setupGithubOnlyUser() {
        User githubUser = new User();
        githubUser.setGithubId("github123");
        githubUser = userRepository.save(githubUser);

        Profile profile = new Profile();
        profile.setUser(githubUser);
        profile.setUsername("githubuser");
        profile.setPrimaryEmail("github@test.com");
        profile.setGithubEmail("github@test.com");
        profile.setAvatarUrl("https://github.com/githubuser.png");
        profileRepository.save(profile);

        var githubDelegate = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of(
                    "id", "github123",
                    "login", "githubuser",
                    "email", "github@test.com",
                    "avatar_url", "https://github.com/githubuser.png"
                ),
                "id"
        );

        this.githubOnlyPrincipal = new UserPrincipal(githubDelegate, githubUser);
    }

    private void setupGoogleOnlyUser() {
        User googleUser = new User();
        googleUser.setGoogleId("google123");
        googleUser = userRepository.save(googleUser);

        Profile profile = new Profile();
        profile.setUser(googleUser);
        profile.setUsername("googleuser");
        profile.setPrimaryEmail("google@test.com");
        profile.setGoogleEmail("google@test.com");
        profile.setAvatarUrl("https://google.com/googleuser.png");
        profileRepository.save(profile);

        var googleDelegate = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of(
                    "sub", "google123",
                    "name", "googleuser",
                    "email", "google@test.com",
                    "picture", "https://google.com/googleuser.png"
                ),
                "sub"
        );

        this.googleOnlyPrincipal = new UserPrincipal(googleDelegate, googleUser);
    }

    private void setupBothProvidersUser() {
        User bothUser = new User();
        bothUser.setGithubId("github456");
        bothUser.setGoogleId("google456");
        bothUser = userRepository.save(bothUser);

        Profile profile = new Profile();
        profile.setUser(bothUser);
        profile.setUsername("bothuser");
        profile.setPrimaryEmail("both@test.com");
        profile.setGithubEmail("github@both.com");
        profile.setGoogleEmail("google@both.com");
        profile.setAvatarUrl("https://github.com/bothuser.png");
        profileRepository.save(profile);

        var bothDelegate = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of(
                    "id", "github456",
                    "login", "bothuser",
                    "email", "both@test.com",
                    "avatar_url", "https://github.com/bothuser.png"
                ),
                "id"
        );

        this.bothProvidersPrincipal = new UserPrincipal(bothDelegate, bothUser);
    }

    private void setupNoProvidersUser() {
        User noProvidersUser = new User();
        noProvidersUser = userRepository.save(noProvidersUser);

        Profile profile = new Profile();
        profile.setUser(noProvidersUser);
        profile.setUsername("nouser");
        profile.setPrimaryEmail("no@test.com");
        profileRepository.save(profile);

        var noDelegate = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of(
                    "id", "temp123",
                    "login", "nouser",
                    "email", "no@test.com"
                ),
                "id"
        );

        this.noProvidersPrincipal = new UserPrincipal(noDelegate, noProvidersUser);
    }

    // === CURRENT USER TESTS ===

    @Test
    public void currentUser_returnsGithubOnlyUser_whenAuthenticatedWithGithub() throws Exception {
        mockMvc
                .perform(get("/api/auth/user")
                        .with(oauth2Login().oauth2User(githubOnlyPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.id").value(githubOnlyPrincipal.getDomainUser().getId()))
                .andExpect(jsonPath("$.githubLinked").value(true))
                .andExpect(jsonPath("$.googleLinked").value(false));
    }

    @Test
    public void currentUser_returnsGoogleOnlyUser_whenAuthenticatedWithGoogle() throws Exception {
        mockMvc
                .perform(get("/api/auth/user")
                        .with(oauth2Login().oauth2User(googleOnlyPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.id").value(googleOnlyPrincipal.getDomainUser().getId()))
                .andExpect(jsonPath("$.githubLinked").value(false))
                .andExpect(jsonPath("$.googleLinked").value(true));
    }

    @Test
    public void currentUser_returnsBothProvidersUser_whenUserHasBothLinked() throws Exception {
        mockMvc
                .perform(get("/api/auth/user")
                        .with(oauth2Login().oauth2User(bothProvidersPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.id").value(bothProvidersPrincipal.getDomainUser().getId()))
                .andExpect(jsonPath("$.githubLinked").value(true))
                .andExpect(jsonPath("$.googleLinked").value(true));
    }

    @Test
    public void currentUser_returnsNoProvidersUser_whenUserHasNoProvidersLinked() throws Exception {
        mockMvc
                .perform(get("/api/auth/user")
                        .with(oauth2Login().oauth2User(noProvidersPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.id").value(noProvidersPrincipal.getDomainUser().getId()))
                .andExpect(jsonPath("$.githubLinked").value(false))
                .andExpect(jsonPath("$.googleLinked").value(false));
    }

    @Test
    public void currentUser_returnsUnauthenticated_whenNotLoggedIn() throws Exception {
        mockMvc
                .perform(get("/api/auth/user"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.githubLinked").value(false))
                .andExpect(jsonPath("$.googleLinked").value(false));
    }

    // === LINK PROVIDER TESTS ===

    @Test
    public void initiateProviderLinking_redirectsToGithubOAuth_whenValidProvider() throws Exception {
        mockMvc
                .perform(get("/api/auth/link/github")
                        .with(oauth2Login().oauth2User(googleOnlyPrincipal)))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.header().string("Location", "/oauth2/authorization/github?state=link"));
    }

    @Test
    public void initiateProviderLinking_redirectsToGoogleOAuth_whenValidProvider() throws Exception {
        mockMvc
                .perform(get("/api/auth/link/google")
                        .with(oauth2Login().oauth2User(githubOnlyPrincipal)))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.header().string("Location", "/oauth2/authorization/google?state=link"));
    }

    @Test
    public void initiateProviderLinking_returnsBadRequest_whenUnsupportedProvider() throws Exception {
        mockMvc
                .perform(get("/api/auth/link/facebook")
                        .with(oauth2Login().oauth2User(githubOnlyPrincipal)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Unsupported provider: facebook"));
    }

    @Test
    public void initiateProviderLinking_returnsRedirect_whenNotLoggedIn() throws Exception {
        mockMvc
                .perform(get("/api/auth/link/github"))
                .andDo(print())
                .andExpect(status().isFound());
    }
}
