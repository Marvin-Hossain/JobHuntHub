package com.jobhunthub.jobhunthub.service;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.jobhunthub.jobhunthub.dto.OAuth2UserAttributes;
import com.jobhunthub.jobhunthub.dto.ProfileDTO;
import com.jobhunthub.jobhunthub.dto.UpdateProfileRequestDTO;
import com.jobhunthub.jobhunthub.exception.GlobalExceptionHandler.InvalidRequestException;
import com.jobhunthub.jobhunthub.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.jobhunthub.jobhunthub.model.Profile;
import com.jobhunthub.jobhunthub.model.User;
import com.jobhunthub.jobhunthub.repository.ProfileRepository;

public class ProfileServiceTests {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    private User user;
    private Profile profile;
    private OAuth2UserAttributes githubAttrs;
    private OAuth2UserAttributes googleAttrs;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        user = User.builder()
                .id(1L)
                .githubId("github123")
                .build();

        profile = new Profile();
        profile.setId(1L);
        profile.setUser(user);
        profile.setUsername("testuser");
        profile.setPrimaryEmail("test@example.com");
        profile.setAvatarUrl("https://example.com/avatar.png");
        profile.setGithubEmail("github@example.com");
        profile.setGoogleEmail("google@example.com");

        githubAttrs = new OAuth2UserAttributes("github123", "github@example.com", "https://github.com/avatar.png");
        googleAttrs = new OAuth2UserAttributes("google123", "google@example.com", "https://google.com/avatar.png");
    }

    // === PROVISION PROFILE TESTS ===

    @Test
    public void provisionProfile_createsNewProfile_withGithubAttributes() {
        // Arrange
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile savedProfile = invocation.getArgument(0);
            savedProfile.setId(1L);
            return savedProfile;
        });

        // Act
        profileService.provisionProfile(user, githubAttrs);

        // Assert
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    public void provisionProfile_createsNewProfile_withGoogleAttributes() {
        // Arrange
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile savedProfile = invocation.getArgument(0);
            savedProfile.setId(1L);
            return savedProfile;
        });

        // Act
        profileService.provisionProfile(user, googleAttrs);

        // Assert
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    public void provisionProfile_setsCorrectAttributes_fromOAuth2UserAttributes() {
        // Arrange
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> {
            Profile savedProfile = invocation.getArgument(0);
            
            // Verify the profile has correct attributes
            Assertions.assertThat(savedProfile.getUser()).isEqualTo(user);
            Assertions.assertThat(savedProfile.getPrimaryEmail()).isEqualTo(githubAttrs.email());
            Assertions.assertThat(savedProfile.getAvatarUrl()).isEqualTo(githubAttrs.avatarUrl());
            
            return savedProfile;
        });

        // Act
        profileService.provisionProfile(user, githubAttrs);

        // Assert
        verify(profileRepository).save(any(Profile.class));
    }

    // === LINK PROVIDER EMAIL TESTS ===

    @Test
    public void linkProviderEmail_updatesGithubEmail_whenGithubProvider() {
        // Arrange
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        // Act
        profileService.linkProviderEmail(user, githubAttrs, "github");

        // Assert
        Assertions.assertThat(profile.getGithubEmail()).isEqualTo(githubAttrs.email());
        verify(profileRepository).save(profile);
    }

    @Test
    public void linkProviderEmail_updatesGoogleEmail_whenGoogleProvider() {
        // Arrange
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        // Act
        profileService.linkProviderEmail(user, googleAttrs, "google");

        // Assert
        Assertions.assertThat(profile.getGoogleEmail()).isEqualTo(googleAttrs.email());
        verify(profileRepository).save(profile);
    }

    @Test
    public void linkProviderEmail_throwsException_whenProfileNotFound() {
        // Arrange
        when(profileRepository.findByUser(user)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            profileService.linkProviderEmail(user, githubAttrs, "github")
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("Profile must already exist before linking a new provider");
    }

    @Test
    public void linkProviderEmail_throwsException_whenUnsupportedProvider() {
        // Arrange
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            profileService.linkProviderEmail(user, githubAttrs, "facebook")
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Unsupported provider: facebook");
    }

    // === GET PROFILE TESTS ===

    @Test
    public void getProfileByUser_returnsProfileDTO_whenProfileExists() {
        // Arrange
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));

        // Act
        ProfileDTO result = profileService.getProfileByUser(user);

        // Assert
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.username()).isEqualTo(profile.getUsername());
        Assertions.assertThat(result.primaryEmail()).isEqualTo(profile.getPrimaryEmail());
        Assertions.assertThat(result.avatarUrl()).isEqualTo(profile.getAvatarUrl());
        Assertions.assertThat(result.githubEmail()).isEqualTo(profile.getGithubEmail());
        Assertions.assertThat(result.googleEmail()).isEqualTo(profile.getGoogleEmail());
    }

    @Test
    public void getProfileByUser_throwsException_whenProfileNotFound() {
        // Arrange
        when(profileRepository.findByUser(user)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            profileService.getProfileByUser(user)
        ).isInstanceOf(ResourceNotFoundException.class)
         .hasMessageContaining("Profile not found with user: '1'");
    }

    // === PROFILE EXISTS TESTS ===

    @Test
    public void profileExists_returnsTrue_whenProfileExists() {
        // Arrange
        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));

        // Act
        boolean result = profileService.profileExists(user);

        // Assert
        Assertions.assertThat(result).isTrue();
        verify(profileRepository).findByUser(user);
    }

    @Test
    public void profileExists_returnsFalse_whenProfileDoesNotExist() {
        // Arrange
        when(profileRepository.findByUser(user)).thenReturn(Optional.empty());

        // Act
        boolean result = profileService.profileExists(user);

        // Assert
        Assertions.assertThat(result).isFalse();
        verify(profileRepository).findByUser(user);
    }

    // === UPDATE PROFILE TESTS ===

    @Test
    public void updateProfile_updatesUsername_whenUsernameIsUnique() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setUsername("newusername");

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.existsByUsername("newusername")).thenReturn(false);
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        // Act
        ProfileDTO result = profileService.updateProfile(user, updateRequest);

        // Assert
        Assertions.assertThat(profile.getUsername()).isEqualTo("newusername");
        Assertions.assertThat(result.username()).isEqualTo("newusername");
        verify(profileRepository).save(profile);
    }

    @Test
    public void updateProfile_updatesPrimaryEmail_whenEmailIsUnique() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setPrimaryEmail("newemail@example.com");

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.existsByPrimaryEmail("newemail@example.com")).thenReturn(false);
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        // Act
        ProfileDTO result = profileService.updateProfile(user, updateRequest);

        // Assert
        Assertions.assertThat(profile.getPrimaryEmail()).isEqualTo("newemail@example.com");
        Assertions.assertThat(result.primaryEmail()).isEqualTo("newemail@example.com");
        verify(profileRepository).save(profile);
    }

    @Test
    public void updateProfile_updatesAvatarUrl_always() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setAvatarUrl("https://newavatar.com/image.png");

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        // Act
        ProfileDTO result = profileService.updateProfile(user, updateRequest);

        // Assert
        Assertions.assertThat(profile.getAvatarUrl()).isEqualTo("https://newavatar.com/image.png");
        Assertions.assertThat(result.avatarUrl()).isEqualTo("https://newavatar.com/image.png");
        verify(profileRepository).save(profile);
    }

    @Test
    public void updateProfile_updatesAllFields_whenAllAreValid() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setUsername("newusername");
        updateRequest.setPrimaryEmail("newemail@example.com");
        updateRequest.setAvatarUrl("https://newavatar.com/image.png");

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.existsByUsername("newusername")).thenReturn(false);
        when(profileRepository.existsByPrimaryEmail("newemail@example.com")).thenReturn(false);
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        // Act
        ProfileDTO result = profileService.updateProfile(user, updateRequest);

        // Assert
        Assertions.assertThat(result.username()).isEqualTo("newusername");
        Assertions.assertThat(result.primaryEmail()).isEqualTo("newemail@example.com");
        Assertions.assertThat(result.avatarUrl()).isEqualTo("https://newavatar.com/image.png");
        verify(profileRepository).save(profile);
    }

    @Test
    public void updateProfile_skipsUpdate_whenUsernameIsSame() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setUsername("testuser"); // Same as current username

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        // Act
        ProfileDTO result = profileService.updateProfile(user, updateRequest);

        // Assert
        verify(profileRepository, never()).existsByUsername(any());
        Assertions.assertThat(result.username()).isEqualTo("testuser");
    }

    @Test
    public void updateProfile_skipsUpdate_whenEmailIsSame() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setPrimaryEmail("test@example.com"); // Same as current email

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        // Act
        ProfileDTO result = profileService.updateProfile(user, updateRequest);

        // Assert
        verify(profileRepository, never()).existsByPrimaryEmail(any());
        Assertions.assertThat(result.primaryEmail()).isEqualTo("test@example.com");
    }

    @Test
    public void updateProfile_throwsException_whenUsernameAlreadyExists() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setUsername("existinguser");

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            profileService.updateProfile(user, updateRequest)
        ).isInstanceOf(InvalidRequestException.class)
         .hasMessageContaining("Username already exists");
    }

    @Test
    public void updateProfile_throwsException_whenEmailAlreadyExists() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setPrimaryEmail("existing@example.com");

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.existsByPrimaryEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            profileService.updateProfile(user, updateRequest)
        ).isInstanceOf(InvalidRequestException.class)
         .hasMessageContaining("Email already exists");
    }

    @Test
    public void updateProfile_throwsException_whenProfileNotFound() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setUsername("newusername");

        when(profileRepository.findByUser(user)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            profileService.updateProfile(user, updateRequest)
        ).isInstanceOf(ResourceNotFoundException.class)
         .hasMessageContaining("Profile not found with user: '1'");
    }

    // === VALIDATION TESTS ===

    @Test
    public void updateProfile_throwsException_whenUsernameIsEmpty() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setUsername("   "); // Empty/whitespace username

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.existsByUsername("   ")).thenReturn(false);

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            profileService.updateProfile(user, updateRequest)
        ).isInstanceOf(InvalidRequestException.class)
         .hasMessageContaining("Username cannot be empty");
    }

    @Test
    public void updateProfile_throwsException_whenEmailIsInvalid() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        updateRequest.setPrimaryEmail("invalidemail"); // No @ symbol

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.existsByPrimaryEmail("invalidemail")).thenReturn(false);

        // Act & Assert
        Assertions.assertThatThrownBy(() -> 
            profileService.updateProfile(user, updateRequest)
        ).isInstanceOf(InvalidRequestException.class)
         .hasMessageContaining("Invalid email format");
    }

    @Test
    public void updateProfile_allowsNullValues_withoutUpdating() {
        // Arrange
        UpdateProfileRequestDTO updateRequest = new UpdateProfileRequestDTO();
        // All fields are null

        when(profileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        String originalUsername = profile.getUsername();
        String originalEmail = profile.getPrimaryEmail();
        String originalAvatar = profile.getAvatarUrl();

        // Act
        ProfileDTO result = profileService.updateProfile(user, updateRequest);

        // Assert
        Assertions.assertThat(result.username()).isEqualTo(originalUsername);
        Assertions.assertThat(result.primaryEmail()).isEqualTo(originalEmail);
        Assertions.assertThat(result.avatarUrl()).isEqualTo(originalAvatar);
        verify(profileRepository).save(profile);
    }
} 