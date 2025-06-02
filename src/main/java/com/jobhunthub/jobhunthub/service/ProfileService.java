package com.jobhunthub.jobhunthub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobhunthub.jobhunthub.dto.OAuth2UserAttributes;
import com.jobhunthub.jobhunthub.dto.ProfileDTO;
import com.jobhunthub.jobhunthub.dto.UpdateProfileRequestDTO;
import com.jobhunthub.jobhunthub.exception.GlobalExceptionHandler.InvalidRequestException;
import com.jobhunthub.jobhunthub.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.jobhunthub.jobhunthub.model.Profile;
import com.jobhunthub.jobhunthub.model.User;
import com.jobhunthub.jobhunthub.repository.ProfileRepository;

@Service
public class ProfileService {
    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    // Provision a profile for a user when they first sign up
    public void provisionProfile(User user, OAuth2UserAttributes attrs) {
        Profile p = new Profile();
        p.setUser(user);
        p.setPrimaryEmail(attrs.email());
        p.setAvatarUrl(attrs.avatarUrl());
        profileRepository.save(p);
    }

    // Link a provider email to a user's profile when they link their account to a provider
    public void linkProviderEmail(User user, OAuth2UserAttributes attrs, String provider) {
        Profile profile = profileRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new IllegalStateException("Profile must already exist before linking a new provider")
                );

        String email = attrs.email();
        switch (provider.toLowerCase()) {
            case "github" -> profile.setGithubEmail(email);
            case "google" -> profile.setGoogleEmail(email);
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        profileRepository.save(profile);
    }


    // Get profile information for a user 
    public ProfileDTO getProfileByUser(User user) {
        return profileRepository.findByUser(user)
                .map(ProfileDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "user", user.getId()));
    }

    // Add validation method like other services
    private void validateProfile(Profile profile) {
        if (profile.getUsername() != null && profile.getUsername().trim().isEmpty()) {
            throw new InvalidRequestException("Username cannot be empty");
        }
        if (profile.getPrimaryEmail() != null && !profile.getPrimaryEmail().contains("@")) {
            throw new InvalidRequestException("Invalid email format");
        }
    }

    // Update profile information for a user
    @Transactional
    public ProfileDTO updateProfile(User user, UpdateProfileRequestDTO dto) {
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "user", user.getId()));
        
        // Check uniqueness before updating
        if (dto.getUsername() != null && !dto.getUsername().equals(profile.getUsername())) {
            if (profileRepository.existsByUsername(dto.getUsername())) {
                throw new InvalidRequestException("Username already exists");
            }
            profile.setUsername(dto.getUsername());
        }
        
        if (dto.getPrimaryEmail() != null && !dto.getPrimaryEmail().equals(profile.getPrimaryEmail())) {
            if (profileRepository.existsByPrimaryEmail(dto.getPrimaryEmail())) {
                throw new InvalidRequestException("Email already exists");
            }
            profile.setPrimaryEmail(dto.getPrimaryEmail());
        }
        
        if (dto.getAvatarUrl() != null) {
            profile.setAvatarUrl(dto.getAvatarUrl());
        }
        
        validateProfile(profile);
        
        Profile savedProfile = profileRepository.save(profile);
        return ProfileDTO.fromEntity(savedProfile);
    }

    public boolean profileExists(User user) {
        return profileRepository.findByUser(user).isPresent();
    }
}
