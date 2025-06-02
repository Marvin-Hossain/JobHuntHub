package com.jobhunthub.jobhunthub.service;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.jobhunthub.jobhunthub.dto.AuthenticatedUserDTO;
import com.jobhunthub.jobhunthub.dto.OAuth2UserAttributes;
import com.jobhunthub.jobhunthub.exception.GlobalExceptionHandler.InvalidRequestException;
import com.jobhunthub.jobhunthub.model.User;
import com.jobhunthub.jobhunthub.repository.UserRepository;

public class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private UserService userService;

    private OAuth2User mockGithubOAuth2User;
    private OAuth2User mockGoogleOAuth2User;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup GitHub OAuth2User mock
        mockGithubOAuth2User = mock(OAuth2User.class);
        when(mockGithubOAuth2User.getAttribute("id")).thenReturn("github123");
        when(mockGithubOAuth2User.getAttribute("email")).thenReturn("github@test.com");
        when(mockGithubOAuth2User.getAttribute("avatar_url")).thenReturn("https://github.com/user.png");
        
        // Setup Google OAuth2User mock
        mockGoogleOAuth2User = mock(OAuth2User.class);
        when(mockGoogleOAuth2User.getAttribute("sub")).thenReturn("google123");
        when(mockGoogleOAuth2User.getAttribute("email")).thenReturn("google@test.com");
        when(mockGoogleOAuth2User.getAttribute("picture")).thenReturn("https://google.com/user.png");
    }

    // =====================
    // authenticateUser Tests
    // =====================
    
    @Test
    public void authenticateUser_returnsExistingUser_whenGithubUserExists() {
        // Arrange
        User existingUser = User.builder().id(1L).githubId("github123").build();
        when(userRepository.findByGithubId("github123")).thenReturn(Optional.of(existingUser));

        // Act
        User result = userService.authenticateUser(mockGithubOAuth2User, "github");

        // Assert
        Assertions.assertThat(result).isEqualTo(existingUser);
        verify(userRepository).findByGithubId("github123");
        verify(userRepository, never()).save(any());
        verify(profileService, never()).provisionProfile(any(), any());
    }

    @Test
    public void authenticateUser_returnsExistingUser_whenGoogleUserExists() {
        // Arrange
        User existingUser = User.builder().id(1L).googleId("google123").build();
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(existingUser));

        // Act
        User result = userService.authenticateUser(mockGoogleOAuth2User, "google");

        // Assert
        Assertions.assertThat(result).isEqualTo(existingUser);
        verify(userRepository).findByGoogleId("google123");
        verify(userRepository, never()).save(any());
        verify(profileService, never()).provisionProfile(any(), any());
    }

    @Test
    public void authenticateUser_createsNewUser_whenGithubUserDoesNotExist() {
        // Arrange
        when(userRepository.findByGithubId("github123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        // Act
        User result = userService.authenticateUser(mockGithubOAuth2User, "github");

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getGithubId()).isEqualTo("github123");
        verify(userRepository).save(any(User.class));
        verify(profileService).provisionProfile(eq(result), any(OAuth2UserAttributes.class));
        verify(profileService).linkProviderEmail(eq(result), any(OAuth2UserAttributes.class), eq("github"));
    }

    @Test
    public void authenticateUser_createsNewUser_whenGoogleUserDoesNotExist() {
        // Arrange
        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        // Act
        User result = userService.authenticateUser(mockGoogleOAuth2User, "google");

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getGoogleId()).isEqualTo("google123");
        verify(userRepository).save(any(User.class));
        verify(profileService).provisionProfile(eq(result), any(OAuth2UserAttributes.class));
        verify(profileService).linkProviderEmail(eq(result), any(OAuth2UserAttributes.class), eq("google"));
    }

    @Test
    public void authenticateUser_throwsException_whenUnsupportedProvider() {
        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            userService.authenticateUser(mockGithubOAuth2User, "facebook")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Unsupported provider: facebook");
    }

    // ========================
    // linkProviderToUser Tests
    // ========================

    @Test
    public void linkProviderToUser_linksProvider_whenProfileExists() {
        // Arrange
        User user = User.builder().id(1L).googleId("google123").build();
        when(profileService.profileExists(user)).thenReturn(true);
        when(userRepository.findByGithubId("github123")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        // Act
        User result = userService.linkProviderToUser(user, mockGithubOAuth2User, "github");

        // Assert
        Assertions.assertThat(result).isEqualTo(user);
        Assertions.assertThat(user.getGithubId()).isEqualTo("github123");
        verify(userRepository).save(user);
        verify(profileService).profileExists(user);
        verify(profileService, never()).provisionProfile(any(), any());
        verify(profileService).linkProviderEmail(eq(user), any(OAuth2UserAttributes.class), eq("github"));
    }

    @Test
    public void linkProviderToUser_provisionsProfile_whenProfileDoesNotExist() {
        // Arrange
        User user = User.builder().id(1L).googleId("google123").build();
        when(profileService.profileExists(user)).thenReturn(false);
        when(userRepository.findByGithubId("github123")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        // Act
        User result = userService.linkProviderToUser(user, mockGithubOAuth2User, "github");

        // Assert
        Assertions.assertThat(result).isEqualTo(user);
        verify(profileService).profileExists(user);
        verify(profileService).provisionProfile(eq(user), any(OAuth2UserAttributes.class));
        verify(profileService).linkProviderEmail(eq(user), any(OAuth2UserAttributes.class), eq("github"));
    }

    @Test
    public void linkProviderToUser_throwsException_whenProviderAlreadyLinkedToAnotherUser() {
        // Arrange
        User user = User.builder().id(1L).googleId("google123").build();
        User existingGithubUser = User.builder().id(2L).githubId("github123").build();
        when(userRepository.findByGithubId("github123")).thenReturn(Optional.of(existingGithubUser));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            userService.linkProviderToUser(user, mockGithubOAuth2User, "github")
        ).isInstanceOf(InvalidRequestException.class)
         .hasMessageContaining("This github account is already linked to another user");
    }

    @Test
    public void linkProviderToUser_throwsException_whenUnsupportedProvider() {
        // Arrange
        User user = User.builder().id(1L).googleId("google123").build();

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            userService.linkProviderToUser(user, mockGithubOAuth2User, "facebook")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Unsupported provider: facebook");
    }

    // ==============================
    // getAuthenticatedUserDTO Tests
    // ==============================

    @Test
    public void getAuthenticatedUserDTO_returnsCorrectStatus_whenBothProvidersLinked() {
        // Arrange
        User user = User.builder().id(1L).githubId("github123").googleId("google123").build();

        // Act
        AuthenticatedUserDTO result = userService.getAuthenticatedUserDTO(user);

        // Assert
        Assertions.assertThat(result.authenticated()).isTrue();
        Assertions.assertThat(result.id()).isEqualTo(1L);
        Assertions.assertThat(result.githubLinked()).isTrue();
        Assertions.assertThat(result.googleLinked()).isTrue();
    }

    @Test
    public void getAuthenticatedUserDTO_returnsCorrectStatus_whenOnlyGithubLinked() {
        // Arrange
        User user = User.builder().id(1L).githubId("github123").build();

        // Act
        AuthenticatedUserDTO result = userService.getAuthenticatedUserDTO(user);

        // Assert
        Assertions.assertThat(result.authenticated()).isTrue();
        Assertions.assertThat(result.id()).isEqualTo(1L);
        Assertions.assertThat(result.githubLinked()).isTrue();
        Assertions.assertThat(result.googleLinked()).isFalse();
    }

    @Test
    public void getAuthenticatedUserDTO_returnsCorrectStatus_whenOnlyGoogleLinked() {
        // Arrange
        User user = User.builder().id(1L).googleId("google123").build();

        // Act
        AuthenticatedUserDTO result = userService.getAuthenticatedUserDTO(user);

        // Assert
        Assertions.assertThat(result.authenticated()).isTrue();
        Assertions.assertThat(result.id()).isEqualTo(1L);
        Assertions.assertThat(result.githubLinked()).isFalse();
        Assertions.assertThat(result.googleLinked()).isTrue();
    }

    @Test
    public void getAuthenticatedUserDTO_returnsCorrectStatus_whenNoProvidersLinked() {
        // Arrange
        User user = User.builder().id(1L).build();

        // Act
        AuthenticatedUserDTO result = userService.getAuthenticatedUserDTO(user);

        // Assert
        Assertions.assertThat(result.authenticated()).isTrue();
        Assertions.assertThat(result.id()).isEqualTo(1L);
        Assertions.assertThat(result.githubLinked()).isFalse();
        Assertions.assertThat(result.googleLinked()).isFalse();
    }
}
