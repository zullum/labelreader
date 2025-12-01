package com.labelreader.service;

import com.labelreader.dto.ArtistProfileDto;
import com.labelreader.dto.UpdateArtistProfileRequest;
import com.labelreader.entity.ArtistProfile;
import com.labelreader.entity.User;
import com.labelreader.repository.ArtistProfileRepository;
import com.labelreader.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtistProfileService {

    private final ArtistProfileRepository artistProfileRepository;
    private final UserRepository userRepository;

    public ArtistProfileDto getProfile(Long userId) {
        ArtistProfile profile = artistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        return mapToDto(profile);
    }

    @Transactional
    public ArtistProfileDto updateProfile(Long userId, UpdateArtistProfileRequest request) {
        ArtistProfile profile = artistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Artist profile not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update profile
        profile.setArtistName(request.getArtistName());
        profile.setGenre(request.getGenre());
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setSpotifyUrl(request.getSpotifyUrl());
        profile.setInstagramHandle(request.getInstagramHandle());
        profile.setTwitterHandle(request.getTwitterHandle());
        profile.setSoundcloudUrl(request.getSoundcloudUrl());

        // Update user info
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }

        artistProfileRepository.save(profile);
        userRepository.save(user);

        return mapToDto(profile);
    }

    private ArtistProfileDto mapToDto(ArtistProfile profile) {
        return ArtistProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .artistName(profile.getArtistName())
                .genre(profile.getGenre())
                .websiteUrl(profile.getWebsiteUrl())
                .spotifyUrl(profile.getSpotifyUrl())
                .instagramHandle(profile.getInstagramHandle())
                .twitterHandle(profile.getTwitterHandle())
                .soundcloudUrl(profile.getSoundcloudUrl())
                .totalSubmissions(profile.getTotalSubmissions())
                .totalPlays(profile.getTotalPlays())
                .build();
    }
}
